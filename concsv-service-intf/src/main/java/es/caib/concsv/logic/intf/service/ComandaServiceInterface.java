package es.caib.concsv.logic.intf.service;

import es.caib.comanda.model.server.monitoring.*;
import es.caib.concsv.logic.intf.enums.ResultTypeEnum;
import es.caib.concsv.logic.intf.exception.GenericServiceException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ComandaServiceInterface {

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

	/* Logs */
	FitxerContingut getFitxerByNom(String nomFitxer) throws GenericServiceException;

	List<String> llegitUltimesLinies(String nomFitxer, Long nLinies) throws GenericServiceException;

	List<FitxerInfo> llistarFitxers() throws GenericServiceException;

}
