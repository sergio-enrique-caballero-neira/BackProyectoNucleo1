package proyectonucleo.back.security;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import proyectonucleo.back.dto.ErrorResponseDTO;

/**
 * Rechaza con 403 las peticiones a {@code /api/**} cuyo encabezado {@code Origin} no es el
 * del cliente web autorizado. Complementa a CORS, que solo lo aplican los navegadores: sin
 * este filtro, herramientas como curl o Postman podrían usar la API sin enviar
 * {@code Origin}. Se activa con {@code app.security.require-origin=true} (perfil prod).
 * <p>
 * No sustituye la autenticación: el encabezado puede falsificarse fuera de un navegador, por
 * lo que la protección de los datos sigue siendo el JWT.
 */
public class OrigenPermitidoFilter extends OncePerRequestFilter {

	private final Set<String> origenes;
	private final ObjectMapper objectMapper;

	public OrigenPermitidoFilter(Collection<String> origenes, ObjectMapper objectMapper) {
		this.origenes = origenes.stream().map(String::trim).collect(Collectors.toUnmodifiableSet());
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String ruta = request.getRequestURI().substring(request.getContextPath().length());
		return !ruta.startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String origen = request.getHeader(HttpHeaders.ORIGIN);
		if (origen != null && origenes.contains(origen)) {
			chain.doFilter(request, response);
			return;
		}
		HttpStatus estado = HttpStatus.FORBIDDEN;
		response.setStatus(estado.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(), new ErrorResponseDTO(OffsetDateTime.now(),
				estado.value(), estado.getReasonPhrase(), "Origen no permitido.", request.getRequestURI(), null));
	}
}
