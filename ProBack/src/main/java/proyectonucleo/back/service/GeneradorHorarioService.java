package proyectonucleo.back.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import proyectonucleo.back.dto.AplicarPropuestaDTO;
import proyectonucleo.back.dto.BloquePropuestoDTO;
import proyectonucleo.back.dto.HorarioDTO;
import proyectonucleo.back.dto.ProgramacionDTO;
import proyectonucleo.back.dto.PropuestaDTO;
import proyectonucleo.back.dto.PropuestaDTO.AsignaturaPendienteDTO;
import proyectonucleo.back.dto.SolicitudPropuestaDTO;
import proyectonucleo.back.entity.Asignatura;
import proyectonucleo.back.entity.DiaSemana;
import proyectonucleo.back.entity.Horario;
import proyectonucleo.back.entity.Programacion;
import proyectonucleo.back.exception.ResourceNotFoundException;
import proyectonucleo.back.repository.AsignaturaRepository;
import proyectonucleo.back.repository.HorarioRepository;

/**
 * Generación automática de horarios: propone dónde ubicar las sesiones semanales de las
 * asignaturas elegidas y, cuando el Director aprueba la propuesta, la guarda.
 * <p>
 * Las reglas duras (franjas, almuerzo, duración y cruces dentro del horario) nunca se
 * incumplen. Entre las ubicaciones válidas se prefiere la de menor costo según estas
 * preferencias: sesiones de una asignatura en días distintos y no consecutivos, a la misma
 * hora; carga repartida entre los días; y el sábado y la franja de 19:00 a 21:00 solo cuando
 * no hay otro espacio. Se prueban varios órdenes de asignación y se conserva el que ubica más
 * sesiones con menor costo; la semilla hace reproducible cada variante.
 */
@Service
public class GeneradorHorarioService {

	private static final int INTENTOS = 300;
	private static final double RUIDO = 1.5;
	private static final Set<Integer> HORAS_ALINEADAS = Set.of(7, 9, 13, 15, 17, 19);

	// Sábado y noche cuestan más que repetir día: solo se usan si no queda otro espacio.
	private static final double COSTO_MISMO_DIA = 40;
	private static final double COSTO_DIA_CONSECUTIVO = 6;
	private static final double COSTO_HORA_DISTINTA = 3;
	private static final double COSTO_SABADO = 70;
	private static final double COSTO_NOCHE = 60;
	private static final double COSTO_POR_BLOQUE_DEL_DIA = 2.5;
	private static final double COSTO_HORA_DESALINEADA = 2;

	private final ProgramacionService programacionService;
	private final HorarioService horarioService;
	private final AsignaturaRepository asignaturaRepository;
	private final HorarioRepository horarioRepository;
	private final List<Franja> franjas;

	public GeneradorHorarioService(ProgramacionService programacionService, HorarioService horarioService,
			AsignaturaRepository asignaturaRepository, HorarioRepository horarioRepository,
			ReglasHorarioValidator reglas) {
		this.programacionService = programacionService;
		this.horarioService = horarioService;
		this.asignaturaRepository = asignaturaRepository;
		this.horarioRepository = horarioRepository;
		List<Franja> validas = new ArrayList<>();
		for (DiaSemana dia : DiaSemana.values()) {
			for (int hora = ReglasHorarioValidator.INICIO_JORNADA.getHour(); hora < 24; hora++) {
				LocalTime inicio = LocalTime.of(hora, 0);
				LocalTime fin = inicio.plus(ReglasHorarioValidator.DURACION_SESION);
				if (fin.isAfter(inicio) && reglas.esValido(dia, inicio, fin)) {
					validas.add(new Franja(dia, inicio, fin));
				}
			}
		}
		this.franjas = List.copyOf(validas);
	}

	/**
	 * Calcula una propuesta sin guardar nada.
	 *
	 * @param programacionId horario que se está editando
	 * @param solicitud      asignaturas elegidas, modo y semilla opcional
	 * @return bloques sugeridos y asignaturas que no cupieron
	 */
	@Transactional(readOnly = true)
	public PropuestaDTO proponer(Long programacionId, SolicitudPropuestaDTO solicitud) {
		Programacion programacion = programacionService.buscar(programacionId);
		List<Asignatura> asignaturas = buscarAsignaturas(solicitud.asignaturaIds());
		List<Horario> conservados = solicitud.reorganizar() ? List.of()
				: horarioRepository.findByProgramacionId(programacion.getId());
		long semilla = solicitud.semilla() != null ? solicitud.semilla()
				: ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);

