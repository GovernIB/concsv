package es.caib.concsv.api.interna;


import io.swagger.v3.jaxrs2.integration.OpenApiServlet;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.util.List;
import java.util.Set;

@WebServlet(urlPatterns = "/openapi.json", loadOnStartup = 1)
public class OpenApiConfig extends OpenApiServlet {
	@Override
	public void init(ServletConfig config) throws ServletException {
		Info info = new Info()
			.title("Concsv API Interna")
			.version("1.0")
			.description("Documentación OpenAPI de la API interna");

        SecurityScheme basicAuthScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic")
                .name("Authorization")
                .description("Autenticació bàsica: introdueix el teu usuari i contrasenya de domini");

        OpenAPI openAPI = new OpenAPI()
                .info(info)
                .servers(List.of(new Server().url("/concsvapi/interna")))
                .components(new Components().addSecuritySchemes("basic-auth", basicAuthScheme));

		SwaggerConfiguration oasConfig = new SwaggerConfiguration()
			.openAPI(openAPI)
			.prettyPrint(true)
			.resourcePackages(Set.of("es.caib.concsv.api.interna.services"));

		try {
			new GenericOpenApiContextBuilder<>()
				.openApiConfiguration(oasConfig)
				.buildContext(true);
		} catch (OpenApiConfigurationException e) {
			throw new RuntimeException(e);
		}

		super.init(config);
	}
}