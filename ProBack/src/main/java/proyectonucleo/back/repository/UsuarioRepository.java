package proyectonucleo.back.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import proyectonucleo.back.entity.Usuario;

/**
 * Acceso a la tabla USUARIO.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByUsername(String username);
}
