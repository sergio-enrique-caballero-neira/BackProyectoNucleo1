package proyectonucleo.back.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import proyectonucleo.back.entity.Programa;

/**
 * Acceso a la tabla PROGRAMA.
 */
public interface ProgramaRepository extends JpaRepository<Programa, Long> {

	boolean existsByCodigoIgnoreCase(String codigo);

	boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

	boolean existsByNombreIgnoreCase(String nombre);

	boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

	List<Programa> findAllByOrderByCodigoAsc();
}
