package proyectonucleo.back.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * Unidad académica identificada por código y caracterizada por nombre, créditos y
 * frecuencia semanal (tabla ASIGNATURA del modelo de datos). Puede ofrecerse en varios
 * programas a la vez (tabla ASIGNATURA_PROGRAMA).
 */
@Entity
@Table(name = "asignatura")
public class Asignatura {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String codigo;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false, columnDefinition = "TINYINT")
	private Integer creditos;

	@Column(name = "sesiones_semana", nullable = false, columnDefinition = "TINYINT")
	private Integer sesionesSemana;

	@ManyToMany
	@JoinTable(name = "asignatura_programa",
			joinColumns = @JoinColumn(name = "asignatura_id",
					foreignKey = @ForeignKey(name = "fk_asignatura_programa_asignatura")),
			inverseJoinColumns = @JoinColumn(name = "programa_id",
					foreignKey = @ForeignKey(name = "fk_asignatura_programa_programa")))
	private Set<Programa> programas = new HashSet<>();

	public Asignatura() {
	}

	public Asignatura(String codigo, String nombre, Integer creditos, Integer sesionesSemana) {
		this.codigo = codigo;
		this.nombre = nombre;
		this.creditos = creditos;
		this.sesionesSemana = sesionesSemana;
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

	public Integer getCreditos() {
		return creditos;
	}

	public void setCreditos(Integer creditos) {
		this.creditos = creditos;
	}

	public Integer getSesionesSemana() {
		return sesionesSemana;
	}

	public void setSesionesSemana(Integer sesionesSemana) {
		this.sesionesSemana = sesionesSemana;
	}

	public Set<Programa> getProgramas() {
		return programas;
	}

	public void setProgramas(Set<Programa> programas) {
		this.programas = programas;
	}
}
