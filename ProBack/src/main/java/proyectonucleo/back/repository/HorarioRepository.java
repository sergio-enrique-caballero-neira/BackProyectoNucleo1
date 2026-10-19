package proyectonucleo.back.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import proyectonucleo.back.entity.DiaSemana;
import proyectonucleo.back.entity.Horario;

/**
 * Acceso a la tabla HORARIO. Las consultas de listado cargan la asignatura en la misma
 * consulta para evitar una consulta adicional por bloque.
 */
public interface HorarioRepository extends JpaRepository<Horario, Long> {

	@Override
	@EntityGraph(attributePaths = "asignatura")
	List<Horario> findAll();

	@EntityGraph(attributePaths = "asignatura")
	List<Horario> findByAsignaturaId(Long asignaturaId);

	@EntityGraph(attributePaths = "asignatura")
	List<Horario> findByProgramacionId(Long programacionId);

	@EntityGraph(attributePaths = "asignatura")
	List<Horario> findByProgramacionIdAndAsignaturaId(Long programacionId, Long asignaturaId);

	@EntityGraph(attributePaths = "asignatura")
	List<Horario> findByProgramacionIdAndDiaSemana(Long programacionId, DiaSemana diaSemana);

	long countByAsignaturaId(Long asignaturaId);

	long countByProgramacionIdAndAsignaturaId(Long programacionId, Long asignaturaId);

	/**
	 * Cantidad de bloques de una asignatura en cada programación, de mayor a menor.
	 *
	 * @param asignaturaId asignatura consultada
	 * @return un conteo por programación en la que aparece la asignatura
	 */
	@Query("select count(h) from Horario h where h.asignatura.id = :asignaturaId"
			+ " group by h.programacion.id order by count(h) desc")
	List<Long> contarPorProgramacion(@Param("asignaturaId") Long asignaturaId);

	@Modifying
	@Query("delete from Horario h where h.programacion.id = :programacionId")
	void eliminarPorProgramacion(@Param("programacionId") Long programacionId);
}
