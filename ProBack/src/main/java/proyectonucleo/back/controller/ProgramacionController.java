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
import proyectonucleo.back.dto.ProgramacionDTO;
import proyectonucleo.back.service.ProgramacionService;

/**
 * Endpoints de las programaciones semanales ("Horarios" en la interfaz), que agrupan los
 * bloques horarios de varias asignaturas.
 */
@RestController
@RequestMapping("/api/programaciones")
@Tag(name = "Programaciones")
public class ProgramacionController {

	private final ProgramacionService programacionService;

	public ProgramacionController(ProgramacionService programacionService) {
		this.programacionService = programacionService;
	}

	@GetMapping
	@Operation(summary = "Consultar las programaciones con sus bloques")
	public List<ProgramacionDTO> listar() {
		return programacionService.getAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar una programación con sus bloques")
	public ProgramacionDTO obtener(@PathVariable Long id) {
		return programacionService.getById(id);
	}

	@PostMapping
	@Operation(summary = "Crear una programación")
	public ResponseEntity<ProgramacionDTO> crear(@Valid @RequestBody ProgramacionDTO data) {
		ProgramacionDTO creada = programacionService.create(data);
		return ResponseEntity.created(URI.create("/api/programaciones/" + creada.getId())).body(creada);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar código y carrera de una programación")
	public ProgramacionDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProgramacionDTO data) {
		return programacionService.updateById(id, data);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar una programación y todos sus bloques")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		programacionService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
