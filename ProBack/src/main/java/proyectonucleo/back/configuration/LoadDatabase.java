package proyectonucleo.back.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import proyectonucleo.back.entity.Usuario;
import proyectonucleo.back.repository.UsuarioRepository;

/**
 * Carga inicial: crea el usuario administrador cuando la tabla USUARIO está vacía. Las
 * credenciales vienen de {@code app.admin.username} y {@code app.admin.password}.
 */
@Configuration
public class LoadDatabase {

	private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

	@Bean
	CommandLineRunner crearAdministrador(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
			@Value("${app.admin.username}") String username, @Value("${app.admin.password:}") String password) {
		return args -> {
			if (usuarioRepository.count() > 0) {
				return;
			}
			if (password == null || password.isBlank()) {
				log.warn("No hay usuarios y app.admin.password está vacío: no se creó el administrador.");
				return;
			}
			usuarioRepository.save(new Usuario(username, passwordEncoder.encode(password), Usuario.ROL_ADMIN, true));
			log.info("Usuario administrador '{}' creado.", username);
		};
	}
}
