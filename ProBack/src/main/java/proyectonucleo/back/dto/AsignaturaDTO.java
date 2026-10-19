package proyectonucleo.back.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de una asignatura que viajan entre la API y el cliente (RV-01, RV-07).
 */
public class AsignaturaDTO {

	@Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1")
	private Long id;

	@NotBlank(message = "El código es obligatorio.")
	@Size(max = 20, message = "El código admite máximo 20 caracteres.")
	@Schema(example = "SIS101")
	private String codigo;

	@NotBlank(message = "El nombre es obligatorio.")
	@Size(max = 120, message = "El nombre admite máximo 120 caracteres.")
	@Schema(example = "Programación I")
	private String nombre;

	@NotNull(message = "Los créditos son obligatorios.")
	@Min(value = 1, message = "Los créditos deben estar entre 1 y 6.")
	@Max(value = 6, message = "Los créditos deben estar entre 1 y 6.")
	@Schema(example = "4")
	private Integer creditos;

	@NotNull(message = "Las sesiones por semana son obligatorias.")
	@Min(value = 1, message = "Las sesiones por semana deben estar entre 1 y 4.")
	@Max(value = 4, message = "Las sesiones por semana deben estar entre 1 y 4.")
	@Schema(example = "3")
	private Integer sesionesSemana;

	public AsignaturaDTO() {
	}

	public AsignaturaDTO(String codigo, String nombre, Integer creditos, Integer sesionesSemana) {
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
}
