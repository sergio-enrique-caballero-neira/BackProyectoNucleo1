package proyectonucleo.back.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import proyectonucleo.back.entity.DiaSemana;
import proyectonucleo.back.exception.BadRequestException;

class ReglasHorarioValidatorTest {

	private final ReglasHorarioValidator reglas = new ReglasHorarioValidator();

	@ParameterizedTest(name = "{0} {1}-{2} es válido")
	@CsvSource({
			"LUNES, 07:00, 09:00",
			"MARTES, 10:00, 12:00",
			"MIERCOLES, 13:00, 15:00",
			"VIERNES, 19:00, 21:00",
			"SABADO, 07:00, 09:00",
			"SABADO, 10:00, 12:00" })
	void aceptaBloquesDentroDeLasReglas(DiaSemana dia, LocalTime inicio, LocalTime fin) {
		assertThatCode(() -> reglas.validarBloque(dia, inicio, fin)).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "{0} {1}-{2} se rechaza en {3}")
	@CsvSource({
			"LUNES, 09:00, 08:00, horaFin",
			"LUNES, 07:00, 08:00, horaFin",
			"LUNES, 07:00, 10:00, horaFin",
			"LUNES, 06:00, 08:00, horaInicio",
			"VIERNES, 20:00, 22:00, horaFin",
			"JUEVES, 21:00, 23:00, horaFin",
			"SABADO, 13:00, 15:00, horaFin",
			"SABADO, 11:00, 13:00, horaInicio",
			"LUNES, 11:00, 13:00, horaInicio",
			"LUNES, 12:00, 14:00, horaInicio",
			"LUNES, 11:30, 13:30, horaInicio" })
	void rechazaBloquesFueraDeLasReglas(DiaSemana dia, LocalTime inicio, LocalTime fin, String campo) {
		assertThatThrownBy(() -> reglas.validarBloque(dia, inicio, fin))
				.isInstanceOf(BadRequestException.class)
				.extracting(e -> ((BadRequestException) e).getCampo())
				.isEqualTo(campo);
	}

	@Test
	void mensajeDeAlmuerzoExplicaLaRegla() {
		assertThatThrownBy(() -> reglas.validarBloque(DiaSemana.LUNES, LocalTime.of(11, 0), LocalTime.of(13, 0)))
				.hasMessage("La sesión de 11:00 a 13:00 invade el descanso de 12:00 a 13:00.");
	}

	@Test
	void detectaSolapamientos() {
		LocalTime siete = LocalTime.of(7, 0);
		LocalTime ocho = LocalTime.of(8, 0);
		LocalTime nueve = LocalTime.of(9, 0);
		LocalTime once = LocalTime.of(11, 0);
		assertThat(reglas.seSolapan(siete, nueve, ocho, LocalTime.of(10, 0))).isTrue();
		assertThat(reglas.seSolapan(siete, nueve, siete, nueve)).isTrue();
		assertThat(reglas.seSolapan(siete, nueve, nueve, once)).isFalse();
	}
}
