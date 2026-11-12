package es.caib.concsv.logic.service;

import es.caib.concsv.logic.helper.ConfigValues;
import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.enums.ExclusioImprimible;
import es.caib.concsv.logic.intf.exception.GenericServiceException;
import es.caib.concsv.logic.intf.model.DocumentInfo;
import es.caib.concsv.logic.intf.service.NewDigitalArchiveServiceInterface;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Exclusió de la còpia imprimible dels PDF portafoli i amb fitxers adjunts segons la propietat
 * {@code imprimible.exclusio} (#18).
 */
public class HashServiceExclusioImprimibleTest {

	private HashService hashService;
	private ConfigValues configValues;
	private NewDigitalArchiveServiceInterface newDigitalArchiveService;

	@Before
	public void setUp() {
		configValues = Mockito.mock(ConfigValues.class);
		newDigitalArchiveService = Mockito.mock(NewDigitalArchiveServiceInterface.class);
		hashService = new HashService();
		hashService.setConfigValues(configValues);
		hashService.setNewDigitalArchiveService(newDigitalArchiveService);
	}

	@Test
	public void perDefecteNomesSExclouenElsPortafolis() {
		propietat(null);
		assertExclos(pdf(true, true), true);
		assertExclos(pdf(false, true), false);
		assertExclos(pdf(false, false), false);
	}

	@Test
	public void desactivatNoExcloCapPdf() {
		propietat("DESACTIVAT");
		assertExclos(pdf(true, true), false);
		assertExclos(pdf(false, true), false);
	}

	@Test
	public void portafoliAdjuntsExcloEnsDosTipus() {
		propietat("PORTAFOLI_ADJUNTS");
		assertExclos(pdf(true, true), true);
		assertExclos(pdf(false, true), true);
		assertExclos(pdf(false, false), false);
	}

	@Test
	public void elValorNoDistingeixMajusculesNiEspais() {
		propietat("  portafoli_adjunts ");
		assertExclos(pdf(false, true), true);
	}

	@Test
	public void unValorNoValidAplicaElValorPerDefecte() {
		propietat("TOTS");
		assertEquals(ExclusioImprimible.PER_DEFECTE, hashService.getExclusioImprimible());
		assertExclos(pdf(true, true), true);
		assertExclos(pdf(false, true), false);
	}

	/** Els documents exclosos de la descàrrega de l'original funcionen com abans de la 2.2.2. */
	@Test
	public void unDocumentExclosDeLOriginalNoSExcloDeLImprimible() {
		propietat("PORTAFOLI_ADJUNTS");
		DocumentInfo info = pdf(true, true);
		info.setCsvExclos(true);
		assertExclos(info, false);
	}

	/** Com quan el document ve de la memòria cau i la propietat s'ha canviat des del backoffice. */
	@Test
	public void esRecalculaEnCanviarLaPropietat() {
		DocumentInfo info = pdf(true, true);
		propietat("PORTAFOLI");
		assertExclos(info, true);
		propietat("DESACTIVAT");
		assertExclos(info, false);
	}

	/** Els documents sense adjunts (o no PDF) conserven el printable que ja tenien. */
	@Test
	public void sensePortafoliNiAdjuntsNoEsTocaPrintable() {
		propietat("PORTAFOLI_ADJUNTS");
		DocumentInfo info = new DocumentInfo();
		info.setExtensionFormato("xml");
		info.setPrintable(false);
		hashService.aplicaExclusioImprimible(info);
		assertEquals(Boolean.FALSE, info.getImprimibleExclos());
		assertEquals(Boolean.FALSE, info.getPrintable());
	}

	/** La barrera de getPrintableDocument no arriba a descarregar el document. */
	@Test
	public void getPrintableDocumentRebutjaUnPortafoliSenseDescarregarLo() throws Exception {
		propietat(null);
		DocumentInfo info = pdf(true, true);
		info.setHash("csv");
		try {
			hashService.getPrintableDocument(info, "ca");
			fail("S'esperava una excepció");
		} catch (GenericServiceException ex) {
			assertFalse(ex.isLoggable());
			assertTrue(ex.getMessage(), ex.getMessage().contains("portafoli"));
		}
		Mockito.verifyNoInteractions(newDigitalArchiveService);
	}

	@Test
	public void valorsDeLEnumeracio() {
		assertEquals(Optional.of(ExclusioImprimible.PORTAFOLI), ExclusioImprimible.fromValor("portafoli"));
		assertEquals(Optional.empty(), ExclusioImprimible.fromValor(""));
		assertEquals(Optional.empty(), ExclusioImprimible.fromValor(null));
		assertEquals(Optional.empty(), ExclusioImprimible.fromValor("S"));
		assertFalse(ExclusioImprimible.DESACTIVAT.exclou(true, true));
		assertTrue(ExclusioImprimible.PORTAFOLI.exclou(true, false));
		assertFalse(ExclusioImprimible.PORTAFOLI.exclou(false, true));
		assertTrue(ExclusioImprimible.PORTAFOLI_ADJUNTS.exclou(false, true));
		assertFalse(ExclusioImprimible.PORTAFOLI_ADJUNTS.exclou(false, false));
	}

	private void propietat(String valor) {
		Mockito.when(configValues.get(PropertyConfig.PROP_IMPRIMIBLE_EXCLUSIO)).thenReturn(valor);
	}

	private static DocumentInfo pdf(boolean portafoli, boolean fitxersAdjunts) {
		DocumentInfo info = new DocumentInfo();
		info.setExtensionFormato("pdf");
		info.setPrintable(true);
		info.setCsvExclos(false);
		info.setPortafoli(portafoli);
		info.setFitxersAdjunts(fitxersAdjunts);
		return info;
	}

	private void assertExclos(DocumentInfo info, boolean exclos) {
		hashService.aplicaExclusioImprimible(info);
		assertEquals(exclos, info.getImprimibleExclos());
		assertEquals(!exclos, info.getPrintable());
	}

}
