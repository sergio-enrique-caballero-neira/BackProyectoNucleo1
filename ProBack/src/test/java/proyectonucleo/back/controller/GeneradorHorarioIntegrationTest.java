package proyectonucleo.back.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import proyectonucleo.back.entity.DiaSemana;
import proyectonucleo.back.service.ReglasHorarioValidator;

/**
 * Generación automática de horarios: la propuesta cumple las reglas duras, respeta los
 * bloques existentes y solo se guarda al aplicarla.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GeneradorHorarioIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private ReglasHorarioValidator reglas;

	private String token;
	private long programa;
	private long programacion;

	@BeforeEach
	void preparar() throws Exception {
		String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin-test-123\"}"))
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		token = json.readTree(respuesta).get("token").asText();
		programa = crear("/api/programas", Map.of("codigo", "SIS", "nombre", "Ingeniería de Sistemas"));
		programacion = crear("/api/programaciones", Map.of("codigo", "SIS-S3", "programaId", programa));
	}

	@Test
	void propuestaCumpleLasReglasYReparteLasSesionesSinGuardar() throws Exception {
		List<Long> asignaturas = List.of(asignatura("SIS301", 3), asignatura("SIS302", 3), asignatura("SIS303", 2),
				asignatura("SIS304", 2), asignatura("SIS305", 4), asignatura("SIS306", 2));

		JsonNode propuesta = proponer(asignaturas, false, 7L);

		JsonNode bloques = propuesta.get("bloques");
		assertThat(bloques).hasSize(16);
		assertThat(propuesta.get("sinUbicar")).isEmpty();
		assertThat(propuesta.get("semilla").asLong()).isEqualTo(7L);
		cumpleReglasDuras(bloques);

		Map<Long, Set<String>> diasPorAsignatura = new HashMap<>();
		for (JsonNode bloque : bloques) {
			String dia = bloque.get("diaSemana").asText();
			assertThat(diasPorAsignatura.computeIfAbsent(bloque.get("asignaturaId").asLong(), id -> new HashSet<>())
					.add(dia)).as("sesiones de una asignatura en días distintos").isTrue();
			assertThat(dia).as("con espacio de sobra no se usa el sábado").isNotEqualTo("Sábado");
			assertThat(bloque.get("horaFin").asText()).as("ni la franja de 19:00 a 21:00").isNotEqualTo("21:00");
		}

		autenticado(get("/api/programaciones/" + programacion)).andExpect(jsonPath("$.bloques", hasSize(0)));
	}

	@Test
	void laMismaSemillaRepiteLaPropuestaYOtraSemillaOfreceVariantes() throws Exception {
		List<Long> asignaturas = List.of(asignatura("SIS301", 3), asignatura("SIS302", 2), asignatura("SIS303", 2));

		JsonNode primera = proponer(asignaturas, false, 11L).get("bloques");
		assertThat(proponer(asignaturas, false, 11L).get("bloques")).isEqualTo(primera);

		boolean hayVariante = false;
		for (long semilla = 12; semilla < 20 && !hayVariante; semilla++) {
			hayVariante = !proponer(asignaturas, false, semilla).get("bloques").equals(primera);
		}
		assertThat(hayVariante).isTrue();

		assertThat(proponer(asignaturas, false, null).get("semilla").asLong()).isPositive();
	}

	@Test
	void conservaLosBloquesExistentesOReorganizaDesdeCero() throws Exception {
		long calculo = asignatura("MAT101", 3);
		crear("/api/horarios", Map.of("programacionId", programacion, "asignaturaId", calculo, "diaSemana", "Lunes",
				"horaInicio", "07:00", "horaFin", "09:00"));

		JsonNode completar = proponer(List.of(calculo), false, 3L).get("bloques");
		assertThat(completar).hasSize(2);
		for (JsonNode bloque : completar) {
			assertThat(bloque.get("diaSemana").asText()).isNotEqualTo("Lunes");
		}

		assertThat(proponer(List.of(calculo), true, 3L).get("bloques")).hasSize(3);
	}

	@Test
	void informaLasSesionesQueNoCabenEnLaSemana() throws Exception {
		List<Long> asignaturas = new ArrayList<>();
		for (int i = 1; i <= 9; i++) {
			asignaturas.add(asignatura("SIS40" + i, 4));
		}

		JsonNode propuesta = proponer(asignaturas, false, 5L);

		int ubicadas = propuesta.get("bloques").size();
		int faltantes = 0;
		for (JsonNode pendiente : propuesta.get("sinUbicar")) {
			faltantes += pendiente.get("sesionesFaltantes").asInt();
		}
		assertThat(ubicadas).isLessThanOrEqualTo(32);
		assertThat(faltantes).isPositive();
		assertThat(ubicadas + faltantes).isEqualTo(36);
		cumpleReglasDuras(propuesta.get("bloques"));
	}

	/** Sin la transacción de la prueba, para comprobar el rollback real; limpia al terminar. */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void aplicarGuardaTodoOMuestraElErrorSinGuardarNada() throws Exception {
		long bases = asignatura("SIS201", 2);
		long redes = asignatura("SIS202", 2);
		try {
			aplicarYRevertir(bases, redes);
		} finally {
			autenticado(delete("/api/programaciones/" + programacion));
			autenticado(delete("/api/asignaturas/" + bases));
			autenticado(delete("/api/asignaturas/" + redes));
			autenticado(delete("/api/programas/" + programa));
		}
	}

	private void aplicarYRevertir(long bases, long redes) throws Exception {
		JsonNode propuesta = proponer(List.of(bases, redes), false, 9L);

		autenticado(post("/api/programaciones/" + programacion + "/propuesta/aplicar")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("reorganizar", false, "bloques", propuesta.get("bloques")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bloques", hasSize(4)));

		List<Map<String, Object>> invalidos = List.of(
				Map.of("asignaturaId", bases, "diaSemana", "Sábado", "horaInicio", "07:00", "horaFin", "09:00"),
				Map.of("asignaturaId", redes, "diaSemana", "Sábado", "horaInicio", "11:00", "horaFin", "13:00"));
		autenticado(post("/api/programaciones/" + programacion + "/propuesta/aplicar")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("reorganizar", true, "bloques", invalidos))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje", containsString("descanso de 12:00 a 13:00")));
		autenticado(get("/api/programaciones/" + programacion)).andExpect(jsonPath("$.bloques", hasSize(4)));

		autenticado(post("/api/programaciones/" + programacion + "/propuesta/aplicar")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("reorganizar", true, "bloques", invalidos.subList(0, 1)))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bloques", hasSize(1)))
				.andExpect(jsonPath("$.bloques[0].diaSemana").value("Sábado"));
	}

	@Test
	void solicitudInvalidaResponde400Y404() throws Exception {
		autenticado(post("/api/programaciones/" + programacion + "/propuesta").contentType(MediaType.APPLICATION_JSON)
				.content("{\"asignaturaIds\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.asignaturaIds").value("Seleccione al menos una asignatura."));
		autenticado(post("/api/programaciones/" + programacion + "/propuesta").contentType(MediaType.APPLICATION_JSON)
				.content("{\"asignaturaIds\":[999]}"))
				.andExpect(status().isNotFound());
		autenticado(post("/api/programaciones/999/propuesta").contentType(MediaType.APPLICATION_JSON)
				.content("{\"asignaturaIds\":[1]}"))
				.andExpect(status().isNotFound());
	}

	private void cumpleReglasDuras(JsonNode bloques) {
		List<JsonNode> vistos = new ArrayList<>();
		for (JsonNode bloque : bloques) {
			DiaSemana dia = DiaSemana.desde(bloque.get("diaSemana").asText());
			LocalTime inicio = LocalTime.parse(bloque.get("horaInicio").asText());
			LocalTime fin = LocalTime.parse(bloque.get("horaFin").asText());
			assertThat(reglas.esValido(dia, inicio, fin)).as(bloque.toString()).isTrue();
			for (JsonNode otro : vistos) {
				boolean mismoDia = otro.get("diaSemana").asText().equals(bloque.get("diaSemana").asText());
				boolean cruce = mismoDia && reglas.seSolapan(inicio, fin, LocalTime.parse(otro.get("horaInicio").asText()),
						LocalTime.parse(otro.get("horaFin").asText()));
				assertThat(cruce).as(bloque + " se cruza con " + otro).isFalse();
			}
			vistos.add(bloque);
		}
	}

	private JsonNode proponer(List<Long> asignaturas, boolean reorganizar, Long semilla) throws Exception {
		Map<String, Object> cuerpo = new HashMap<>();
		cuerpo.put("asignaturaIds", asignaturas);
		cuerpo.put("reorganizar", reorganizar);
		cuerpo.put("semilla", semilla);
		String respuesta = autenticado(post("/api/programaciones/" + programacion + "/propuesta")
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		return json.readTree(respuesta);
	}

	private long asignatura(String codigo, int sesiones) throws Exception {
		return crear("/api/asignaturas", Map.of("codigo", codigo, "nombre", "Asignatura " + codigo, "creditos", 3,
				"sesionesSemana", sesiones));
	}

	private long crear(String ruta, Map<String, Object> cuerpo) throws Exception {
		String respuesta = autenticado(post(ruta).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(cuerpo)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		return json.readTree(respuesta).get("id").asLong();
	}

	private ResultActions autenticado(MockHttpServletRequestBuilder peticion) throws Exception {
		return mvc.perform(peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}
}
