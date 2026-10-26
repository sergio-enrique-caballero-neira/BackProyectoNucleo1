package proyectonucleo.back.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de un programa académico. {@code asignaturas} y {@code horarios} son conteos que solo
 * se devuelven en las respuestas.
 */
public class ProgramaDTO {

	@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1")
	private Long id;

	@NotBlank(message = "El código es obligatorio.")
	@Size(max = 20, message = "El código admite máximo 20 caracteres.")
	@Schema(example = "SIS")
	private String codigo;

	@NotBlank(message = "El nombre es obligatorio.")
	@Size(max = 120, message = "El nombre admite máximo 120 caracteres.")
	@Schema(example = "Ingeniería de Sistemas")
	private String nombre;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "12")
	private long asignaturas;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "2")
	private long horarios;

	public ProgramaDTO() {
	}

	public ProgramaDTO(String codigo, String nombre) {
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

	public long getAsignaturas() {
		return asignaturas;
	}

	public void setAsignaturas(long asignaturas) {
		this.asignaturas = asignaturas;
	}

	public long getHorarios() {
		return horarios;
	}

	public void setHorarios(long horarios) {
		this.horarios = horarios;
	}
}
