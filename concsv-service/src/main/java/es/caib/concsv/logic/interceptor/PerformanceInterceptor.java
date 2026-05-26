package es.caib.concsv.logic.interceptor;

import es.caib.concsv.logic.annotation.Performance;
import es.caib.concsv.logic.annotation.PerformanceInt;
import es.caib.concsv.logic.intf.config.PropertyConfig;
import org.apache.log4j.Logger;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * Clase interceptora para la gestión de rendimiento
 */
@PerformanceInt
@Interceptor
public class PerformanceInterceptor {

	private final static Logger LOGGER = Logger.getLogger(PerformanceInterceptor.class);

    private Integer minPerformanceTimeMS = 500;
    private Integer middlePerformanceTimeMS = 1000;
    private Integer maxPerformanceTimeMS = 2000;

	public PerformanceInterceptor() {}

	@AroundInvoke
	public Object performance(InvocationContext ic) throws Exception {
		try {
            // Tiempo inicial
            Long tIni = System.currentTimeMillis();
            // Ejecuto el método
            Object result = ic.proceed();
            // Tiempo final
            Long tEnd = System.currentTimeMillis();

            Boolean forcePerformance = false;

            Performance sp = ic.getMethod().getAnnotation(Performance.class);
            if (sp!=null) {
                forcePerformance = sp.forcePerformance();
            }

            String logPerformace = ConfigProvider.getConfig().getValue(PropertyConfig.PROP_PERFORMANCE, String.class);
            if (forcePerformance || logPerformace.equals("S")) {
                LOGGER.info("----------------------------------------------------------------------------------------");
                LOGGER.info("- Ejecución de método: "+ic.getTarget().getClass().getName()+"."+ic.getMethod().getName());
                LOGGER.info("- Parámetros");
                int pAux = 0;
                for (Object parameter: ic.getParameters()) {
                    if (parameter!=null) {
                        LOGGER.info("- \t\t P["+pAux+"] | "+parameter.getClass().getName()+" = "+parameter);
                    }
                    pAux++;
                }
                Long execTime = tEnd-tIni;
                LOGGER.info("- Tiempo de ejecución: "+execTime+" ms");
                if (execTime <= this.minPerformanceTimeMS) {
                    LOGGER.info("- Status: OK");
                } else if (execTime > this.minPerformanceTimeMS && execTime <= this.middlePerformanceTimeMS) {
                    LOGGER.info("- Status: MIDDLE");
                } else if (execTime > this.middlePerformanceTimeMS && execTime <= this.maxPerformanceTimeMS ) {
                    LOGGER.info("- Status: SLOW");
                } else {
                    LOGGER.info("- Status: VERY SLOW");
                }
                LOGGER.info("----------------------------------------------------------------------------------------");
            }
			return result;
		} catch (Exception e) {
			throw e;
		}
	}

}
