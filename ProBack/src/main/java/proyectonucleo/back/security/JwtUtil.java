package proyectonucleo.back.security;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Genera y valida los tokens JWT. El secreto y la duración se leen de la configuración
 * ({@code jwt.secret}, {@code jwt.expiration-ms}), nunca del código fuente.
 */
@Component
public class JwtUtil {

	private final Key key;
	private final long expirationMs;

	public JwtUtil(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {
		this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
		this.expirationMs = expirationMs;
	}

	/**
	 * Emite un token para el usuario autenticado, con su rol como claim.
	 *
	 * @param user usuario autenticado
	 * @return token firmado con HS256
	 */
	public String generateToken(UserDetails user) {
		Date ahora = new Date();
		String rol = user.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.findFirst()
				.orElse("");
		return Jwts.builder()
				.setSubject(user.getUsername())
				.claim("rol", rol)
				.setIssuedAt(ahora)
				.setExpiration(new Date(ahora.getTime() + expirationMs))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	/**
	 * Obtiene el usuario de un token con firma válida y sin expirar.
	 *
	 * @param token token recibido
	 * @return nombre de usuario
	 * @throws JwtException si el token es inválido o expiró
	 */
	public String extractUsername(String token) {
		return parse(token).getSubject();
	}

	public long getExpirationMs() {
		return expirationMs;
	}

	private Claims parse(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
	}
}
