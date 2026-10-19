package proyectonucleo.back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Credenciales y rol de acceso (tabla USUARIO). La contraseña se guarda como hash BCrypt,
 * nunca en texto plano.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

	/** Único rol de esta versión: el Director del Programa o quien delegue. */
	public static final String ROL_ADMIN = "ADMIN";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false, length = 30)
	private String rol;

	@Column(nullable = false)
	private Boolean activo;

	public Usuario() {
	}

	public Usuario(String username, String passwordHash, String rol, Boolean activo) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.rol = rol;
		this.activo = activo;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getRol() {
		return rol;
	}

	public void setRol(String rol) {
		this.rol = rol;
	}

	public Boolean getActivo() {
		return activo;
	}

	public void setActivo(Boolean activo) {
		this.activo = activo;
	}
}
