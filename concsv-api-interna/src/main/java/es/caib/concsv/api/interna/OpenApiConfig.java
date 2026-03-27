package es.caib.concsv.api.interna;


import io.swagger.v3.jaxrs2.integration.OpenApiServlet;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.models.info.Info;
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

		SwaggerConfiguration oasConfig = new SwaggerConfiguration()
			.openAPI(new io.swagger.v3.oas.models.OpenAPI()
				.info(info)
				.servers(List.of(new Server().url("/concsvapi/interna"))))
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