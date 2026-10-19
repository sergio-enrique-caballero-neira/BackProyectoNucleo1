package proyectonucleo.back.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import proyectonucleo.back.dto.HorarioDTO;
import proyectonucleo.back.service.HorarioService;

/**
 * Endpoints de bloques horarios (RF-05 a RF-08).
 */
@RestController
@RequestMapping("/api/horarios")
@Tag(name = "Horarios")
public class HorarioController {

	private final HorarioService horarioService;

	public HorarioController(HorarioService horarioService) {
		this.horarioService = horarioService;
	}

	@GetMapping
	@Operation(summary = "Consultar horarios ordenados por día y hora; filtros opcionales por programación y asignatura")
	public List<HorarioDTO> listar(
			@Parameter(description = "Id de la programación") @RequestParam(required = false) Long programacionId,
			@Parameter(description = "Id de la asignatura") @RequestParam(required = false) Long asignaturaId) {
		return horarioService.buscar(programacionId, asignaturaId);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar un bloque horario")
	public HorarioDTO obtener(@PathVariable Long id) {
		return horarioService.getById(id);
	}

	@PostMapping
	@Operation(summary = "Crear un bloque horario de una asignatura dentro de una programación")
	public ResponseEntity<HorarioDTO> crear(@Valid @RequestBody HorarioDTO data) {
		HorarioDTO creado = horarioService.create(data);
		return ResponseEntity.created(URI.create("/api/horarios/" + creado.getId())).body(creado);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar día y franja del bloque")
	public HorarioDTO actualizar(@PathVariable Long id, @Valid @RequestBody HorarioDTO data) {
		return horarioService.updateById(id, data);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar un bloque horario")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		horarioService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
