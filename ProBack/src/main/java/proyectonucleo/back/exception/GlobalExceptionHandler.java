package proyectonucleo.back.exception;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.servlet.http.HttpServletRequest;
import proyectonucleo.back.dto.ErrorResponseDTO;

/**
 * Traduce las excepciones a respuestas JSON con el código HTTP del contrato de la API. Nunca
 * expone trazas internas, credenciales ni tokens.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponseDTO> validacion(MethodArgumentNotValidException e, HttpServletRequest req) {
		Map<String, String> campos = e.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage(),
						(primero, segundo) -> primero, LinkedHashMap::new));
		return responder(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud.", req, campos);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponseDTO> badRequest(BadRequestException e, HttpServletRequest req) {
		return responder(HttpStatus.BAD_REQUEST, e.getMessage(), req, campo(e.getCampo(), e.getMessage()));
	}

	/** JSON mal formado, día inexistente u hora que no sigue el formato HH:mm. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponseDTO> cuerpoIlegible(HttpMessageNotReadableException e,
			HttpServletRequest req) {
		if (e.getCause() instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
			String nombre = mapping.getPath().get(mapping.getPath().size() - 1).getFieldName();
			String mensaje = mapping.getCause() instanceof IllegalArgumentException causa ? causa.getMessage()
					: "Valor inválido para '" + nombre + "'" + (nombre.startsWith("hora") ? "; use el formato HH:mm." : ".");
			return responder(HttpStatus.BAD_REQUEST, mensaje, req, campo(nombre, mensaje));
		}
		return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido.", req, null);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponseDTO> tipoInvalido(MethodArgumentTypeMismatchException e,
			HttpServletRequest req) {
		return responder(HttpStatus.BAD_REQUEST, "El parámetro '" + e.getName() + "' no es válido.", req, null);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponseDTO> autenticacion(AuthenticationException e, HttpServletRequest req) {
		return responder(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.", req, null);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponseDTO> noEncontrado(ResourceNotFoundException e, HttpServletRequest req) {
		return responder(HttpStatus.NOT_FOUND, e.getMessage(), req, null);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponseDTO> rutaInexistente(NoResourceFoundException e, HttpServletRequest req) {
		return responder(HttpStatus.NOT_FOUND, "La ruta solicitada no existe.", req, null);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponseDTO> metodo(HttpRequestMethodNotSupportedException e,
			HttpServletRequest req) {
		return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método " + e.getMethod() + " no permitido en esta ruta.",
				req, null);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponseDTO> conflicto(ConflictException e, HttpServletRequest req) {
		return responder(HttpStatus.CONFLICT, e.getMessage(), req, campo(e.getCampo(), e.getMessage()));
	}

	/** Respaldo de las restricciones de MySQL ante peticiones concurrentes (RNF-02). */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponseDTO> integridad(DataIntegrityViolationException e, HttpServletRequest req) {
		log.warn("Violación de integridad en {}: {}", req.getRequestURI(), e.getMostSpecificCause().getMessage());
		return responder(HttpStatus.CONFLICT,
				"La operación entra en conflicto con datos existentes (registro duplicado o con dependencias).", req,
				null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDTO> inesperado(Exception e, HttpServletRequest req) {
		log.error("Error no controlado en {}", req.getRequestURI(), e);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno. Intente de nuevo.", req, null);
	}

	private static Map<String, String> campo(String nombre, String mensaje) {
		return nombre == null ? null : Map.of(nombre, mensaje);
	}

	private static ResponseEntity<ErrorResponseDTO> responder(HttpStatus estado, String mensaje,
			HttpServletRequest req, Map<String, String> campos) {
		return ResponseEntity.status(estado).body(new ErrorResponseDTO(OffsetDateTime.now(), estado.value(),
				estado.getReasonPhrase(), mensaje, req.getRequestURI(), campos));
	}
}
