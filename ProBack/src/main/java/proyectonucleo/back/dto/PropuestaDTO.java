package proyectonucleo.back.dto;

import java.util.List;

/**
 * Propuesta de la generación automática. No se guarda nada hasta que el Director la aplica.
 *
 * @param semilla     semilla con la que se generó; repetirla produce la misma propuesta
 * @param reorganizar si la propuesta reemplaza los bloques actuales
 * @param bloques     bloques nuevos sugeridos
 * @param sinUbicar   asignaturas con sesiones que no cupieron en la semana
 */
public record PropuestaDTO(long semilla, boolean reorganizar, List<BloquePropuestoDTO> bloques,
		List<AsignaturaPendienteDTO> sinUbicar) {

	/**
	 * Asignatura a la que le quedaron sesiones por ubicar.
	 *
	 * @param asignaturaId      asignatura
	 * @param codigo            código de la asignatura
	 * @param nombre            nombre de la asignatura
	 * @param sesionesFaltantes sesiones que no se pudieron ubicar
	 */
	public record AsignaturaPendienteDTO(Long asignaturaId, String codigo, String nombre, int sesionesFaltantes) {
	}
}
