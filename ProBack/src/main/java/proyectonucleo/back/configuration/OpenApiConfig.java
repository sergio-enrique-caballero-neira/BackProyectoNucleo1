package proyectonucleo.back.configuration;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Documentación Swagger. El botón "Authorize" acepta el token devuelto por
 * {@code POST /api/auth/login}.
 */
@Configuration
@OpenAPIDefinition(
		info = @Info(title = "Gestor de Horarios API", version = "1.0",
				description = "Gestión de asignaturas y bloques horarios del Programa de Ingeniería de Sistemas, Universidad El Bosque."),
		security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
