package proyectonucleo.back.dto;

import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Formato uniforme de los errores de la API. {@code campos} relaciona cada campo inválido
 * con su mensaje, para que el cliente lo muestre junto al campo afectado.
 *
 * @param fecha   momento del error
 * @param estado  código HTTP
 * @param error   descripción corta del código HTTP
 * @param mensaje explicación comprensible para el usuario
 * @param ruta    ruta de la petición
 * @param campos  errores por campo; se omite cuando no aplica
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponseDTO(OffsetDateTime fecha, int estado, String error, String mensaje, String ruta,
		Map<String, String> campos) {
}
