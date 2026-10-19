package proyectonucleo.back.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import proyectonucleo.back.entity.Programacion;

/**
 * Acceso a la tabla PROGRAMACION.
 */
public interface ProgramacionRepository extends JpaRepository<Programacion, Long> {

	boolean existsByCodigoIgnoreCase(String codigo);

	boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

	List<Programacion> findAllByOrderByCodigoAsc();
}
