package es.caib.concsv.persist.config;

import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.persist.base.config.BasePersistenceConfig;
import es.caib.concsv.persist.base.repository.BaseRepositoryImpl;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Configuració dels components de persistència de Spring.
 * <p>
 * Conviu amb la unitat de persistència {@code concsvPU} del {@code META-INF/persistence.xml}, que
 * gestiona JBoss i fan servir els serveis CDI (estadístiques de Comanda). Són dues unitats
 * independents: aquesta només coneix les entitats del paquet {@code persist.entity} i aquella
 * només les que declara explícitament el persistence.xml.
 *
 * @author Límit Tecnologies
 */
@Configuration
@EnableJpaRepositories(
		basePackages = { BaseConfig.BASE_PACKAGE + ".persist.repository" },
		entityManagerFactoryRef = "mainEntityManager",
		transactionManagerRef = "mainTransactionManager",
		repositoryBaseClass = BaseRepositoryImpl.class
)
public class PersistenceConfig extends BasePersistenceConfig {

	/**
	 * Ubicació inexistent a posta: amb {@code classpath*:} no troba res i Spring no llegeix cap
	 * persistence.xml. Si llegís el de {@code concsvPU} en resoldria el {@code jta-data-source} per
	 * JNDI, cosa que en mode Spring Boot (sense JBoss) fa petar l'arrencada.
	 */
	private static final String PERSISTENCE_XML_INEXISTENT = "classpath*:META-INF/concsv-spring-persistence.xml";

	@Override
	@Bean
	@Primary
	public LocalContainerEntityManagerFactoryBean mainEntityManager(EntityManagerFactoryBuilder builder) {
		LocalContainerEntityManagerFactoryBean entityManager = super.mainEntityManager(builder);
		entityManager.setPersistenceXmlLocation(PERSISTENCE_XML_INEXISTENT);
		return entityManager;
	}

	@Override
	protected String[] getEntityPackages() {
		return new String[] { BaseConfig.BASE_PACKAGE + ".persist.entity" };
	}

}