		Map<Long, Integer> faltantes = new HashMap<>();
		for (Asignatura asignatura : asignaturas) {
			long programadas = conservados.stream()
					.filter(bloque -> bloque.getAsignatura().getId().equals(asignatura.getId())).count();
			int faltan = asignatura.getSesionesSemana() - (int) programadas;
			if (faltan > 0) {
				faltantes.put(asignatura.getId(), faltan);
			}
		}
		List<Asignatura> pendientes = asignaturas.stream().filter(a -> faltantes.containsKey(a.getId())).toList();

		Random azar = new Random(semilla);
		Solucion mejor = null;
		for (int intento = 0; intento < INTENTOS && !pendientes.isEmpty(); intento++) {
			Solucion candidata = asignar(ordenar(pendientes, faltantes, azar), faltantes, conservados, azar);
			if (mejor == null || candidata.esMejorQue(mejor)) {
				mejor = candidata;
			}
		}

		List<BloquePropuestoDTO> bloques = mejor == null ? List.of()
				: mejor.ubicados.stream().sorted(Comparator.comparing((Ubicado u) -> u.franja.dia)
						.thenComparing(u -> u.franja.inicio))
						.map(u -> new BloquePropuestoDTO(u.asignatura.getId(), u.asignatura.getCodigo(),
								u.asignatura.getNombre(), u.franja.dia, u.franja.inicio, u.franja.fin))
						.toList();
		List<AsignaturaPendienteDTO> sinUbicar = new ArrayList<>();
		for (Asignatura asignatura : pendientes) {
			long ubicadas = bloques.stream().filter(b -> b.asignaturaId().equals(asignatura.getId())).count();
			int restantes = faltantes.get(asignatura.getId()) - (int) ubicadas;
			if (restantes > 0) {
				sinUbicar.add(new AsignaturaPendienteDTO(asignatura.getId(), asignatura.getCodigo(),
						asignatura.getNombre(), restantes));
			}
		}
		return new PropuestaDTO(semilla, solicitud.reorganizar(), bloques, sinUbicar);
	}

	/**
	 * Guarda la propuesta aprobada en una sola transacción. Cada bloque pasa por las mismas
	 * validaciones que un bloque creado a mano; si alguno falla no se guarda ninguno.
	 *
	 * @param programacionId horario que se está editando
	 * @param aprobada       bloques aprobados y si reemplazan a los actuales
	 * @return el horario con sus bloques definitivos
	 */
	@Transactional
	public ProgramacionDTO aplicar(Long programacionId, AplicarPropuestaDTO aprobada) {
		programacionService.buscar(programacionId);
		if (aprobada.reorganizar()) {
			horarioRepository.eliminarPorProgramacion(programacionId);
		}
		for (BloquePropuestoDTO bloque : aprobada.bloques()) {
			horarioService.create(new HorarioDTO(programacionId, bloque.asignaturaId(), bloque.diaSemana(),
					bloque.horaInicio(), bloque.horaFin()));
		}
		return programacionService.getById(programacionId);
	}

	private List<Asignatura> buscarAsignaturas(List<Long> ids) {
		Set<Long> buscadas = new LinkedHashSet<>(ids);
		Map<Long, Asignatura> encontradas = asignaturaRepository.findAllById(buscadas).stream()
				.collect(Collectors.toMap(Asignatura::getId, asignatura -> asignatura));
		for (Long id : buscadas) {
			if (!encontradas.containsKey(id)) {
				throw new ResourceNotFoundException("La asignatura con id " + id + " no existe.");
			}
		}
		return encontradas.values().stream().sorted(Comparator.comparing(Asignatura::getCodigo)).toList();
	}

	/** Primero las asignaturas con más sesiones por ubicar; los empates se barajan. */
	private static List<Asignatura> ordenar(List<Asignatura> asignaturas, Map<Long, Integer> faltantes,
			Random azar) {
		List<Asignatura> orden = new ArrayList<>(asignaturas);
		Collections.shuffle(orden, azar);
		orden.sort(Comparator.comparing((Asignatura a) -> faltantes.get(a.getId())).reversed());
		return orden;
	}

	private Solucion asignar(List<Asignatura> orden, Map<Long, Integer> faltantes, List<Horario> conservados,
			Random azar) {
		Map<DiaSemana, List<Franja>> ocupadas = new EnumMap<>(DiaSemana.class);
		Map<Long, List<Franja>> porAsignatura = new HashMap<>();
		for (Horario bloque : conservados) {
			Franja franja = new Franja(bloque.getDiaSemana(), bloque.getHoraInicio(), bloque.getHoraFin());
			ocupadas.computeIfAbsent(franja.dia, d -> new ArrayList<>()).add(franja);
			porAsignatura.computeIfAbsent(bloque.getAsignatura().getId(), id -> new ArrayList<>()).add(franja);
		}

		Solucion solucion = new Solucion();
		for (Asignatura asignatura : orden) {
			List<Franja> propias = porAsignatura.computeIfAbsent(asignatura.getId(), id -> new ArrayList<>());
			for (int sesion = 0; sesion < faltantes.get(asignatura.getId()); sesion++) {
				Franja elegida = null;
				double costoElegida = 0;
				double puntajeElegida = Double.MAX_VALUE;
				for (Franja franja : franjas) {
					if (ocupadas.getOrDefault(franja.dia, List.of()).stream().anyMatch(franja::seCruzaCon)) {
						continue;
					}
					double costo = costo(franja, propias, ocupadas);
					double puntaje = costo + azar.nextDouble() * RUIDO;
					if (puntaje < puntajeElegida) {
						elegida = franja;
						costoElegida = costo;
						puntajeElegida = puntaje;
					}
				}
				if (elegida == null) {
					break;
				}
				ocupadas.computeIfAbsent(elegida.dia, d -> new ArrayList<>()).add(elegida);
				propias.add(elegida);
				solucion.agregar(new Ubicado(asignatura, elegida), costoElegida);
			}
		}
		return solucion;
	}

	private static double costo(Franja franja, List<Franja> propias, Map<DiaSemana, List<Franja>> ocupadas) {
		double costo = 0;
		for (Franja otra : propias) {
			int distancia = Math.abs(franja.dia.ordinal() - otra.dia.ordinal());
			if (distancia == 0) {
				costo += COSTO_MISMO_DIA;
			} else if (distancia == 1) {
				costo += COSTO_DIA_CONSECUTIVO;
			}
			if (!franja.inicio.equals(otra.inicio)) {
				costo += COSTO_HORA_DISTINTA;
			}
		}
		if (franja.dia == DiaSemana.SABADO) {
			costo += COSTO_SABADO;
		}
		if (franja.fin.isAfter(LocalTime.of(19, 0))) {
			costo += COSTO_NOCHE;
		}
		if (!HORAS_ALINEADAS.contains(franja.inicio.getHour())) {
			costo += COSTO_HORA_DESALINEADA;
		}
		costo += COSTO_POR_BLOQUE_DEL_DIA * ocupadas.getOrDefault(franja.dia, List.of()).size();
		return costo;
	}

	private record Franja(DiaSemana dia, LocalTime inicio, LocalTime fin) {

		boolean seCruzaCon(Franja otra) {
			return dia == otra.dia && inicio.isBefore(otra.fin) && otra.inicio.isBefore(fin);
		}
	}

	private record Ubicado(Asignatura asignatura, Franja franja) {
	}

	private static final class Solucion {

		private final List<Ubicado> ubicados = new ArrayList<>();
		private double costo;

		void agregar(Ubicado ubicado, double costoUbicado) {
			ubicados.add(ubicado);
			costo += costoUbicado;
		}

		boolean esMejorQue(Solucion otra) {
			if (ubicados.size() != otra.ubicados.size()) {
				return ubicados.size() > otra.ubicados.size();
			}
			return costo < otra.costo;
		}
	}
}
