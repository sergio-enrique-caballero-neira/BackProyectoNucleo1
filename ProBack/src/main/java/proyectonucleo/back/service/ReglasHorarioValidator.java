package proyectonucleo.back.service;

import java.time.Duration;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import proyectonucleo.back.entity.DiaSemana;
import proyectonucleo.back.exception.BadRequestException;

/**
 * Reglas horarias institucionales (R-01 a R-03, RV-04 a RV-06). Concentra los valores en un
 * solo lugar para poder cambiar los límites sin tocar el resto del código.
 */
@Component
public class ReglasHorarioValidator {

	public static final Duration DURACION_SESION = Duration.ofHours(2);
	public static final LocalTime INICIO_JORNADA = LocalTime.of(7, 0);
	public static final LocalTime FIN_JORNADA_SEMANA = LocalTime.of(21, 0);
	public static final LocalTime FIN_JORNADA_SABADO = LocalTime.of(13, 0);
	public static final LocalTime INICIO_ALMUERZO = LocalTime.of(12, 0);
	public static final LocalTime FIN_ALMUERZO = LocalTime.of(13, 0);

	/**
	 * Comprueba que el bloque dure dos horas, esté dentro de la franja del día y no cruce el
	 * almuerzo.
	 *
	 * @param dia    día del bloque
	 * @param inicio hora de inicio
	 * @param fin    hora de fin
	 * @throws BadRequestException con el campo y la regla incumplida
	 */
	public void validarBloque(DiaSemana dia, LocalTime inicio, LocalTime fin) {
		if (!fin.isAfter(inicio)) {
			throw new BadRequestException("horaFin", "La hora de fin debe ser posterior a la hora de inicio.");
		}
		if (!Duration.between(inicio, fin).equals(DURACION_SESION)) {
			throw new BadRequestException("horaFin", "La sesión debe durar exactamente 2 horas; de "
					+ inicio + " a " + fin + " no cumple.");
		}
		if (inicio.isBefore(INICIO_JORNADA)) {
			throw new BadRequestException("horaInicio", "La jornada inicia a las " + INICIO_JORNADA + ".");
		}
		if (dia == DiaSemana.SABADO && fin.isAfter(FIN_JORNADA_SABADO)) {
			throw new BadRequestException("horaFin",
					"El sábado solo admite sesiones hasta las " + FIN_JORNADA_SABADO + ".");
		}
		if (fin.isAfter(FIN_JORNADA_SEMANA)) {
			throw new BadRequestException("horaFin",
					"La jornada de lunes a viernes termina a las " + FIN_JORNADA_SEMANA + ".");
		}
		if (inicio.isBefore(FIN_ALMUERZO) && fin.isAfter(INICIO_ALMUERZO)) {
			throw new BadRequestException("horaInicio", "La sesión de " + inicio + " a " + fin
					+ " invade el descanso de " + INICIO_ALMUERZO + " a " + FIN_ALMUERZO + ".");
		}
	}

	/**
	 * Indica si dos franjas del mismo día se cruzan.
	 *
	 * @param inicioA inicio de la primera franja
	 * @param finA    fin de la primera franja
	 * @param inicioB inicio de la segunda franja
	 * @param finB    fin de la segunda franja
	 * @return {@code true} si comparten algún instante
	 */
	public boolean seSolapan(LocalTime inicioA, LocalTime finA, LocalTime inicioB, LocalTime finB) {
		return inicioA.isBefore(finB) && inicioB.isBefore(finA);
	}
}
