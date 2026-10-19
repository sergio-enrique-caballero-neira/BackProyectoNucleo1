package proyectonucleo.back.exception;

/**
 * El recurso solicitado no existe (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(String mensaje) {
		super(mensaje);
	}
}
