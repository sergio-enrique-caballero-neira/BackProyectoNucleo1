package proyectonucleo.back.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Reproduce la configuración de producción: solo el cliente web puede usar la API y la
 * documentación Swagger no está disponible.
 */
@SpringBootTest(properties = {
		"app.cors.allowed-origins=https://horarios.sergio.games,https://horarios.serbux.qzz.io",
		"app.security.require-origin=true",
		"springdoc.api-docs.enabled=false",
		"springdoc.swagger-ui.enabled=false" })
@AutoConfigureMockMvc
class OrigenPermitidoIntegrationTest {

	private static final String CLIENTE = "https://horarios.sergio.games";
	private static final String CLIENTE_ALTERNO = "https://horarios.serbux.qzz.io";
	private static final String LOGIN = "{\"username\":\"admin\",\"password\":\"admin-test-123\"}";

	@Autowired
	private MockMvc mvc;

	@Test
	void sinOrigenSeRechaza() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Origen no permitido."));
		mvc.perform(get("/api/asignaturas")).andExpect(status().isForbidden());
	}

	@Test
	void origenAjenoSeRechaza() throws Exception {
		for (String origen : new String[] { "https://otro-sitio.com", "http://localhost:4200" }) {
			mvc.perform(post("/api/auth/login").header(HttpHeaders.ORIGIN, origen)
					.contentType(MediaType.APPLICATION_JSON).content(LOGIN))
					.andExpect(status().isForbidden());
			mvc.perform(options("/api/asignaturas").header(HttpHeaders.ORIGIN, origen)
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
					.andExpect(status().isForbidden());
		}
	}

	@Test
	void elClienteWebUsaLaApiConNormalidad() throws Exception {
		for (String origen : new String[] { CLIENTE, CLIENTE_ALTERNO }) {
			mvc.perform(options("/api/asignaturas").header(HttpHeaders.ORIGIN, origen)
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
					.andExpect(status().isOk())
					.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origen));
			mvc.perform(post("/api/auth/login").header(HttpHeaders.ORIGIN, origen)
					.contentType(MediaType.APPLICATION_JSON).content(LOGIN))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.token").exists());
			mvc.perform(get("/api/asignaturas").header(HttpHeaders.ORIGIN, origen))
					.andExpect(status().isUnauthorized());
		}
	}

	@Test
	void swaggerNoEstaDisponible() throws Exception {
		mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
		mvc.perform(get("/swagger-ui/index.html").header(HttpHeaders.ORIGIN, CLIENTE))
				.andExpect(status().isNotFound());
	}
}
