package proyectonucleo.back.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import proyectonucleo.back.dto.LoginRequestDTO;
import proyectonucleo.back.dto.LoginResponseDTO;
import proyectonucleo.back.security.JwtUtil;

/**
 * Autenticación del administrador y emisión del JWT.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtUtil jwtUtil;

	public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
		this.authenticationManager = authenticationManager;
		this.jwtUtil = jwtUtil;
	}

	/**
	 * Valida las credenciales y emite un JWT. Credenciales inválidas o usuario inactivo
	 * responden 401.
	 *
	 * @param request credenciales
	 * @return token y datos del usuario
	 */
	@PostMapping("/login")
	@SecurityRequirements
	@Operation(summary = "Autenticar credenciales y emitir JWT")
	public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
		Authentication auth = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
		UserDetails user = (UserDetails) auth.getPrincipal();
		String rol = user.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");
		return new LoginResponseDTO(jwtUtil.generateToken(user), "Bearer", jwtUtil.getExpirationMs(),
				user.getUsername(), rol);
	}
}
