package proyectonucleo.back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Programa académico (por ejemplo Ingeniería de Sistemas o Bioingeniería). Agrupa los
 * horarios de la carrera y define qué asignaturas se le ofrecen (tabla PROGRAMA).
 */
@Entity
@Table(name = "programa")
public class Programa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String codigo;

	@Column(nullable = false, unique = true, length = 120)
	private String nombre;

	public Programa() {
	}

	public Programa(String codigo, String nombre) {
		this.codigo = codigo;
		this.nombre = nombre;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
}
