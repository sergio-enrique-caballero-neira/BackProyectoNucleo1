package proyectonucleo.back.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import proyectonucleo.back.entity.DiaSemana;

/**
 * Bloque sugerido por la generación automática, todavía sin guardar. Al aplicar la propuesta
 * el cliente lo devuelve, quizá movido por el Director; el código y el nombre de la
 * asignatura se ignoran en la petición.
 *
 * @param asignaturaId     asignatura del bloque
 * @param asignaturaCodigo código de la asignatura (solo respuesta)
 * @param asignaturaNombre nombre de la asignatura (solo respuesta)
 * @param diaSemana        día del bloque
 * @param horaInicio       hora de inicio
 * @param horaFin          hora de fin
 */
public record BloquePropuestoDTO(
		@NotNull(message = "La asignatura es obligatoria.") @Schema(example = "1") Long asignaturaId,
		@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "SIS101") String asignaturaCodigo,
		@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "Programación I") String asignaturaNombre,
		@NotNull(message = "El día es obligatorio.") @Schema(type = "string", example = "Lunes") DiaSemana diaSemana,
		@NotNull(message = "La hora de inicio es obligatoria.") @JsonFormat(pattern = "HH:mm")
		@Schema(type = "string", pattern = "HH:mm", example = "07:00") LocalTime horaInicio,
		@NotNull(message = "La hora de fin es obligatoria.") @JsonFormat(pattern = "HH:mm")
		@Schema(type = "string", pattern = "HH:mm", example = "09:00") LocalTime horaFin) {
}
