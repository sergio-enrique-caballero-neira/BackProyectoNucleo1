package proyectonucleo.back.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import proyectonucleo.back.dto.AsignaturaDTO;
import proyectonucleo.back.entity.Asignatura;
import proyectonucleo.back.entity.Programa;
import proyectonucleo.back.exception.ConflictException;
import proyectonucleo.back.exception.ResourceNotFoundException;
import proyectonucleo.back.repository.AsignaturaRepository;
import proyectonucleo.back.repository.HorarioRepository;
import proyectonucleo.back.repository.ProgramaRepository;

/**
 * Lógica de negocio de asignaturas (RF-01 a RF-04).
 */
@Service
public class AsignaturaService implements CRUDoperation<AsignaturaDTO> {

	private final AsignaturaRepository asignaturaRepository;
	private final HorarioRepository horarioRepository;
	private final ProgramaRepository programaRepository;

	public AsignaturaService(AsignaturaRepository asignaturaRepository, HorarioRepository horarioRepository,
			ProgramaRepository programaRepository) {
		this.asignaturaRepository = asignaturaRepository;
		this.horarioRepository = horarioRepository;
		this.programaRepository = programaRepository;
	}

	/** RF-01. El código se guarda sin espacios y en mayúsculas; no puede repetirse (RV-02). */
	@Override
	@Transactional
	public AsignaturaDTO create(AsignaturaDTO data) {
		normalizar(data);
		if (asignaturaRepository.existsByCodigoIgnoreCase(data.getCodigo())) {
			throw codigoDuplicado(data.getCodigo());
		}
		Asignatura entidad = new Asignatura(data.getCodigo(), data.getNombre(), data.getCreditos(),
				data.getSesionesSemana());
		entidad.setProgramas(resolverProgramas(data.getProgramaIds()));
		return toDto(asignaturaRepository.save(entidad));
	}

	/** RF-02. Catálogo ordenado por código. */
	@Override
	@Transactional(readOnly = true)
	public List<AsignaturaDTO> getAll() {
		return asignaturaRepository.findAllByOrderByCodigoAsc().stream().map(AsignaturaService::toDto).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public AsignaturaDTO getById(Long id) {
		return toDto(buscar(id));
	}

	/**
	 * RF-03. No permite reducir las sesiones semanales por debajo de los bloques que la
	 * asignatura ya tiene en alguna programación, para no superar su frecuencia (RV-07).
	 */
	@Override
	@Transactional
	public AsignaturaDTO updateById(Long id, AsignaturaDTO data) {
		Asignatura entidad = buscar(id);
		normalizar(data);
		if (asignaturaRepository.existsByCodigoIgnoreCaseAndIdNot(data.getCodigo(), id)) {
			throw codigoDuplicado(data.getCodigo());
		}
		long programados = horarioRepository.contarPorProgramacion(id).stream().findFirst().orElse(0L);
		if (data.getSesionesSemana() < programados) {
			throw new ConflictException("sesionesSemana", "La asignatura tiene " + programados
					+ " bloques en un mismo horario; elimine bloques antes de reducir las sesiones a "
					+ data.getSesionesSemana() + ".");
		}
		entidad.setCodigo(data.getCodigo());
		entidad.setNombre(data.getNombre());
		entidad.setCreditos(data.getCreditos());
		entidad.setSesionesSemana(data.getSesionesSemana());
		entidad.setProgramas(resolverProgramas(data.getProgramaIds()));
		return toDto(asignaturaRepository.save(entidad));
	}

	/** RF-04 / RV-09. Rechaza la eliminación si la asignatura aún tiene bloques horarios. */
	@Override
	@Transactional
	public void deleteById(Long id) {
		Asignatura entidad = buscar(id);
		long programados = horarioRepository.countByAsignaturaId(id);
		if (programados > 0) {
			throw new ConflictException("No se puede eliminar " + entidad.getCodigo() + " porque tiene "
					+ programados + (programados == 1 ? " bloque horario asociado" : " bloques horarios asociados")
					+ ". Elimínelos primero.");
		}
		asignaturaRepository.delete(entidad);
	}

	private Asignatura buscar(Long id) {
		return asignaturaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("La asignatura con id " + id + " no existe."));
	}

	private Set<Programa> resolverProgramas(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return new HashSet<>();
		}
		Set<Long> buscados = new HashSet<>(ids);
		Set<Programa> programas = new HashSet<>(programaRepository.findAllById(buscados));
		if (programas.size() < buscados.size()) {
			Set<Long> encontrados = programas.stream().map(Programa::getId).collect(Collectors.toSet());
			Long faltante = buscados.stream().filter(id -> !encontrados.contains(id)).findFirst().orElseThrow();
			throw new ResourceNotFoundException("El programa con id " + faltante + " no existe.");
		}
		return programas;
	}

	private void normalizar(AsignaturaDTO data) {
		data.setCodigo(data.getCodigo().trim().toUpperCase());
		data.setNombre(data.getNombre().trim());
	}

	private ConflictException codigoDuplicado(String codigo) {
		return new ConflictException("codigo", "Ya existe una asignatura con el código " + codigo + ".");
	}

	static AsignaturaDTO toDto(Asignatura entidad) {
		AsignaturaDTO dto = new AsignaturaDTO(entidad.getCodigo(), entidad.getNombre(), entidad.getCreditos(),
				entidad.getSesionesSemana());
		dto.setId(entidad.getId());
		dto.setProgramaIds(entidad.getProgramas().stream().map(Programa::getId).sorted().toList());
		return dto;
	}
}
