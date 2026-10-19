package proyectonucleo.back.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales enviadas a {@code POST /api/auth/login}.
 *
 * @param username nombre de usuario
 * @param password contraseña en texto plano (solo viaja en la petición, nunca se guarda)
 */
public record LoginRequestDTO(
		@NotBlank(message = "El usuario es obligatorio.") @Schema(example = "admin") String username,
		@NotBlank(message = "La contraseña es obligatoria.") String password) {
}
