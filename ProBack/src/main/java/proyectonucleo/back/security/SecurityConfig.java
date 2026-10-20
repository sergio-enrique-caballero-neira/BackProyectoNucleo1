package proyectonucleo.back.security;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import proyectonucleo.back.dto.ErrorResponseDTO;
import proyectonucleo.back.entity.Usuario;

/**
 * Reglas de acceso (RNF-04): sin sesión de servidor, todo {@code /api/**} exige un JWT válido
 * de un administrador, excepto el inicio de sesión. En desarrollo la documentación Swagger es
 * pública; en producción está desactivada y la API solo acepta peticiones del cliente web
 * ({@code app.security.require-origin=true}).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String[] RUTAS_PUBLICAS = { "/", "/error", "/swagger-ui.html", "/swagger-ui/**",
			"/v3/api-docs/**" };

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final ObjectMapper objectMapper;
	private final List<String> origenes;
	private final boolean exigirOrigen;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper,
			@Value("${app.cors.allowed-origins}") List<String> origenes,
			@Value("${app.security.require-origin:false}") boolean exigirOrigen) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.objectMapper = objectMapper;
		this.origenes = origenes.stream().map(String::trim).toList();
		this.exigirOrigen = exigirOrigen;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		if (exigirOrigen) {
			http.addFilterBefore(new OrigenPermitidoFilter(origenes, objectMapper),
					UsernamePasswordAuthenticationFilter.class);
		}
		http.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(RUTAS_PUBLICAS).permitAll()
						.requestMatchers("/api/**").hasRole(Usuario.ROL_ADMIN)
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, e) -> escribirError(request, response,
								HttpStatus.UNAUTHORIZED, "Debe iniciar sesión para acceder a este recurso."))
						.accessDeniedHandler((request, response, e) -> escribirError(request, response,
								HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación.")))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	/**
	 * Evita que Spring Boot registre el filtro JWT como filtro de servlet independiente; solo
	 * debe ejecutarse dentro de la cadena de Spring Security.
	 */
	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
		FilterRegistrationBean<JwtAuthenticationFilter> registro = new FilterRegistrationBean<>(
				jwtAuthenticationFilter);
		registro.setEnabled(false);
		return registro;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(origenes);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		config.setExposedHeaders(List.of("Location"));
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	private void escribirError(HttpServletRequest request, HttpServletResponse response, HttpStatus estado,
			String mensaje) throws IOException {
		response.setStatus(estado.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(), new ErrorResponseDTO(OffsetDateTime.now(),
				estado.value(), estado.getReasonPhrase(), mensaje, request.getRequestURI(), null));
	}
}
