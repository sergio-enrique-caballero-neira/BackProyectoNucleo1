package proyectonucleo.back.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import proyectonucleo.back.entity.DiaSemana;

/**
 * Datos de un bloque horario. {@code asignaturaCodigo} y {@code asignaturaNombre} solo se
 * devuelven en las respuestas; en las peticiones bastan {@code programacionId} y
 * {@code asignaturaId}.
 */
public class HorarioDTO {

	@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1")
	private Long id;

	@NotNull(message = "La programación es obligatoria.")
	@Schema(example = "1")
	private Long programacionId;

	@NotNull(message = "La asignatura es obligatoria.")
	@Schema(example = "1")
	private Long asignaturaId;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "SIS101")
	private String asignaturaCodigo;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "Programación I")
	private String asignaturaNombre;

	@NotNull(message = "El día es obligatorio.")
	@Schema(type = "string", example = "Lunes",
			allowableValues = { "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado" })
	private DiaSemana diaSemana;

	@NotNull(message = "La hora de inicio es obligatoria.")
	@JsonFormat(pattern = "HH:mm")
	@Schema(type = "string", pattern = "HH:mm", example = "07:00")
	private LocalTime horaInicio;

	@NotNull(message = "La hora de fin es obligatoria.")
	@JsonFormat(pattern = "HH:mm")
	@Schema(type = "string", pattern = "HH:mm", example = "09:00")
	private LocalTime horaFin;

	public HorarioDTO() {
	}

	public HorarioDTO(Long programacionId, Long asignaturaId, DiaSemana diaSemana, LocalTime horaInicio,
			LocalTime horaFin) {
		this.programacionId = programacionId;
		this.asignaturaId = asignaturaId;
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

	public Long getProgramacionId() {
		return programacionId;
	}

	public void setProgramacionId(Long programacionId) {
		this.programacionId = programacionId;
	}

	public Long getAsignaturaId() {
		return asignaturaId;
	}

	public void setAsignaturaId(Long asignaturaId) {
		this.asignaturaId = asignaturaId;
	}

	public String getAsignaturaCodigo() {
		return asignaturaCodigo;
	}

	public void setAsignaturaCodigo(String asignaturaCodigo) {
		this.asignaturaCodigo = asignaturaCodigo;
	}

	public String getAsignaturaNombre() {
		return asignaturaNombre;
	}

	public void setAsignaturaNombre(String asignaturaNombre) {
		this.asignaturaNombre = asignaturaNombre;
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
