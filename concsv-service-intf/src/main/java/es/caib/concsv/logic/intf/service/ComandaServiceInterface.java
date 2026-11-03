package es.caib.concsv.logic.intf.service;

import es.caib.comanda.model.server.monitoring.*;
import es.caib.concsv.logic.intf.enums.ResultTypeEnum;
import es.caib.concsv.logic.intf.exception.GenericServiceException;
import es.caib.concsv.logic.intf.model.FitxerStream;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ComandaServiceInterface {

	/**
	 * Mida màxima (bytes) d'un fitxer de log que es retorna dins un JSON. Per sobre d'aquesta mida,
	 * llegir-lo sencer a memòria (més la còpia en zip i en Base64) posa en perill el heap; a partir de
	 * 2 GB, a més, no cap en un array i acaba en {@code OutOfMemoryError: Required array size too large}.
	 */
	long MIDA_MAXIMA_LOG_JSON = 100L * 1024 * 1024;

	/* Salut */
	AppInfo appInfo(Map<String, Object> manifestInfo, String serverRoot) throws GenericServiceException;

	SalutInfo checkSalut(String versio, String performanceUrl) throws GenericServiceException;

	/* Estadístiques */
	RegistresEstadistics consultaUltimesEstadistiques();

	RegistresEstadistics consultaEstadistiques(LocalDate data);

	List<RegistresEstadistics> consultaEstadistiques(LocalDate dataInici, LocalDate dataFi);

	List<DimensioDesc> getDimensions();

	List<IndicadorDesc> getIndicadors();

	void registrarPeticio(String origen, ResultTypeEnum result, long durationMillis);

	void flushEstadisticas(LocalDate date);

	void netejarEstadistiques(LocalDate date);

	/* Monitor d'integracions */
	/** Desa a la base de dades les peticions a les integracions pendents. */
	void flushIntegracions();

	/** Esborra les peticions a les integracions més antigues que la retenció configurada. */
	void netejarIntegracions();

	/* Logs */
	/**
	 * Retorna el fitxer sencer dins un JSON (contingut en Base64), per tant el carrega a memòria.
	 * Si el fitxer supera {@link #MIDA_MAXIMA_LOG_JSON} llança una excepció sense llegir-lo:
	 * s'ha de fer servir {@link #getFitxerStreamByNom(String)}.
	 */
	FitxerContingut getFitxerByNom(String nomFitxer) throws GenericServiceException;

	/**
	 * Retorna el fitxer per enviar-lo en streaming (comprimit en zip si no ho estava), sense carregar-lo
	 * a memòria. Qui el rep ha de tancar l'{@code InputStream}.
	 */
	FitxerStream getFitxerStreamByNom(String nomFitxer) throws GenericServiceException;

	List<String> llegitUltimesLinies(String nomFitxer, Long nLinies) throws GenericServiceException;

	List<FitxerInfo> llistarFitxers() throws GenericServiceException;

}
