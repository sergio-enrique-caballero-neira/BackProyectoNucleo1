package proyectonucleo.back.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de una programación semanal ("Horario" en la interfaz). {@code bloques} solo se
 * devuelve en las respuestas; los bloques se crean y modifican en {@code /api/horarios}.
 */
public class ProgramacionDTO {

	@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1")
	private Long id;

	@NotBlank(message = "El código es obligatorio.")
	@Size(max = 30, message = "El código admite máximo 30 caracteres.")
	@Schema(example = "HIS-2026-1")
	private String codigo;

	@NotBlank(message = "La carrera es obligatoria.")
	@Size(max = 120, message = "La carrera admite máximo 120 caracteres.")
	@Schema(example = "Ingeniería de Sistemas")
	private String carrera;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	private List<HorarioDTO> bloques = new ArrayList<>();

	public ProgramacionDTO() {
	}

	public ProgramacionDTO(String codigo, String carrera) {
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

	public List<HorarioDTO> getBloques() {
		return bloques;
	}

	public void setBloques(List<HorarioDTO> bloques) {
		this.bloques = bloques;
	}
}
