package proyectonucleo.back.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

/**
 * Pide una propuesta de horario para las asignaturas indicadas.
 *
 * @param asignaturaIds asignaturas a organizar en la semana
 * @param reorganizar   {@code true} para ignorar los bloques actuales y proponer la semana
 *                      completa; {@code false} para conservarlos y completar las sesiones que
 *                      faltan
 * @param semilla       semilla de la variante; si se omite se elige una al azar
 */
public record SolicitudPropuestaDTO(
		@NotEmpty(message = "Seleccione al menos una asignatura.") @Schema(example = "[1, 2, 3]") List<Long> asignaturaIds,
		@Schema(example = "false") boolean reorganizar,
		@Schema(example = "42") Long semilla) {
}
