package proyectonucleo.back.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import proyectonucleo.back.dto.HorarioDTO;
import proyectonucleo.back.entity.Asignatura;
import proyectonucleo.back.entity.Horario;
import proyectonucleo.back.entity.Programacion;
import proyectonucleo.back.exception.ConflictException;
import proyectonucleo.back.exception.ResourceNotFoundException;
import proyectonucleo.back.repository.AsignaturaRepository;
import proyectonucleo.back.repository.HorarioRepository;
import proyectonucleo.back.repository.ProgramacionRepository;

/**
 * Lógica de negocio de bloques horarios (RF-05 a RF-08). Toda creación o modificación se
 * valida aquí antes de persistir (RNF-03).
 */
@Service
public class HorarioService implements CRUDoperation<HorarioDTO> {

	/** Orden de la programación: día de la semana, hora de inicio y código de asignatura. */
	static final Comparator<Horario> ORDEN = Comparator.comparing(Horario::getDiaSemana)
			.thenComparing(Horario::getHoraInicio)
			.thenComparing(horario -> horario.getAsignatura().getCodigo());

	private final HorarioRepository horarioRepository;
	private final AsignaturaRepository asignaturaRepository;
	private final ProgramacionRepository programacionRepository;
	private final ReglasHorarioValidator reglas;

	public HorarioService(HorarioRepository horarioRepository, AsignaturaRepository asignaturaRepository,
			ProgramacionRepository programacionRepository, ReglasHorarioValidator reglas) {
		this.horarioRepository = horarioRepository;
		this.asignaturaRepository = asignaturaRepository;
		this.programacionRepository = programacionRepository;
		this.reglas = reglas;
	}

	/** RF-05. */
	@Override
	@Transactional
	public HorarioDTO create(HorarioDTO data) {
		Programacion programacion = buscarProgramacion(data.getProgramacionId());
		Asignatura asignatura = buscarAsignatura(data.getAsignaturaId());
		validar(null, programacion, asignatura, data);
		Horario nuevo = new Horario(programacion, asignatura, data.getDiaSemana(), data.getHoraInicio(),
				data.getHoraFin());
		return toDto(horarioRepository.save(nuevo));
	}

	/** RF-06. Todos los bloques, ordenados por día y hora. */
	@Override
	@Transactional(readOnly = true)
	public List<HorarioDTO> getAll() {
		return ordenar(horarioRepository.findAll());
	}

	/**
	 * RF-06 con filtros opcionales.
	 *
	 * @param programacionId programación cuyos bloques se consultan, o {@code null}
	 * @param asignaturaId   asignatura cuyos bloques se consultan, o {@code null}
	 * @return bloques que cumplen los filtros, ordenados por día y hora
	 */
	@Transactional(readOnly = true)
	public List<HorarioDTO> buscar(Long programacionId, Long asignaturaId) {
		if (programacionId != null && asignaturaId != null) {
			return ordenar(horarioRepository.findByProgramacionIdAndAsignaturaId(programacionId, asignaturaId));
		}
		if (programacionId != null) {
			return ordenar(horarioRepository.findByProgramacionId(programacionId));
		}
		if (asignaturaId != null) {
			return ordenar(horarioRepository.findByAsignaturaId(asignaturaId));
		}
		return getAll();
	}

	@Override
	@Transactional(readOnly = true)
	public HorarioDTO getById(Long id) {
		return toDto(buscar(id));
	}

	/** RF-07. Vuelve a aplicar todas las reglas sobre los nuevos valores. */
	@Override
	@Transactional
	public HorarioDTO updateById(Long id, HorarioDTO data) {
		Horario actual = buscar(id);
		Programacion programacion = buscarProgramacion(data.getProgramacionId());
		Asignatura asignatura = buscarAsignatura(data.getAsignaturaId());
		validar(actual, programacion, asignatura, data);
		actual.setProgramacion(programacion);
		actual.setAsignatura(asignatura);
		actual.setDiaSemana(data.getDiaSemana());
		actual.setHoraInicio(data.getHoraInicio());
		actual.setHoraFin(data.getHoraFin());
		return toDto(horarioRepository.save(actual));
	}

	/** RF-08. */
	@Override
	@Transactional
	public void deleteById(Long id) {
		horarioRepository.delete(buscar(id));
	}

	/**
	 * Aplica las reglas horarias (RV-04 a RV-06), el límite de sesiones semanales de la
	 * asignatura dentro de la programación (RV-07) y la prohibición de bloques cruzados en la
	 * misma programación (RV-08).
	 *
	 * @param actual       bloque que se modifica, o {@code null} si se está creando
	 * @param programacion programación a la que quedará asociado el bloque
	 * @param asignatura   asignatura a la que quedará asociado el bloque
	 * @param data         nuevos valores
	 */
	private void validar(Horario actual, Programacion programacion, Asignatura asignatura, HorarioDTO data) {
		reglas.validarBloque(data.getDiaSemana(), data.getHoraInicio(), data.getHoraFin());

		boolean ocupaSesionNueva = actual == null
				|| !actual.getProgramacion().getId().equals(programacion.getId())
				|| !actual.getAsignatura().getId().equals(asignatura.getId());
		if (ocupaSesionNueva && horarioRepository.countByProgramacionIdAndAsignaturaId(programacion.getId(),
				asignatura.getId()) >= asignatura.getSesionesSemana()) {
			throw new ConflictException("asignaturaId", asignatura.getNombre() + " ya alcanzó sus "
					+ asignatura.getSesionesSemana() + " sesiones semanales.");
		}

		horarioRepository.findByProgramacionIdAndDiaSemana(programacion.getId(), data.getDiaSemana()).stream()
				.filter(otro -> actual == null || !otro.getId().equals(actual.getId()))
				.filter(otro -> reglas.seSolapan(otro.getHoraInicio(), otro.getHoraFin(), data.getHoraInicio(),
						data.getHoraFin()))
				.findFirst()
				.ifPresent(otro -> {
					throw new ConflictException("horaInicio", "El " + otro.getDiaSemana().getNombre() + " de "
							+ otro.getHoraInicio() + " a " + otro.getHoraFin() + " ya está ocupado por "
							+ otro.getAsignatura().getNombre() + ".");
				});
	}

	private Horario buscar(Long id) {
		return horarioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("El bloque horario con id " + id + " no existe."));
	}

	private Programacion buscarProgramacion(Long id) {
		return programacionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("El horario con id " + id + " no existe."));
	}

	private Asignatura buscarAsignatura(Long id) {
		return asignaturaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("La asignatura con id " + id + " no existe."));
	}

	private List<HorarioDTO> ordenar(List<Horario> horarios) {
		return horarios.stream().sorted(ORDEN).map(HorarioService::toDto).toList();
	}

	static HorarioDTO toDto(Horario entidad) {
		Asignatura asignatura = entidad.getAsignatura();
		HorarioDTO dto = new HorarioDTO(entidad.getProgramacion().getId(), asignatura.getId(),
				entidad.getDiaSemana(), entidad.getHoraInicio(), entidad.getHoraFin());
		dto.setId(entidad.getId());
		dto.setAsignaturaCodigo(asignatura.getCodigo());
		dto.setAsignaturaNombre(asignatura.getNombre());
		return dto;
	}
}
