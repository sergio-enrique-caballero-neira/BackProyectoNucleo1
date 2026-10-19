package proyectonucleo.back.entity;

import java.time.LocalTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Bloque horario: franja individual de una asignatura dentro de una programación, con día,
 * hora de inicio y hora de fin (tabla HORARIO del modelo de datos).
 * <p>
 * La restricción única impide dos bloques que inicien a la misma hora el mismo día dentro de
 * una programación (RV-08); las claves foráneas impiden bloques huérfanos.
 */
@Entity
@Table(name = "horario",
		uniqueConstraints = @UniqueConstraint(name = "uk_horario_programacion_dia_inicio",
				columnNames = { "programacion_id", "dia_semana", "hora_inicio" }),
		indexes = { @Index(name = "idx_horario_asignatura", columnList = "asignatura_id"),
				@Index(name = "idx_horario_programacion", columnList = "programacion_id") })
public class Horario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "programacion_id", nullable = false,
			foreignKey = @ForeignKey(name = "fk_horario_programacion"))
	private Programacion programacion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "asignatura_id", nullable = false,
			foreignKey = @ForeignKey(name = "fk_horario_asignatura"))
	private Asignatura asignatura;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "dia_semana", nullable = false, length = 10)
	private DiaSemana diaSemana;

	@Column(name = "hora_inicio", nullable = false)
	private LocalTime horaInicio;

	@Column(name = "hora_fin", nullable = false)
	private LocalTime horaFin;

	public Horario() {
	}

	public Horario(Programacion programacion, Asignatura asignatura, DiaSemana diaSemana, LocalTime horaInicio,
			LocalTime horaFin) {
		this.programacion = programacion;
		this.asignatura = asignatura;
		this.diaSemana = diaSemana;
		this.horaInicio = horaInicio;
		this.horaFin = horaFin;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Programacion getProgramacion() {
		return programacion;
	}

	public void setProgramacion(Programacion programacion) {
		this.programacion = programacion;
	}

	public Asignatura getAsignatura() {
		return asignatura;
	}

	public void setAsignatura(Asignatura asignatura) {
		this.asignatura = asignatura;
	}

	public DiaSemana getDiaSemana() {
		return diaSemana;
	}

	public void setDiaSemana(DiaSemana diaSemana) {
		this.diaSemana = diaSemana;
	}

	public LocalTime getHoraInicio() {
		return horaInicio;
	}

	public void setHoraInicio(LocalTime horaInicio) {
		this.horaInicio = horaInicio;
	}

	public LocalTime getHoraFin() {
		return horaFin;
	}

	public void setHoraFin(LocalTime horaFin) {
		this.horaFin = horaFin;
	}
}
