package proyectonucleo.back.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de una programación semanal ("Horario" en la interfaz). {@code programaCodigo},
 * {@code programaNombre} y {@code bloques} solo se devuelven en las respuestas; los bloques se
 * crean y modifican en {@code /api/horarios} o con la generación automática.
 */
public class ProgramacionDTO {

	@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1")
	private Long id;

	@NotBlank(message = "El código es obligatorio.")
	@Size(max = 30, message = "El código admite máximo 30 caracteres.")
	@Schema(example = "SIS-2026-2-S3")
	private String codigo;

	@NotNull(message = "El programa es obligatorio.")
	@Schema(example = "1")
	private Long programaId;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "SIS")
	private String programaCodigo;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "Ingeniería de Sistemas")
	private String programaNombre;

	@Size(max = 120, message = "La descripción admite máximo 120 caracteres.")
	@Schema(example = "Semestre 3, grupo A")
	private String descripcion;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	private List<HorarioDTO> bloques = new ArrayList<>();

	public ProgramacionDTO() {
	}

	public ProgramacionDTO(String codigo, Long programaId, String descripcion) {
		this.codigo = codigo;
		this.programaId = programaId;
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

	public Long getProgramaId() {
		return programaId;
	}

	public void setProgramaId(Long programaId) {
		this.programaId = programaId;
	}

	public String getProgramaCodigo() {
		return programaCodigo;
	}

	public void setProgramaCodigo(String programaCodigo) {
		this.programaCodigo = programaCodigo;
	}

	public String getProgramaNombre() {
		return programaNombre;
	}

	public void setProgramaNombre(String programaNombre) {
		this.programaNombre = programaNombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public List<HorarioDTO> getBloques() {
		return bloques;
	}

	public void setBloques(List<HorarioDTO> bloques) {
		this.bloques = bloques;
	}
}
