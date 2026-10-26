package proyectonucleo.back.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

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
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Recorre el contrato de la API contra una base H2 en memoria. Cada prueba se revierte al
 * terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	private String token;

	private long programa;

	@BeforeEach
	void iniciarSesion() throws Exception {
		String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin-test-123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("Bearer"))
				.andExpect(jsonPath("$.rol").value("ADMIN"))
				.andReturn().getResponse().getContentAsString();
		token = json.readTree(respuesta).get("token").asText();
		programa = crear("/api/programas", programaJson("SIS", "Ingeniería de Sistemas"));
	}

	// --- Autenticación ---------------------------------------------------------------

	@Test
	void loginConCredencialesInvalidasResponde401() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos."));
	}

	@Test
	void loginSinDatosResponde400() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.username").exists())
				.andExpect(jsonPath("$.campos.password").exists());
	}

	@Test
	void sinTokenOTokenInvalidoResponde401() throws Exception {
		mvc.perform(get("/api/asignaturas")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.estado").value(401));
		mvc.perform(get("/api/programaciones")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/horarios").header(HttpHeaders.AUTHORIZATION, "Bearer token.falso.xyz"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void preflightCorsDesdeElFrontEstaPermitido() throws Exception {
		for (String origen : new String[] { "http://localhost:4200", "https://horarios.sergio.games" }) {
			mvc.perform(options("/api/asignaturas").header(HttpHeaders.ORIGIN, origen)
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
					.andExpect(status().isOk())
					.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origen));
		}
	}

	// --- Programas -------------------------------------------------------------------

	@Test
	void crudDeProgramasConSusConteos() throws Exception {
		long bio = crear("/api/programas", programaJson(" bio ", "Bioingeniería"));
		long calculo = crear("/api/asignaturas", asignatura("MAT101", "Cálculo Diferencial", 4, 2, programa, bio));
		crearProgramacion("BIO-2026-1", "Semestre 1");

		autenticado(get("/api/programas")).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].codigo").value("BIO"))
				.andExpect(jsonPath("$[0].asignaturas").value(1))
				.andExpect(jsonPath("$[0].horarios").value(0))
				.andExpect(jsonPath("$[1].horarios").value(1));

		autenticado(post("/api/programas").contentType(MediaType.APPLICATION_JSON)
				.content(programaJson("BIO2", "bioingeniería")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.campos.nombre").exists());
		autenticado(put("/api/programas/" + bio).contentType(MediaType.APPLICATION_JSON)
				.content(programaJson("sis", "Otro")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.campos.codigo").exists());
		autenticado(post("/api/programas").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.codigo").exists())
				.andExpect(jsonPath("$.campos.nombre").exists());

		autenticado(delete("/api/programas/" + programa)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje", containsString("1 horario")));

		// La asignatura compartida sobrevive y solo deja de ofrecerse en Bioingeniería.
		autenticado(delete("/api/programas/" + bio)).andExpect(status().isNoContent());
		autenticado(get("/api/asignaturas/" + calculo)).andExpect(status().isOk())
				.andExpect(jsonPath("$.programaIds", hasSize(1)))
				.andExpect(jsonPath("$.programaIds[0]").value(programa));
	}

	@Test
	void asignaturaConProgramasInexistentesResponde404() throws Exception {
		autenticado(post("/api/asignaturas").contentType(MediaType.APPLICATION_JSON)
				.content(asignatura("MAT101", "Cálculo", 4, 2, programa, 999L)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("El programa con id 999 no existe."));
	}

	// --- Asignaturas -----------------------------------------------------------------

	@Test
	void crudCompletoDeAsignaturas() throws Exception {
		long id = crearAsignatura(" sis101 ", "Programación I", 4, 3);

		autenticado(get("/api/asignaturas")).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].codigo").value("SIS101"));

		autenticado(put("/api/asignaturas/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(asignatura("SIS101", "Programación 1", 3, 2)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Programación 1"))
				.andExpect(jsonPath("$.id").value(id));

		autenticado(get("/api/asignaturas/" + id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.creditos").value(3));

		autenticado(delete("/api/asignaturas/" + id)).andExpect(status().isNoContent());
		autenticado(get("/api/asignaturas/" + id)).andExpect(status().isNotFound());
	}

	@Test
	void asignaturaInvalidaResponde400ConCampos() throws Exception {
		autenticado(post("/api/asignaturas").contentType(MediaType.APPLICATION_JSON)
				.content(asignatura("", "", 0, 5)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.codigo").exists())
				.andExpect(jsonPath("$.campos.nombre").exists())
				.andExpect(jsonPath("$.campos.creditos").exists())
				.andExpect(jsonPath("$.campos.sesionesSemana").value("Las sesiones por semana deben estar entre 1 y 4."));
	}

	@Test
	void codigoDuplicadoResponde409() throws Exception {
		crearAsignatura("SIS101", "Programación I", 4, 3);
		long otra = crearAsignatura("SIS201", "Bases de Datos", 4, 3);

		autenticado(post("/api/asignaturas").contentType(MediaType.APPLICATION_JSON)
				.content(asignatura("sis101", "Otra", 3, 2)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.campos.codigo").exists());

		autenticado(put("/api/asignaturas/" + otra).contentType(MediaType.APPLICATION_JSON)
				.content(asignatura("SIS101", "Bases de Datos", 4, 3)))
				.andExpect(status().isConflict());
	}

	@Test
	void asignaturaInexistenteResponde404() throws Exception {
		autenticado(put("/api/asignaturas/999").contentType(MediaType.APPLICATION_JSON)
				.content(asignatura("X1", "X", 3, 2)))
				.andExpect(status().isNotFound());
		autenticado(delete("/api/asignaturas/999")).andExpect(status().isNotFound());
	}

	@Test
	void noEliminaAsignaturaConHorariosNiReduceSesionesBajoLosProgramados() throws Exception {
		long programacion = crearProgramacion("HIS-2026-1", "Ingeniería de Sistemas");
		long otraProgramacion = crearProgramacion("HIS-2026-2", "Ingeniería de Sistemas");
		long id = crearAsignatura("SIS101", "Programación I", 4, 3);
		crearHorario(programacion, id, "Lunes", "07:00", "09:00");
		crearHorario(programacion, id, "Martes", "07:00", "09:00");
		crearHorario(otraProgramacion, id, "Lunes", "07:00", "09:00");

		autenticado(delete("/api/asignaturas/" + id)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje", containsString("3 bloques horarios asociados")));

		autenticado(put("/api/asignaturas/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(asignatura("SIS101", "Programación I", 4, 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.campos.sesionesSemana").exists());

		autenticado(put("/api/asignaturas/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(asignatura("SIS101", "Programación I", 4, 2)))
				.andExpect(status().isOk());
	}

	// --- Programaciones --------------------------------------------------------------

	@Test
	void crudDeProgramacionesConSusBloques() throws Exception {
		long id = crearProgramacion(" his-2026-1 ", "Ingeniería de Sistemas");
		long asignatura = crearAsignatura("SIS101", "Programación I", 4, 3);
		crearHorario(id, asignatura, "Martes", "14:00", "16:00");
		crearHorario(id, asignatura, "Lunes", "07:00", "09:00");

		autenticado(get("/api/programaciones")).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].codigo").value("HIS-2026-1"))
				.andExpect(jsonPath("$[0].bloques", hasSize(2)))
				.andExpect(jsonPath("$[0].bloques[0].diaSemana").value("Lunes"));

		autenticado(put("/api/programaciones/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(programacion("HIS-2026-1", "Semestre 3")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.descripcion").value("Semestre 3"))
				.andExpect(jsonPath("$.programaId").value(programa))
				.andExpect(jsonPath("$.programaNombre").value("Ingeniería de Sistemas"))
				.andExpect(jsonPath("$.bloques", hasSize(2)));

		autenticado(post("/api/programaciones").contentType(MediaType.APPLICATION_JSON)
				.content(programacion("his-2026-1", "Otra")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.campos.codigo").exists());

		autenticado(post("/api/programaciones").contentType(MediaType.APPLICATION_JSON)
				.content("{\"codigo\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.codigo").exists())
				.andExpect(jsonPath("$.campos.programaId").exists());

		long sinDescripcion = crearProgramacion("HIS-2026-2", "  ");
		autenticado(get("/api/programaciones/" + sinDescripcion)).andExpect(jsonPath("$.descripcion").doesNotExist());

		autenticado(delete("/api/programaciones/" + id)).andExpect(status().isNoContent());
		autenticado(get("/api/programaciones/" + id)).andExpect(status().isNotFound());
		autenticado(get("/api/horarios")).andExpect(jsonPath("$", hasSize(0)));
		autenticado(delete("/api/asignaturas/" + asignatura)).andExpect(status().isNoContent());
	}

	// --- Horarios --------------------------------------------------------------------

	@Test
	void crudCompletoDeHorarios() throws Exception {
		long his = crearProgramacion("HIS-2026-1", "Ingeniería de Sistemas");
		long hii = crearProgramacion("HII-2026-1", "Ingeniería Industrial");
		long programacion = crearAsignatura("SIS101", "Programación I", 4, 3);
		long bases = crearAsignatura("SIS201", "Bases de Datos", 4, 3);

		long martes = crearHorario(his, programacion, "martes", "14:00", "16:00");
		crearHorario(his, programacion, "Lunes", "07:00", "09:00");
		crearHorario(hii, bases, "Miércoles", "18:00", "20:00");

		autenticado(get("/api/horarios")).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].diaSemana").value("Lunes"))
				.andExpect(jsonPath("$[0].horaInicio").value("07:00"))
				.andExpect(jsonPath("$[0].programacionId").value(his))
				.andExpect(jsonPath("$[2].diaSemana").value("Miércoles"))
				.andExpect(jsonPath("$[2].asignaturaId").value(bases))
				.andExpect(jsonPath("$[2].asignaturaCodigo").value("SIS201"));

		autenticado(get("/api/horarios").param("asignaturaId", String.valueOf(bases)))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].asignaturaNombre").value("Bases de Datos"));
		autenticado(get("/api/horarios").param("programacionId", String.valueOf(his)))
				.andExpect(jsonPath("$", hasSize(2)));

		autenticado(put("/api/horarios/" + martes).contentType(MediaType.APPLICATION_JSON)
				.content(horario(his, programacion, "Sábado", "09:00", "11:00")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diaSemana").value("Sábado"))
				.andExpect(jsonPath("$.horaFin").value("11:00"));

		autenticado(delete("/api/horarios/" + martes)).andExpect(status().isNoContent());
		autenticado(get("/api/horarios/" + martes)).andExpect(status().isNotFound());
	}

	@Test
	void horarioQueIncumpleReglasResponde400() throws Exception {
		long his = crearProgramacion("HIS-2026-1", "Ingeniería de Sistemas");
		long id = crearAsignatura("SIS101", "Programación I", 4, 3);

		autenticado(post("/api/horarios").contentType(MediaType.APPLICATION_JSON)
				.content(horario(his, id, "Lunes", "11:00", "13:00")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje", containsString("descanso de 12:00 a 13:00")))
				.andExpect(jsonPath("$.campos.horaInicio").exists());

		autenticado(post("/api/horarios").contentType(MediaType.APPLICATION_JSON)
				.content(horario(his, id, "Lunes", "20:00", "22:00")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La jornada de lunes a viernes termina a las 21:00."));

		autenticado(post("/api/horarios").contentType(MediaType.APPLICATION_JSON)
				.content(horario(his, id, "Domingo", "07:00", "09:00")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.diaSemana").value("El día debe estar entre lunes y sábado."));

		autenticado(post("/api/horarios").contentType(MediaType.APPLICATION_JSON)
				.content(horario(his, id, "Lunes", "7am", "09:00")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.horaInicio").exists());

		autenticado(post("/api/horarios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"diaSemana\":\"Lunes\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.programacionId").exists())
				.andExpect(jsonPath("$.campos.asignaturaId").exists())
				.andExpect(jsonPath("$.campos.horaInicio").exists())
				.andExpect(jsonPath("$.campos.horaFin").exists());
	}

	@Test
	void horarioConReferenciasInexistentesResponde404() throws Exception {
		long his = crearProgramacion("HIS-2026-1", "Ingeniería de Sistemas");
		long id = crearAsignatura("SIS101", "Programación I", 4, 3);
		autenticado(post("/api/horarios").contentType(MediaType.APPLICATION_JSON)
				.content(horario(his, 999, "Lunes", "07:00", "09:00")))
				.andExpect(status().isNotFound());
		autenticado(post("/api/horarios").contentType(MediaType.APPLICATION_JSON)
				.content(horario(999, id, "Lunes", "07:00", "09:00")))
				.andExpect(status().isNotFound());
	}

	@Test
	void noSeCruzanBloquesDentroDeUnaMismaProgramacion() throws Exception {
		long his = crearProgramacion("HIS-2026-1", "Ingeniería de Sistemas");
		long hii = crearProgramacion("HII-2026-1", "Ingeniería Industrial");
		long programacion = crearAsignatura("SIS101", "Programación I", 4, 3);
		long bases = crearAsignatura("SIS201", "Bases de Datos", 4, 3);
		crearHorario(his, programacion, "Lunes", "07:00", "09:00");

		autenticado(post("/api/horarios").contentType(MediaType.APPLICATION_JSON)
				.content(horario(his, programacion, "Lunes", "07:00", "09:00")))
				.andExpect(status().isConflict());
		autenticado(post("/api/horarios").contentType(MediaType.APPLICATION_JSON)
				.content(horario(his, bases, "Lunes", "08:00", "10:00")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("El Lunes de 07:00 a 09:00 ya está ocupado por Programación I."));

		// En otra programación la misma franja está libre.
		crearHorario(hii, bases, "Lunes", "07:00", "09:00");
		// Y en la misma programación, una franja contigua también.
		crearHorario(his, bases, "Lunes", "09:00", "11:00");
	}

	@Test
	void sesionesSemanalesSeCuentanPorProgramacion() throws Exception {
		long his = crearProgramacion("HIS-2026-1", "Ingeniería de Sistemas");
		long hii = crearProgramacion("HII-2026-1", "Ingeniería Industrial");
		long id = crearAsignatura("SIS301", "Ingeniería de Software", 3, 2);
		crearHorario(his, id, "Lunes", "07:00", "09:00");
		long martes = crearHorario(his, id, "Martes", "07:00", "09:00");

		autenticado(post("/api/horarios").contentType(MediaType.APPLICATION_JSON)
				.content(horario(his, id, "Jueves", "07:00", "09:00")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("Ingeniería de Software ya alcanzó sus 2 sesiones semanales."));

		// Mover un bloque existente no cuenta como sesión nueva.
		autenticado(put("/api/horarios/" + martes).contentType(MediaType.APPLICATION_JSON)
				.content(horario(his, id, "Jueves", "07:00", "09:00")))
				.andExpect(status().isOk());

		// En otra programación la asignatura tiene su propio cupo.
		crearHorario(hii, id, "Lunes", "07:00", "09:00");
	}

	// --- Utilidades ------------------------------------------------------------------

	private ResultActions autenticado(MockHttpServletRequestBuilder peticion) throws Exception {
		return mvc.perform(peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private long crearAsignatura(String codigo, String nombre, int creditos, int sesiones) throws Exception {
		return crear("/api/asignaturas", asignatura(codigo, nombre, creditos, sesiones));
	}

	private long crearProgramacion(String codigo, String descripcion) throws Exception {
		return crear("/api/programaciones", programacion(codigo, descripcion));
	}

	private long crearHorario(long programacionId, long asignaturaId, String dia, String inicio, String fin)
			throws Exception {
		return crear("/api/horarios", horario(programacionId, asignaturaId, dia, inicio, fin));
	}

	private long crear(String ruta, String cuerpo) throws Exception {
		String respuesta = autenticado(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isCreated())
				.andExpect(header().exists(HttpHeaders.LOCATION))
				.andReturn().getResponse().getContentAsString();
		return json.readTree(respuesta).get("id").asLong();
	}

	private String asignatura(String codigo, String nombre, int creditos, int sesiones, Long... programas)
			throws Exception {
		return json.writeValueAsString(Map.of("codigo", codigo, "nombre", nombre, "creditos", creditos,
				"sesionesSemana", sesiones, "programaIds", List.of(programas)));
	}

	private String programacion(String codigo, String descripcion) throws Exception {
		return json.writeValueAsString(Map.of("codigo", codigo, "programaId", programa, "descripcion", descripcion));
	}

	private String programaJson(String codigo, String nombre) throws Exception {
		return json.writeValueAsString(Map.of("codigo", codigo, "nombre", nombre));
	}

	private String horario(long programacionId, long asignaturaId, String dia, String inicio, String fin)
			throws Exception {
		return json.writeValueAsString(Map.of("programacionId", programacionId, "asignaturaId", asignaturaId,
				"diaSemana", dia, "horaInicio", inicio, "horaFin", fin));
	}
}
