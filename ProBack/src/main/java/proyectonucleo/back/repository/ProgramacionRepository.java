package proyectonucleo.back.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import proyectonucleo.back.entity.Programacion;

/**
 * Acceso a la tabla PROGRAMACION. Los listados cargan el programa en la misma consulta.
 */
public interface ProgramacionRepository extends JpaRepository<Programacion, Long> {

	boolean existsByCodigoIgnoreCase(String codigo);

	boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

	@EntityGraph(attributePaths = "programa")
	List<Programacion> findAllByOrderByCodigoAsc();

	long countByProgramaId(Long programaId);
}
