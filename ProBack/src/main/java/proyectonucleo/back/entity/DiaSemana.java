package proyectonucleo.back.entity;

import java.text.Normalizer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Días en los que se puede programar un bloque horario (RV-03: lunes a sábado).
 * <p>
 * En la base de datos se guarda el nombre de la constante (por ejemplo {@code MIERCOLES});
 * en JSON se expone el nombre legible ({@code "Miércoles"}).
 */
public enum DiaSemana {

	LUNES("Lunes"),
	MARTES("Martes"),
	MIERCOLES("Miércoles"),
	JUEVES("Jueves"),
	VIERNES("Viernes"),
	SABADO("Sábado");

	private final String nombre;

	DiaSemana(String nombre) {
		this.nombre = nombre;
	}

	/**
	 * Nombre legible del día, usado en las respuestas JSON y en los mensajes de validación.
	 *
	 * @return nombre del día con tildes, por ejemplo "Sábado"
	 */
	@JsonValue
	public String getNombre() {
		return nombre;
	}

	/**
	 * Convierte un texto en un día de la semana sin distinguir mayúsculas ni tildes.
	 * Acepta "Miércoles", "miercoles" o "MIERCOLES".
	 *
	 * @param valor texto recibido en la petición
	 * @return día correspondiente
	 * @throws IllegalArgumentException si el texto no corresponde a un día de lunes a sábado
	 */
	@JsonCreator
	public static DiaSemana desde(String valor) {
		if (valor != null) {
			String normalizado = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
					.replaceAll("\\p{M}", "")
					.toUpperCase();
			for (DiaSemana dia : values()) {
				if (dia.name().equals(normalizado)) {
					return dia;
				}
			}
		}
		throw new IllegalArgumentException("El día debe estar entre lunes y sábado.");
	}
}
