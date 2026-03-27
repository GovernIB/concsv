package es.caib.concsv.api.interna.services;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.caib.comanda.model.v1.estadistica.*;
import es.caib.concsv.persistence.model.EnviamentOrigen;
import es.caib.concsv.persistence.model.EnviamentTipus;
import es.caib.concsv.service.enums.ResultTypeEnum;
import es.caib.concsv.service.exception.GenericServiceException;
import es.caib.concsv.service.facade.EstadisticaServiceInterface;
import es.caib.concsv.service.facade.HashServiceInterface;
import es.caib.concsv.service.facade.SalutServiceInterface;
import es.caib.concsv.service.model.DocumentContent;
import es.caib.concsv.service.model.DocumentInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

@Slf4j
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name="api", description="Api interna")
public class FrontResource {

    private static final Response.Status NOT_FOUND_STATUS = Response.Status.NO_CONTENT;

    public enum ResultType { OK, INVALID, ERROR }

    @EJB
    HashServiceInterface hashService;
    @Context
    ServletContext servletContext;
    @Inject
    SalutServiceInterface salutService;
    @EJB
    EstadisticaServiceInterface estadisticaService;

    /**
     * Servicio para recuperar un documento con la estampación de CSV, QR,...
     */
    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
        summary = "Prova de connexió",
        description = "Retorna un 'OK' i l'hora del sistema per comprovar que el servei està actiu",
        responses = {
            @ApiResponse(responseCode = "200", description = "Servicio activo",
                content = @Content(mediaType = "text/plain",
                    schema = @Schema(implementation = String.class)))
        }
    )
    public Response test() {
	    return Response.ok("Ok " + new Date().getTime()).build();
    }
    
    /**
     * Servicio para recuperar un documento con la estampación de CSV, QR,...
     */
    @GET
    @Path("/printable/{csv:(.+)?}")
    @Operation(summary = "Recuperar la versió imprimible d'un document a partir d'un codi CSV")
    public Response decodeHashPrintable(
        @Parameter(description = "Codi CSV del document") @PathParam("csv") String csv,
        @Parameter(description = "Idioma del document (ca per defecte)") @QueryParam("lang") String lang) {
        return ejecutarYRegistrar(EnviamentTipus.IMPRIMIBLE.name(), () -> {
            DocumentInfo documentInfo = hashService.checkHash(csv);
            if (documentInfo == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            final String idioma = (lang != null) ? lang : "ca";
            DocumentContent doc = hashService.getPrintableDocument(documentInfo, idioma);
            return Response.ok(new ByteArrayInputStream(doc.getContent()), MediaType.APPLICATION_OCTET_STREAM)
                    .header("content-disposition","attachment; filename=" + doc.getFileName())
                    .build();
        });
    }

    /**
     * Servicio para recuperar un documento con la estampación de CSV, QR,... a partir de un UUID de Alfresco
     */
    @GET
    @Path("/printable/uuid/{uuid:(.+)?}")
    @Operation(summary = "Recuperar la versió imprimible d'un document a partir d'un codi UUID")
    public Response decodeHashPrintableFromUUID(
        @Parameter(description = "Codi UUID del document") @PathParam("uuid") String uuid,
        @Parameter(description = "Idioma del document (ca per defecte)") @QueryParam("lang") String lang) {
        return ejecutarYRegistrar(EnviamentTipus.IMPRIMIBLE.name(), () -> {
            DocumentInfo documentInfo = hashService.checkHashFromUUID(uuid);
            if (documentInfo == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            if (documentInfo.getHash() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El document no té CSV.")
                        .build();
            }
            final String idioma = (lang != null) ? lang : "ca";
            DocumentContent doc = hashService.getPrintableDocument(documentInfo, idioma);
            return Response.ok(new ByteArrayInputStream(doc.getContent()), MediaType.APPLICATION_OCTET_STREAM)
                    .header("content-disposition","attachment; filename=" + doc.getFileName())
                    .build();
        });
    }

    /**
     * Servicio para recuperar un documento original con CSV
     */
    @GET
    @Path("/original/{csv:(.+)?}")
    @Operation(summary = "Recuperar l'original d'un document a partir d'un codi CSV")
    public Response decodeHashOriginal(
        @Parameter(description = "Codi CSV del document") @PathParam("csv") String csv) {
        return ejecutarYRegistrar(EnviamentTipus.ORIGINAL.name(), () -> {
            if (hashService.getCsvExclosos().contains(csv)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Descàrrega de l'original no permesa")
                        .build();
            }
            DocumentInfo documentInfo = hashService.checkHash(csv);
            if (documentInfo == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            DocumentContent doc = hashService.getDocument(documentInfo, true);
            return Response.ok(new ByteArrayInputStream(doc.getContent()), MediaType.APPLICATION_OCTET_STREAM)
                    .header("content-disposition","attachment; filename=" + doc.getFileName())
                    .build();
        });
    }

	/**
     * Servicio para recuperar un documento original a partir de un UUID de Alfresco
     */
    @GET
    @Path("/original/uuid/{uuid:(.+)?}")
    @Operation(summary = "Recuperar l'original d'un document a partir d'un codi UUID")
    public Response decodeHashOriginalFromUUID(
        @Parameter(description = "Codi UUID del document") @PathParam("uuid") String uuid) {
        return ejecutarYRegistrar(EnviamentTipus.ORIGINAL.name(), () -> {
            DocumentInfo documentInfo = hashService.checkHashFromUUID(uuid);
            if (documentInfo == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            if (hashService.getCsvExclosos().contains(documentInfo.getHash())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Descàrrega de l'original no permesa")
                        .build();
            }
            DocumentContent doc = hashService.getDocument(documentInfo, true);
            return Response.ok(new ByteArrayInputStream(doc.getContent()), MediaType.APPLICATION_OCTET_STREAM)
                    .header("content-disposition","attachment; filename=" + doc.getFileName())
                    .build();
        });
    }
    
    /**
     * Servicio para recuperar un documento ENI con CSV
     */
    @GET
    @Path("/enidoc/{csv:(.+)?}")
    @Operation(summary = "Recuperar un document ENI a partir d'un codi CSV")
    public Response decodeHashEniDoc(
        @Parameter(description = "Codi CSV del document") @PathParam("csv") String csv) {
        return ejecutarYRegistrar(EnviamentTipus.ENI.name(), () -> {
            DocumentInfo documentInfo = hashService.checkHash(csv);
            if (documentInfo == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            DocumentContent doc = hashService.getEniDocument(documentInfo);
            return Response.ok(new ByteArrayInputStream(doc.getContent()), MediaType.APPLICATION_OCTET_STREAM)
                    .header("content-disposition","attachment; filename=" + documentInfo.getDocumentCode())
                    .build();
        });
    }

    /**
     * Servicio para recuperar un documento ENI a partir de un UUID de Alfresco
     */
    @GET
    @Path("/enidoc/uuid/{uuid:(.+)?}")
    @Operation(summary = "Recuperar un document ENI a partir d'un codi UUID")
    public Response decodeHashEniDocFromUUID(
        @Parameter(description = "Codi UUID del document") @PathParam("uuid") String uuid) {
        return ejecutarYRegistrar(EnviamentTipus.ENI.name(), () -> {
            DocumentInfo documentInfo = hashService.checkHashFromUUID(uuid);
            if (documentInfo == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            if (documentInfo.getHash() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El document no té CSV.")
                        .build();
            }
            DocumentContent doc = hashService.getEniDocument(documentInfo);
            return Response.ok(new ByteArrayInputStream(doc.getContent()), MediaType.APPLICATION_OCTET_STREAM)
                    .header("content-disposition","attachment; filename=" + documentInfo.getDocumentCode())
                    .build();
        });
    }
    
    /**
     * Servicio para recuperar los metadatos de un documento con CSV
     */
    @GET
    @Path("/metadata/{csv:(.+)?}")
    @Operation(summary = "Recuperar les metadades d'un document a partir d'un codi CSV")
    public Response decodeHashMetadata(
        @Parameter(description = "Codi CSV del document") @PathParam("csv") String csv) {
        return ejecutarYRegistrar(EnviamentTipus.METADATOS.name(), () -> {
            DocumentInfo documentInfo = hashService.checkHash(csv);
            if (documentInfo == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            String json = new ObjectMapper().writeValueAsString(documentInfo.getMetadata());
            return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
        });
    }

    /**
     * Servicio para recuperar los metadatos de un documento a partir de un UUID de Alfresco
     */
    @GET
    @Path("/metadata/uuid/{uuid:(.+)?}")
    @Operation(summary = "Recuperar les metadades d'un document a partir d'un codi UUID")
    public Response decodeHashMetadataFromUUID(
        @Parameter(description = "Codi UUID del document") @PathParam("uuid") String uuid) {
        return ejecutarYRegistrar(EnviamentTipus.METADATOS.name(), () -> {
            DocumentInfo documentInfo = hashService.checkHashFromUUID(uuid);
            if (documentInfo == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            String json = new ObjectMapper().writeValueAsString(documentInfo.getMetadata());
            return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
        });
    }

    /** Mètode per al monotireig de l'aplicació **/
    @GET
    @Path("/v1/salut/info")
    @Operation(summary = "(Integració amb Comanda) Retorna informació de l'aplicació")
    public Response appInfo(@Context HttpServletRequest request) throws IOException, GenericServiceException {
        return Response
            .ok(salutService.appInfo(getManifestProperties(), request),
                MediaType.APPLICATION_JSON_TYPE)
            .build();
    }

    /** Mètode per al monotireig un element **/
    @GET
    @Path("/v1/salut")
    @Operation(summary = "(Integració amb Comanda) Retorna l'estat de l'aplicació")
    public Response health(@Context HttpServletRequest request) throws IOException, GenericServiceException {
        Map<String, Object> manifestInfo = getManifestProperties();
        return Response
            .ok(salutService.checkSalut(
                (String) manifestInfo.get("Implementation-Version"),
                request.getRequestURL().toString() + "Performance")
                , MediaType.APPLICATION_JSON_TYPE)
            .build();
    }

    @GET
    @Path("/v1/salutPerformance")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
        summary = "Prova de connexió",
        description = "(Integració amb Comanda) Retorna sí el controlador és actiu",
        responses = {
            @ApiResponse(responseCode = "200", description = "Servicio activo",
                content = @Content(mediaType = "text/plain",
                    schema = @Schema(implementation = String.class)))
        }
    )
    public String healthCheck() {
        return "OK";
    }

    @GET
    @Path("/v1/estadistiques/info")
    @Operation(summary = "(Integració amb Comanda) Retorna les dimensions disponibles")
    public EstadistiquesInfo statsInfo() {
        List<DimensioDesc> dimensions = estadisticaService.getDimensions();
        List<IndicadorDesc> indicadors = estadisticaService.getIndicadors();
        return EstadistiquesInfo.builder().codi("CSV").dimensions(dimensions).indicadors(indicadors).build();
    }

    @GET
    @Path("/v1/estadistiques")
    @Operation(summary = "(Integració amb Comanda) Recupera les dades de l'últim dia registrat")
    public RegistresEstadistics estadistiques() {
        return estadisticaService.consultaUltimesEstadistiques();
    }

    @GET
    @Path("/v1/estadistiques/{dies}")
    @Operation(summary = "(Integració amb Comanda) Recupera les dades dels últims dies registrats")
    public List<RegistresEstadistics> estadistiques(
            @Parameter(description = "Nombre de dies enrere des d'avui") @PathParam("dies") Integer dies) {
        List<RegistresEstadistics> result = new ArrayList<>();
        LocalDate data = LocalDate.now().minusDays(1);
        for (int i = 0; i < dies; i++) {
            result.add(estadisticaService.consultaEstadistiques(data));
            data = data.minusDays(1);
        }
        return result;
    }

    @GET
    @Path("/v1/estadistiques/of/{data}")
    @Operation(summary = "(Integració amb Comanda) Recupera les dades del dia indicat")
    public RegistresEstadistics estadistiques(
            @Parameter(description = "Data a consultar (format dd-MM-yyyy)") @PathParam("data") String data) {
        LocalDate date = LocalDate.parse(data, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        return estadisticaService.consultaEstadistiques(date);
    }

    @GET
    @Path("/v1/estadistiques/from/{dataInici}/to/{dataFi}")
    @Operation(summary = "(Integració amb Comanda) Recupera les dades entre les dues dades sol·licitades")
    public List<RegistresEstadistics> estadistiques(
            @Parameter(description = "Data d'inici (format dd-MM-yyyy)") @PathParam("dataInici") String dataInici,
            @Parameter(description = "Data de fi (format dd-MM-yyyy)") @PathParam("dataFi") String dataFi) {
        LocalDate dataFrom = LocalDate.parse(dataInici, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        LocalDate dataTo = LocalDate.parse(dataFi, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        LocalDate startDate = dataFrom.isBefore(dataTo) ? dataFrom : dataTo;
        LocalDate endDate = dataFrom.isBefore(dataTo) ? dataTo : dataFrom;
        LocalDate ahir = LocalDate.now().minusDays(1);
        if (endDate.isAfter(ahir)) {
            endDate = ahir;
        }
        return estadisticaService.consultaEstadistiques(startDate, endDate);
    }

    private Map<String, Object> getManifestProperties() throws IOException {
        InputStream manifestIs = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF");
        if (manifestIs != null) {
            Manifest manifest = new Manifest(manifestIs);
            Attributes attributes = manifest.getMainAttributes();
            Map<String, Object> props = attributes.keySet().stream().collect(Collectors.toMap(
                k -> k.toString(),
                k -> attributes.get(k)));
            return props;
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Executa una acció que retorna un {@link Response} i registra automàticament
     * l’estadística de la petició segons el resultat.
     *
     * <p>S’avalua el {@link Response.Status} retornat:
     * <ul>
     *   <li>{@link Response.Status#NO_CONTENT} → {@link ResultTypeEnum#INVALID}</li>
     *   <li>Codis d’èxit (2xx) → {@link ResultTypeEnum#OK}</li>
     *   <li>Qualsevol altre codi → {@link ResultTypeEnum#ERROR}</li>
     * </ul>
     *
     * <p>Si es llença una excepció durant l’execució de l’acció, es registra
     * automàticament com a {@link ResultTypeEnum#ERROR} i es retorna un
     * {@link Response.Status#INTERNAL_SERVER_ERROR} amb el missatge de l’excepció.
     *
     * @param key Identificador de l’operació que s’utilitzarà per registrar l’estadística.
     * @param accion L’acció a executar, representada com un {@link Callable<Response>}.
     * @return El {@link Response} retornat per l’acció, o un {@link Response} d’error
     *         en cas d’excepció.
     */
    private Response ejecutarYRegistrar(@NotNull String key, Callable<Response> accion) {
        long start = System.currentTimeMillis();
        String fullKey = key + ":" + EnviamentOrigen.API.name();
        ResultTypeEnum resultType = ResultTypeEnum.ERROR;

        try {
            Response response = accion.call();
            Response.StatusType status = response.getStatusInfo();

            if (status.getStatusCode() == Response.Status.NO_CONTENT.getStatusCode()) {
                resultType = ResultTypeEnum.INVALID;
            } else if (status.getFamily() == Response.Status.Family.SUCCESSFUL) {
                resultType = ResultTypeEnum.OK;
            }

            return response;
        } catch (Exception ex) {
            log.error("Error executant l'acció {}: {}", fullKey, ex.getMessage(), ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        } finally {
            long duration = System.currentTimeMillis() - start;
            try {
                estadisticaService.registrarPeticio(fullKey, resultType, duration);
            } catch (Exception e) {
                //No llançarem l'error per permetre que els usuaris descarreguin els adjunts en cas de no funcionar la implementació amb estadística.
                log.debug("No s'ha pogut registrar la petició en estadística per {}: {}", fullKey, e.getMessage(), e);
            }
        }
    }

}
