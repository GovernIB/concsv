package es.caib.concsv.ejb.interceptor;

import es.caib.concsv.ejb.annotation.ErrorInt;
import es.caib.concsv.service.exception.DuplicatedHashException;
import es.caib.concsv.service.exception.GenericServiceException;
import org.apache.log4j.Logger;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase interceptora para la gestión de errores
 */
@ErrorInt
@Interceptor
public class ErrorInterceptor {

	private Logger log = Logger.getLogger(this.getClass());

	public ErrorInterceptor() {}

	@AroundInvoke
	public Object advice(InvocationContext ic) throws Exception {
		try {
			// Ejecuto el método
			return ic.proceed();
		} catch (DuplicatedHashException ex) {
			throw ex;
		} catch (Exception ex) {
			List<String> parameters = new ArrayList<String>();
			String method = ic.getTarget().getClass().getName()+"."+ic.getMethod().getName();
			log.error("----------------------------------------------------------------------------------------");
			log.error("--------------- Método y parámetros de la llamada que ha generado error ----------------");
			log.error("----------------------------------------------------------------------------------------");
			log.error("- Ejecución de método: "+method);
			log.error("- Parámetros");
			int pAux = 0;
			String parameterStr;
			for (Object parameter: ic.getParameters()) {
				if (parameter!=null) {
					parameterStr = "P["+pAux+"]: "+parameter.getClass().getName()+" = "+parameter;
					log.error("\t\t - "+parameterStr);
					parameters.add(parameterStr);
				}
				pAux++;
			}
			log.error("----------------------------------------------------------------------------------------");
			//log.error(ex);
			return ic.proceed();
			//throw ex;
		}
	}

}
