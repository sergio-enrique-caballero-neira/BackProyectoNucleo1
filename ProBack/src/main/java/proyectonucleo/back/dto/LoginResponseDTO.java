package proyectonucleo.back.dto;

/**
 * Respuesta de un inicio de sesión exitoso.
 *
 * @param token    JWT que el cliente debe enviar como {@code Authorization: Bearer <token>}
 * @param tipo     tipo de token, siempre "Bearer"
 * @param expiraEn milisegundos de validez del token desde su emisión
 * @param username usuario autenticado
 * @param rol      rol del usuario autenticado
 */
public record LoginResponseDTO(String token, String tipo, long expiraEn, String username, String rol) {
}
