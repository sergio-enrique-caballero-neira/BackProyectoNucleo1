package proyectonucleo.back.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Propuesta aprobada por el Director, con los ajustes que haya hecho en la rejilla.
 *
 * @param reorganizar {@code true} para eliminar los bloques actuales antes de guardar
 * @param bloques     bloques que se guardarán
 */
public record AplicarPropuestaDTO(@Schema(example = "false") boolean reorganizar,
		@NotNull(message = "Los bloques son obligatorios.") List<@Valid BloquePropuestoDTO> bloques) {
}
