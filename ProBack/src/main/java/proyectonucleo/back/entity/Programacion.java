package proyectonucleo.back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Programación semanal de un programa académico: agrupa los bloques horarios de varias
 * asignaturas (tabla PROGRAMACION). En la interfaz se presenta como "Horario".
 * <p>
 * La descripción se guarda en la columna {@code carrera}, que antes identificaba la carrera
 * y ahora es un texto libre opcional (por ejemplo "Semestre 3, grupo A").
 */
@Entity
@Table(name = "programacion")
public class Programacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String codigo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "programa_id", nullable = false,
			foreignKey = @ForeignKey(name = "fk_programacion_programa"))
	private Programa programa;

	@Column(name = "carrera", length = 120)
	private String descripcion;

	public Programacion() {
	}

	public Programacion(String codigo, Programa programa, String descripcion) {
		this.codigo = codigo;
		this.programa = programa;
		this.descripcion = descripcion;
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

	public Programa getPrograma() {
		return programa;
	}

	public void setPrograma(Programa programa) {
		this.programa = programa;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
}
