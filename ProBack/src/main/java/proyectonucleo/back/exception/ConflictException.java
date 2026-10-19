package proyectonucleo.back.exception;

/**
 * La operación choca con datos existentes: duplicados o dependencias (HTTP 409).
 */
public class ConflictException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String campo;

	public ConflictException(String mensaje) {
		this(null, mensaje);
	}

	public ConflictException(String campo, String mensaje) {
		super(mensaje);
		this.campo = campo;
	}

	public String getCampo() {
		return campo;
	}
}
