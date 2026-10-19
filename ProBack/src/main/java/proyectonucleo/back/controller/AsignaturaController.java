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
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import proyectonucleo.back.dto.AsignaturaDTO;
import proyectonucleo.back.service.AsignaturaService;

/**
 * Endpoints del catálogo de asignaturas (RF-01 a RF-04).
 */
@RestController
@RequestMapping("/api/asignaturas")
@Tag(name = "Asignaturas")
public class AsignaturaController {

	private final AsignaturaService asignaturaService;

	public AsignaturaController(AsignaturaService asignaturaService) {
		this.asignaturaService = asignaturaService;
	}

	@GetMapping
	@Operation(summary = "Consultar el catálogo de asignaturas")
	public List<AsignaturaDTO> listar() {
		return asignaturaService.getAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar una asignatura")
	public AsignaturaDTO obtener(@PathVariable Long id) {
		return asignaturaService.getById(id);
	}

	@PostMapping
	@Operation(summary = "Crear una asignatura")
	public ResponseEntity<AsignaturaDTO> crear(@Valid @RequestBody AsignaturaDTO data) {
		AsignaturaDTO creada = asignaturaService.create(data);
		return ResponseEntity.created(URI.create("/api/asignaturas/" + creada.getId())).body(creada);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar los datos de una asignatura")
	public AsignaturaDTO actualizar(@PathVariable Long id, @Valid @RequestBody AsignaturaDTO data) {
		return asignaturaService.updateById(id, data);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar una asignatura sin bloques horarios asociados")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		asignaturaService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
