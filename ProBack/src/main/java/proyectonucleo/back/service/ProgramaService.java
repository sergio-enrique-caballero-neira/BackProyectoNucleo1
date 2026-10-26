package proyectonucleo.back.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import proyectonucleo.back.dto.ProgramaDTO;
import proyectonucleo.back.entity.Programa;
import proyectonucleo.back.exception.ConflictException;
import proyectonucleo.back.exception.ResourceNotFoundException;
import proyectonucleo.back.repository.AsignaturaRepository;
import proyectonucleo.back.repository.ProgramaRepository;
import proyectonucleo.back.repository.ProgramacionRepository;

/**
 * Lógica de negocio de los programas académicos.
 */
@Service
public class ProgramaService implements CRUDoperation<ProgramaDTO> {

	private final ProgramaRepository programaRepository;
	private final AsignaturaRepository asignaturaRepository;
	private final ProgramacionRepository programacionRepository;

	public ProgramaService(ProgramaRepository programaRepository, AsignaturaRepository asignaturaRepository,
			ProgramacionRepository programacionRepository) {
		this.programaRepository = programaRepository;
		this.asignaturaRepository = asignaturaRepository;
		this.programacionRepository = programacionRepository;
	}

	/** El código se guarda en mayúsculas; ni el código ni el nombre pueden repetirse. */
	@Override
	@Transactional
	public ProgramaDTO create(ProgramaDTO data) {
		normalizar(data);
		if (programaRepository.existsByCodigoIgnoreCase(data.getCodigo())) {
			throw codigoDuplicado(data.getCodigo());
		}
		if (programaRepository.existsByNombreIgnoreCase(data.getNombre())) {
			throw nombreDuplicado(data.getNombre());
		}
		return toDto(programaRepository.save(new Programa(data.getCodigo(), data.getNombre())));
	}

	/** Programas ordenados por código, con su número de asignaturas y horarios. */
	@Override
	@Transactional(readOnly = true)
	public List<ProgramaDTO> getAll() {
		return programaRepository.findAllByOrderByCodigoAsc().stream().map(this::toDto).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ProgramaDTO getById(Long id) {
		return toDto(buscar(id));
	}

	@Override
	@Transactional
	public ProgramaDTO updateById(Long id, ProgramaDTO data) {
		Programa entidad = buscar(id);
		normalizar(data);
		if (programaRepository.existsByCodigoIgnoreCaseAndIdNot(data.getCodigo(), id)) {
			throw codigoDuplicado(data.getCodigo());
		}
		if (programaRepository.existsByNombreIgnoreCaseAndIdNot(data.getNombre(), id)) {
			throw nombreDuplicado(data.getNombre());
		}
		entidad.setCodigo(data.getCodigo());
		entidad.setNombre(data.getNombre());
		return toDto(programaRepository.save(entidad));
	}

	/**
	 * Rechaza la eliminación si el programa tiene horarios. Las asignaturas no se eliminan: solo
	 * dejan de ofrecerse en el programa.
	 */
	@Override
	@Transactional
	public void deleteById(Long id) {
		Programa entidad = buscar(id);
		long horarios = programacionRepository.countByProgramaId(id);
		if (horarios > 0) {
			throw new ConflictException("No se puede eliminar " + entidad.getNombre() + " porque tiene " + horarios
					+ (horarios == 1 ? " horario" : " horarios") + ". Elimínelos primero.");
		}
		asignaturaRepository.findByProgramasId(id).forEach(asignatura -> asignatura.getProgramas().remove(entidad));
		programaRepository.delete(entidad);
	}

	Programa buscar(Long id) {
		return programaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("El programa con id " + id + " no existe."));
	}

	private void normalizar(ProgramaDTO data) {
		data.setCodigo(data.getCodigo().trim().toUpperCase());
		data.setNombre(data.getNombre().trim());
	}

	private ConflictException codigoDuplicado(String codigo) {
		return new ConflictException("codigo", "Ya existe un programa con el código " + codigo + ".");
	}

	private ConflictException nombreDuplicado(String nombre) {
		return new ConflictException("nombre", "Ya existe un programa llamado " + nombre + ".");
	}

	private ProgramaDTO toDto(Programa entidad) {
		ProgramaDTO dto = new ProgramaDTO(entidad.getCodigo(), entidad.getNombre());
		dto.setId(entidad.getId());
		dto.setAsignaturas(asignaturaRepository.countByProgramasId(entidad.getId()));
		dto.setHorarios(programacionRepository.countByProgramaId(entidad.getId()));
		return dto;
	}
}
