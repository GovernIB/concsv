package es.caib.concsv.service.facade;

import es.caib.comanda.model.v1.salut.*;
import es.caib.concsv.service.exception.GenericServiceException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface SalutServiceInterface {
    AppInfo appInfo(Map<String, Object> manifestInfo, HttpServletRequest request) throws GenericServiceException;
    SalutInfo checkSalut(String versio, String performanceUrl) throws GenericServiceException;
}
