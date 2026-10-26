package proyectonucleo.back.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import proyectonucleo.back.dto.HorarioDTO;
import proyectonucleo.back.dto.ProgramacionDTO;
import proyectonucleo.back.entity.Horario;
import proyectonucleo.back.entity.Programa;
import proyectonucleo.back.entity.Programacion;
import proyectonucleo.back.exception.ConflictException;
import proyectonucleo.back.exception.ResourceNotFoundException;
import proyectonucleo.back.repository.HorarioRepository;
import proyectonucleo.back.repository.ProgramacionRepository;

/**
 * Lógica de negocio de las programaciones semanales ("Horarios" en la interfaz), que
 * agrupan los bloques horarios de varias asignaturas de un programa.
 */
@Service
public class ProgramacionService implements CRUDoperation<ProgramacionDTO> {

	private final ProgramacionRepository programacionRepository;
	private final HorarioRepository horarioRepository;
	private final ProgramaService programaService;

	public ProgramacionService(ProgramacionRepository programacionRepository, HorarioRepository horarioRepository,
			ProgramaService programaService) {
		this.programacionRepository = programacionRepository;
		this.horarioRepository = horarioRepository;
		this.programaService = programaService;
	}

	/** El código se guarda sin espacios y en mayúsculas; no puede repetirse. */
	@Override
	@Transactional
	public ProgramacionDTO create(ProgramacionDTO data) {
		normalizar(data);
		if (programacionRepository.existsByCodigoIgnoreCase(data.getCodigo())) {
			throw codigoDuplicado(data.getCodigo());
		}
		Programa programa = programaService.buscar(data.getProgramaId());
		Programacion entidad = new Programacion(data.getCodigo(), programa, data.getDescripcion());
		return toDto(programacionRepository.save(entidad), List.of());
	}

	/** Todas las programaciones con sus bloques, ordenadas por código. */
	@Override
	@Transactional(readOnly = true)
	public List<ProgramacionDTO> getAll() {
		Map<Long, List<Horario>> bloques = horarioRepository.findAll().stream()
				.collect(Collectors.groupingBy(horario -> horario.getProgramacion().getId()));
		return programacionRepository.findAllByOrderByCodigoAsc().stream()
				.map(programacion -> toDto(programacion, bloques.getOrDefault(programacion.getId(), List.of())))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ProgramacionDTO getById(Long id) {
		return toDto(buscar(id), horarioRepository.findByProgramacionId(id));
	}

	@Override
	@Transactional
	public ProgramacionDTO updateById(Long id, ProgramacionDTO data) {
		Programacion entidad = buscar(id);
		normalizar(data);
		if (programacionRepository.existsByCodigoIgnoreCaseAndIdNot(data.getCodigo(), id)) {
			throw codigoDuplicado(data.getCodigo());
		}
		entidad.setCodigo(data.getCodigo());
		entidad.setPrograma(programaService.buscar(data.getProgramaId()));
		entidad.setDescripcion(data.getDescripcion());
		return toDto(programacionRepository.save(entidad), horarioRepository.findByProgramacionId(id));
	}

	/** Elimina la programación junto con todos sus bloques, en una sola transacción. */
	@Override
	@Transactional
	public void deleteById(Long id) {
		Programacion entidad = buscar(id);
		horarioRepository.eliminarPorProgramacion(id);
		programacionRepository.delete(entidad);
	}

	Programacion buscar(Long id) {
		return programacionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("El horario con id " + id + " no existe."));
	}

	/** La descripción vacía se guarda como {@code null}. */
	private void normalizar(ProgramacionDTO data) {
		data.setCodigo(data.getCodigo().trim().toUpperCase());
		String descripcion = data.getDescripcion() == null ? "" : data.getDescripcion().trim();
		data.setDescripcion(descripcion.isEmpty() ? null : descripcion);
	}

	private ConflictException codigoDuplicado(String codigo) {
		return new ConflictException("codigo", "Ya existe un horario con el código " + codigo + ".");
	}

	ProgramacionDTO toDto(Programacion entidad, List<Horario> bloques) {
		Programa programa = entidad.getPrograma();
		ProgramacionDTO dto = new ProgramacionDTO(entidad.getCodigo(), programa.getId(), entidad.getDescripcion());
		dto.setId(entidad.getId());
		dto.setProgramaCodigo(programa.getCodigo());
		dto.setProgramaNombre(programa.getNombre());
		List<HorarioDTO> ordenados = bloques.stream().sorted(HorarioService.ORDEN).map(HorarioService::toDto)
				.toList();
		dto.setBloques(ordenados);
		return dto;
	}
}
