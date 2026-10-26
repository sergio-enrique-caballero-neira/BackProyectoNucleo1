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
import proyectonucleo.back.dto.ProgramaDTO;
import proyectonucleo.back.service.ProgramaService;

/**
 * Endpoints de los programas académicos.
 */
@RestController
@RequestMapping("/api/programas")
@Tag(name = "Programas")
public class ProgramaController {

	private final ProgramaService programaService;

	public ProgramaController(ProgramaService programaService) {
		this.programaService = programaService;
	}

	@GetMapping
	@Operation(summary = "Consultar los programas")
	public List<ProgramaDTO> listar() {
		return programaService.getAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar un programa")
	public ProgramaDTO obtener(@PathVariable Long id) {
		return programaService.getById(id);
	}

	@PostMapping
	@Operation(summary = "Crear un programa")
	public ResponseEntity<ProgramaDTO> crear(@Valid @RequestBody ProgramaDTO data) {
		ProgramaDTO creado = programaService.create(data);
		return ResponseEntity.created(URI.create("/api/programas/" + creado.getId())).body(creado);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar código y nombre de un programa")
	public ProgramaDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProgramaDTO data) {
		return programaService.updateById(id, data);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar un programa sin horarios")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		programaService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
