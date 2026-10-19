package proyectonucleo.back.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import proyectonucleo.back.entity.Asignatura;

/**
 * Acceso a la tabla ASIGNATURA.
 */
public interface AsignaturaRepository extends JpaRepository<Asignatura, Long> {

	boolean existsByCodigoIgnoreCase(String codigo);

	boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

	List<Asignatura> findAllByOrderByCodigoAsc();
}
