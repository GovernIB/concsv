package es.caib.concsv.api.interna;

import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/")
public class JAXRSConfiguration extends Application {

    /**
     * Les aplicacions JAX-RS necessiten un constructor buid.
     */
    public JAXRSConfiguration() {

    }

    /**
     * Podem introduir tasques a realitzar per la inicialització de l'API REST.
     */
    @PostConstruct
    private void init() {
        System.out.println("   █████████                        █████████   █████████  █████   █████");
        System.out.println("  ███░░░░░███                      ███░░░░░███ ███░░░░░███░░███   ░░███ ");
        System.out.println(" ███     ░░░   ██████  ████████   ███     ░░░ ░███    ░░░  ░███    ░███ ");
        System.out.println("░███          ███░░███░░███░░███ ░███         ░░█████████  ░███    ░███ ");
        System.out.println("░███         ░███ ░███ ░███ ░███ ░███          ░░░░░░░░███ ░░███   ███  ");
        System.out.println("░░███     ███░███ ░███ ░███ ░███ ░░███     ███ ███    ░███  ░░░█████░   ");
        System.out.println(" ░░█████████ ░░██████  ████ █████ ░░█████████ ░░█████████     ░░███     ");
        System.out.println("  ░░░░░░░░░   ░░░░░░  ░░░░ ░░░░░   ░░░░░░░░░   ░░░░░░░░░       ░░░      ");
        System.out.println("========================================================( api-interna )=");
    }

}
