package proyectonucleo.back.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import proyectonucleo.back.entity.Asignatura;

/**
 * Acceso a la tabla ASIGNATURA. Los listados cargan los programas en la misma consulta.
 */
public interface AsignaturaRepository extends JpaRepository<Asignatura, Long> {

	boolean existsByCodigoIgnoreCase(String codigo);

	boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

	@EntityGraph(attributePaths = "programas")
	List<Asignatura> findAllByOrderByCodigoAsc();

	@EntityGraph(attributePaths = "programas")
	List<Asignatura> findByProgramasId(Long programaId);

	long countByProgramasId(Long programaId);
}
