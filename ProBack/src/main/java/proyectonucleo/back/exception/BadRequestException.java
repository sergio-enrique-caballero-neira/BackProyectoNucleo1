package proyectonucleo.back.exception;

/**
 * Datos que incumplen una regla de negocio (HTTP 400). Puede indicar el campo afectado para
 * que el cliente muestre el mensaje junto a él.
 */
public class BadRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String campo;

	public BadRequestException(String mensaje) {
		this(null, mensaje);
	}

	public BadRequestException(String campo, String mensaje) {
		super(mensaje);
		this.campo = campo;
	}

	public String getCampo() {
		return campo;
	}
}
