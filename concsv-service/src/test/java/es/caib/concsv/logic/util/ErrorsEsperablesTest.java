package es.caib.concsv.logic.util;

import es.caib.concsv.logic.intf.exception.GenericServiceException;
import org.apache.axis.AxisFault;
import org.junit.Test;

import javax.xml.ws.WebServiceException;
import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ErrorsEsperablesTest {

	/** Igual que l'AxisFault que crea HTTPSender quan el servei antic respon 502. */
	private static AxisFault errorHttp(int codi, String missatge) {
		return new AxisFault("HTTP", "(" + codi + ")" + missatge, null, null);
	}

	@Test
	public void errorHttpDelServeiAnticEsEsperable() {
		assertTrue(ErrorsEsperables.isEsperable(errorHttp(502, "Proxy Error")));
		assertTrue(ErrorsEsperables.isEsperable(new GenericServiceException(errorHttp(503, "Service Unavailable"))));
	}

	@Test
	public void errorDeNegociDeCustodiaEsEsperable() {
		AxisFault fault = new AxisFault("El documento ya ha sido eliminado; nested exception is: \n\t"
			+ "es.caib.signatura.error.DocumentoNotFoundException: El documento ya ha sido eliminado");
		assertTrue(ErrorsEsperables.isEsperable(fault));
	}

	@Test
	public void timeoutEsEsperable() {
		WebServiceException ex = new WebServiceException("Could not receive Message.",
			new SocketTimeoutException("Read timed out"));
		assertTrue(ErrorsEsperables.isEsperable(ex));
	}

	@Test
	public void errorsDelCodiNoSonEsperables() {
		assertFalse(ErrorsEsperables.isEsperable(new NullPointerException()));
		assertFalse(ErrorsEsperables.isEsperable(new GenericServiceException(new IllegalStateException("x"))));
		assertFalse(ErrorsEsperables.isEsperable(null));
	}

	@Test
	public void resumEsUnaLiniaAmbLaCausaArrel() {
		String resum = ErrorsEsperables.resum(new GenericServiceException(errorHttp(502, "Proxy Error")));
		assertEquals("org.apache.axis.AxisFault: (502)Proxy Error", resum);
		assertFalse(resum.contains("\n"));

		String resumTimeout = ErrorsEsperables.resum(new WebServiceException("Could not receive Message.",
			new SocketTimeoutException("Read timed out")));
		assertEquals("Could not receive Message. (causa: java.net.SocketTimeoutException: Read timed out)", resumTimeout);
	}

}
