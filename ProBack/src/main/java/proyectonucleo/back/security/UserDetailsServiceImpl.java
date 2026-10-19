package proyectonucleo.back.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import proyectonucleo.back.entity.Usuario;
import proyectonucleo.back.repository.UsuarioRepository;

/**
 * Carga los usuarios desde la tabla USUARIO para Spring Security.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		Usuario usuario = usuarioRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
		return User.withUsername(usuario.getUsername())
				.password(usuario.getPasswordHash())
				.roles(usuario.getRol())
				.disabled(!Boolean.TRUE.equals(usuario.getActivo()))
				.build();
	}
}
