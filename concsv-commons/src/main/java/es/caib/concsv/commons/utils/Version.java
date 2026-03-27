package es.caib.concsv.commons.utils;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

/**
 * Bean amb dades de la versió. Serà el mateix per tothom per tant el definim dins l'scope
 * d'aplicació. Les agafa del fitxer Vesion.properties del mateix package.
 *
 * @author areus
 * @author anadal
 */
@Slf4j
@Getter
@ToString
@Named
@ApplicationScoped
public class Version {

    private static final DateTimeFormatter BUILD_TIME_PATTERN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String version;
    private String buildTime;
    private String scmRevision;
    private String scmBranch;
    private String jdkVersion;
    private String projectName;

    /**
     * Inicialitza el bean amb els valors de Version.properties
     */
    @PostConstruct
    protected void init() {
        /* Agafa fitxer Version.properties amb el mateix package */
        ResourceBundle bundle = ResourceBundle.getBundle("concsv.version.Version");
        version = bundle.getString("project.version");
        try {
            buildTime = ZonedDateTime
                    .parse(bundle.getString("project.buildtime"))
                    .withZoneSameInstant(ZoneId.systemDefault())
                    .format(BUILD_TIME_PATTERN);
        } catch (DateTimeParseException dtpe) {
            log.error("No s'ha pogut obtenir la data de compilació.", dtpe);
        }
        scmRevision = bundle.getString("scm.revision");
        scmBranch = bundle.getString("scm.branch");
        jdkVersion = bundle.getString("jdk.version");
        projectName = bundle.getString("project.name");

        log.info("Versió: {}", this.toString());
    }

}