package es.caib.concsv.logic.util;

import org.apache.axis.AxisFault;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Identifica els errors esperables de les integracions externes (servei caigut, error HTTP, timeout, error
 * de negoci retornat pel servei remot) per poder-los registrar en una sola línia. La traça d'aquests errors
 * només mostra el codi client (Axis, CXF, JBoss) i no aporta res per diagnosticar-los, però quan el servei
 * remot falla es repeteixen milers de vegades i omplen el log.
 *
 * <p>Els errors que no s'identifiquen com a esperables s'han de seguir registrant amb la traça completa.
 */
public final class ErrorsEsperables {

	/** Errors de xarxa o de resposta HTTP. Es comparen pel nom per no dependre de CXF en aquest mòdul. */
	private static final Set<String> CLASSES_ERROR_COMUNICACIO = new HashSet<>(Arrays.asList(
		"java.net.SocketTimeoutException",
		"java.net.ConnectException",
		"java.net.UnknownHostException",
		"java.net.NoRouteToHostException",
		"org.apache.cxf.transport.http.HTTPException"));

	/** Paquet de les excepcions de negoci que retorna el servei antic de custòdia (p. ex. DocumentoNotFoundException). */
	private static final String PAQUET_ERRORS_CUSTODIA = "es.caib.signatura.error.";

	private static final int PROFUNDITAT_MAXIMA = 15;
	private static final int LONGITUD_MAXIMA_MISSATGE = 300;

	private ErrorsEsperables() {
	}

	/**
	 * Indica si l'error (o alguna de les seves causes) és un error de comunicació o una resposta d'error del
	 * servei remot, i no un error del codi de ConCSV.
	 */
	public static boolean isEsperable(Throwable th) {
		Throwable t = th;
		for (int i = 0; t != null && i < PROFUNDITAT_MAXIMA; i++) {
			String nomClasse = t.getClass().getName();
			if (CLASSES_ERROR_COMUNICACIO.contains(nomClasse) || nomClasse.startsWith(PAQUET_ERRORS_CUSTODIA)) {
				return true;
			}
			if (t instanceof AxisFault) {
				AxisFault fault = (AxisFault) t;
				QName codi = fault.getFaultCode();
				// Axis crea els errors HTTP (502, 503, 404...) amb el codi {http://xml.apache.org/axis/}HTTP
				if (codi != null && "HTTP".equals(codi.getLocalPart())) {
					return true;
				}
				String missatge = fault.getMessage();
				if (missatge != null && missatge.contains(PAQUET_ERRORS_CUSTODIA)) {
					return true;
				}
			}
			t = (t.getCause() == t) ? null : t.getCause();
		}
		return false;
	}

	/**
	 * Resum d'una línia de l'error: la causa arrel (classe i missatge) i, si és diferent, el missatge de
	 * l'error rebut.
	 */
	public static String resum(Throwable th) {
		if (th == null) {
			return "null";
		}
		Throwable arrel = th;
		for (int i = 0; i < PROFUNDITAT_MAXIMA && arrel.getCause() != null && arrel.getCause() != arrel; i++) {
			arrel = arrel.getCause();
		}
		String resumArrel = arrel.getClass().getName() + ": " + primeraLinia(arrel.getMessage());
		if (arrel == th) {
			return resumArrel;
		}
		String missatge = primeraLinia(th.getMessage());
		return resumArrel.contains(missatge) ? resumArrel : missatge + " (causa: " + resumArrel + ")";
	}

	private static String primeraLinia(String text) {
		if (text == null) {
			return "null";
		}
		String linia = text.trim();
		int salt = linia.indexOf('\n');
		if (salt >= 0) {
			linia = linia.substring(0, salt).trim();
		}
		return linia.length() > LONGITUD_MAXIMA_MISSATGE ? linia.substring(0, LONGITUD_MAXIMA_MISSATGE) + "..." : linia;
	}

}
