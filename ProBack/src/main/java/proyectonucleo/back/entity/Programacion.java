package proyectonucleo.back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Programación semanal de una carrera: agrupa los bloques horarios de varias asignaturas
 * (tabla PROGRAMACION). En la interfaz se presenta como "Horario".
 */
@Entity
@Table(name = "programacion")
public class Programacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String codigo;

	@Column(nullable = false, length = 120)
	private String carrera;

	public Programacion() {
	}

	public Programacion(String codigo, String carrera) {
		this.codigo = codigo;
		this.carrera = carrera;
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

	public String getCarrera() {
		return carrera;
	}

	public void setCarrera(String carrera) {
		this.carrera = carrera;
	}
}
