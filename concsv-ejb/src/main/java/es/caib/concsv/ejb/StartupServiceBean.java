package es.caib.concsv.ejb;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * EJB únic que s'executa a la inicialització.
 *
 * @author areus
 */
@Slf4j
@Singleton
@Startup
public class StartupServiceBean {

    /**
     * Executat a l'inici de l'aplicació.
     */
    @PostConstruct
    private void init() {
        // Aquí es podrien llegir les opcions de configuració, i comprovar que tots els paràmetres necessaris hi són,
        // o fixar els valors per defecte pels que no hi siguin, programar timers no persistents, ...
        System.out.println("   █████████                        █████████   █████████  █████   █████");
        System.out.println("  ███░░░░░███                      ███░░░░░███ ███░░░░░███░░███   ░░███ ");
        System.out.println(" ███     ░░░   ██████  ████████   ███     ░░░ ░███    ░░░  ░███    ░███ ");
        System.out.println("░███          ███░░███░░███░░███ ░███         ░░█████████  ░███    ░███ ");
        System.out.println("░███         ░███ ░███ ░███ ░███ ░███          ░░░░░░░░███ ░░███   ███  ");
        System.out.println("░░███     ███░███ ░███ ░███ ░███ ░░███     ███ ███    ░███  ░░░█████░   ");
        System.out.println(" ░░█████████ ░░██████  ████ █████ ░░█████████ ░░█████████     ░░███     ");
        System.out.println("  ░░░░░░░░░   ░░░░░░  ░░░░ ░░░░░   ░░░░░░░░░   ░░░░░░░░░       ░░░      ");
        System.out.println("================================================================( ejb )=");
    }

    /**
     * Executat quan s'atura l'aplicació.
     */
    @PreDestroy
    private void destroy() {
        log.info("Aturada del mòdul EJB");
    }
}
