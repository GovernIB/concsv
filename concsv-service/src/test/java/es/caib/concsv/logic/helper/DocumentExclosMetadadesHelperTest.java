package es.caib.concsv.logic.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import es.caib.concsv.logic.helper.DocumentExclosMetadadesHelper.DocumentArxiu;
import es.caib.concsv.logic.helper.DocumentExclosMetadadesHelper.Metadades;
import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.DocumentExclosEstatEnum;
import es.caib.concsv.logic.intf.model.DocumentExclosOrigenEnum;
import es.caib.concsv.logic.intf.model.DocumentExclosResource;
import es.caib.concsv.logic.intf.model.DocumentExclosUbicacioEnum;

/**
 * Proves de {@link DocumentExclosMetadadesHelper}: les peticions a l'Arxiu i a la custòdia es
 * substitueixen per respostes preparades.
 */
public class DocumentExclosMetadadesHelperTest {

	private static final String UUID = "12e2f405-ac11-49ed-9b93-c2f11cac41e7";
	private static final String CSV = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	private static final String XML_CUSTODIA = "<vcd:ViewCustodiaData xmlns:vcd=\"urn:prova\">"
			+ "<vcd:CustodiaOK>S</vcd:CustodiaOK>"
			+ "<vcd:Documento><vcd:Nombre>Informe antic</vcd:Nombre></vcd:Documento>"
			+ "<vcd:Sellos>"
			+ "<vcd:Sello><vcd:TipoSello>FIRMANTE</vcd:TipoSello><vcd:FechaSello>01/04/2020 10:00</vcd:FechaSello></vcd:Sello>"
			+ "<vcd:Sello><vcd:TipoSello>CUSTODIA</vcd:TipoSello><vcd:FechaSello>31/03/2020 13:48</vcd:FechaSello></vcd:Sello>"
			+ "</vcd:Sellos></vcd:ViewCustodiaData>";

	private ConfigHelper configHelper;
	private DocumentExclosMetadadesHelper helper;

	@Before
	public void setUp() {
		configHelper = mock(ConfigHelper.class);
		helper = spy(new DocumentExclosMetadadesHelper(configHelper));
		activa(true, true);
	}

	private void activa(boolean arxiu, boolean custodia) {
		doReturn(arxiu).when(helper).consultaActiva(PropertyConfig.PROP_CONSULT_NEW_DIGITAL_ARCHIVE);
		doReturn(custodia).when(helper).consultaActiva(PropertyConfig.PROP_CONSULT_OLD_SAFEKEEPING);
	}

	private static DocumentArxiu document(String id, String nom, Object... metadades) {
		DocumentArxiu document = new DocumentArxiu();
		document.id = id;
		document.nom = nom;
		for (int i = 0; i < metadades.length; i += 2) {
			document.metadades.put((String) metadades[i], metadades[i + 1]);
		}
		return document;
	}

	private static DocumentArxiu documentComplet() {
		return document(UUID, "Informe trimestral.pdf",
				"eni:fecha_inicio", "2020-03-31T13:48:00.000+02:00",
				"eni:organo", "A04003003",
				"eni:origen", 1,
				"eni:csv", CSV,
				"eni:id", "ES_A04003003_2020_ABC");
	}

	// --- Per UUID ----------------------------------------------------------------------------

	@Test
	public void perUuidLlegeixLesMetadadesDeLArxiuSenseContingut() throws Exception {
		doReturn(documentComplet()).when(helper).obteArxiu(UUID);

		Metadades m = helper.resol(UUID);

		assertEquals(DocumentExclosEstatEnum.OK, m.estat);
		assertEquals("Informe_trimestral.pdf", m.nomDocument);
		assertEquals(ZonedDateTime.of(2020, 3, 31, 13, 48, 0, 0, java.time.ZoneOffset.ofHours(2)).
				withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(), m.dataCaptura);
		assertEquals(CSV, m.csv);
		assertEquals(UUID, m.uuid);
		assertEquals("A04003003", m.organ);
		assertEquals(DocumentExclosOrigenEnum.ADMINISTRACIO, m.origen);
		assertEquals(DocumentExclosUbicacioEnum.ARXIU, m.ubicacio);
		verify(helper, never()).cercaArxiuPerCsv(anyString());
		verify(helper, never()).xmlCustodia(anyString());
	}

	@Test
	public void perUuidElCsvDelsDocumentsMigratsEsLaMetadadaGdibHash() throws Exception {
		doReturn(document(UUID, "a.pdf", "gdib:hash", "migrat", "eni:csv", CSV)).when(helper).obteArxiu(UUID);

		assertEquals("migrat", helper.resol(UUID).csv);
	}

	@Test
	public void perUuidInexistentEsNoTrobatISenseCustodia() throws Exception {
		doReturn(null).when(helper).obteArxiu(UUID);

		Metadades m = helper.resol(UUID);

		assertEquals(DocumentExclosEstatEnum.NO_TROBAT, m.estat);
		verify(helper, never()).xmlCustodia(anyString());
	}

	@Test
	public void perUuidAmbLArxiuDesactivatEsError() {
		activa(false, true);

		Metadades m = helper.resol(UUID);

		assertEquals(DocumentExclosEstatEnum.ERROR, m.estat);
		assertNotNull(m.missatge);
	}

	// --- Per CSV -----------------------------------------------------------------------------

	@Test
	public void perCsvCercaLUuidIDespresLlegeixLesMetadades() throws Exception {
		doReturn(List.of(document(UUID, "x.pdf", "eni:id", "ID1"))).when(helper).cercaArxiuPerCsv(CSV);
		doReturn(document(UUID, "Informe.pdf", "eni:csv", "un-altre", "eni:organo", "O")).when(helper).obteArxiu(UUID);

		Metadades m = helper.resol(CSV);

		assertEquals(DocumentExclosEstatEnum.OK, m.estat);
		assertEquals(UUID, m.uuid);
		// El CSV de la cerca és el de la fila, encara que la metadada digui una altra cosa.
		assertEquals(CSV, m.csv);
		assertEquals("Informe.pdf", m.nomDocument);
		verify(helper, never()).xmlCustodia(anyString());
	}

	@Test
	public void perCsvAmbMesDUnDocumentDiferentEsError() throws Exception {
		doReturn(List.of(document("u1", "a.pdf", "eni:id", "ID1"), document("u2", "b.pdf", "eni:id", "ID2"))).
				when(helper).cercaArxiuPerCsv(CSV);

		Metadades m = helper.resol(CSV);

		assertEquals(DocumentExclosEstatEnum.ERROR, m.estat);
		verify(helper, never()).obteArxiu(anyString());
	}

	@Test
	public void perCsvAmbElMateixDocumentRepetitNoEsDuplicat() throws Exception {
		doReturn(List.of(document(UUID, "a.pdf", "eni:id", "ID1"), document(UUID, "a.pdf", "eni:id", "ID1"))).
				when(helper).cercaArxiuPerCsv(CSV);
		doReturn(documentComplet()).when(helper).obteArxiu(UUID);

		assertEquals(DocumentExclosEstatEnum.OK, helper.resol(CSV).estat);
	}

	@Test
	public void perCsvQueLArxiuNoTeEsProvaLaCustodiaAntiga() throws Exception {
		doReturn(List.of()).when(helper).cercaArxiuPerCsv(CSV);
		doReturn(XML_CUSTODIA).when(helper).xmlCustodia(CSV);

		Metadades m = helper.resol(CSV);

		assertEquals(DocumentExclosEstatEnum.OK, m.estat);
		assertEquals(DocumentExclosUbicacioEnum.CUSTODIA_ANTIGA, m.ubicacio);
		assertEquals("Informe_antic.pdf", m.nomDocument);
		assertEquals(LocalDateTime.of(2020, 3, 31, 13, 48), m.dataCaptura);
		assertEquals(CSV, m.csv);
		assertNull(m.uuid);
	}

	@Test
	public void perCsvQueNoEsATotArreuEsNoTrobatAmbElMotiuDeLesDues() throws Exception {
		doReturn(List.of()).when(helper).cercaArxiuPerCsv(CSV);
		doThrow(new Exception("... es.caib.signatura.error.ReservaNotFoundException ...")).when(helper).xmlCustodia(CSV);

		Metadades m = helper.resol(CSV);

		assertEquals(DocumentExclosEstatEnum.NO_TROBAT, m.estat);
		assertTrue(m.missatge.contains("Arxiu"));
		assertTrue(m.missatge.contains("custòdia"));
	}

	@Test
	public void perCsvUnErrorDeLaCustodiaEsError() throws Exception {
		doReturn(List.of()).when(helper).cercaArxiuPerCsv(CSV);
		doThrow(new Exception("connexió refusada")).when(helper).xmlCustodia(CSV);

		Metadades m = helper.resol(CSV);

		assertEquals(DocumentExclosEstatEnum.ERROR, m.estat);
		assertTrue(m.missatge.contains("connexió refusada"));
	}

	@Test
	public void perCsvAmbCustodiaNoCorrectaEsNoTrobat() throws Exception {
		doReturn(List.of()).when(helper).cercaArxiuPerCsv(CSV);
		doReturn(XML_CUSTODIA.replace(">S<", ">N<")).when(helper).xmlCustodia(CSV);

		assertEquals(DocumentExclosEstatEnum.NO_TROBAT, helper.resol(CSV).estat);
	}

	@Test
	public void unCodiQueNoTeElFormatDeLArxiuNomesEsProvaALaCustodia() throws Exception {
		doReturn(XML_CUSTODIA).when(helper).xmlCustodia("codi-antic");

		Metadades m = helper.resol("codi-antic");

		assertEquals(DocumentExclosEstatEnum.OK, m.estat);
		verify(helper, never()).cercaArxiuPerCsv(anyString());
	}

	@Test
	public void ambLaCustodiaDesactivadaUnCsvQueLArxiuNoTeEsNoTrobat() throws Exception {
		activa(true, false);
		doReturn(List.of()).when(helper).cercaArxiuPerCsv(CSV);

		Metadades m = helper.resol(CSV);

		assertEquals(DocumentExclosEstatEnum.NO_TROBAT, m.estat);
		verify(helper, never()).xmlCustodia(anyString());
	}

	@Test
	public void ambLArxiuDesactivatNomesEsConsultaLaCustodia() throws Exception {
		activa(false, true);
		doReturn(XML_CUSTODIA).when(helper).xmlCustodia(CSV);

		assertEquals(DocumentExclosUbicacioEnum.CUSTODIA_ANTIGA, helper.resol(CSV).ubicacio);
		verify(helper, never()).cercaArxiuPerCsv(anyString());
	}

	@Test
	public void ambCapIntegracioActivaEsError() {
		activa(false, false);

		assertEquals(DocumentExclosEstatEnum.ERROR, helper.resol(CSV).estat);
	}

	// --- Errors i límits ---------------------------------------------------------------------

	@Test
	public void unaExcepcioDeLArxiuNoEsPropagaIEsError() throws Exception {
		doThrow(new IllegalStateException("COD_500 - error intern")).when(helper).obteArxiu(UUID);

		Metadades m = helper.resol(UUID);

		assertEquals(DocumentExclosEstatEnum.ERROR, m.estat);
		assertTrue(m.missatge.contains("COD_500"));
	}

	@Test
	public void elMissatgeDeLErrorNoDuLaContrasenyaDeLArxiu() throws Exception {
		when(configHelper.getEnvironmentProperty(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_PASSWORD, null)).thenReturn("secret123");
		doThrow(new IllegalStateException("autenticació fallida amb secret123")).when(helper).obteArxiu(UUID);

		Metadades m = helper.resol(UUID);

		assertTrue(m.missatge.contains("***"));
		assertTrue(!m.missatge.contains("secret123"));
	}

	@Test
	public void siElServeiNoResponEnElTempsEsError() throws Exception {
		helper.timeoutMs = 100;
		doAnswerLent(UUID);

		Metadades m = helper.resol(UUID);

		assertEquals(DocumentExclosEstatEnum.ERROR, m.estat);
		assertTrue(m.missatge.contains("no ha respost"));
	}

	private void doAnswerLent(String uuid) throws Exception {
		org.mockito.Mockito.doAnswer(invocation -> {
			Thread.sleep(5000);
			return null;
		}).when(helper).obteArxiu(uuid);
	}

	@Test
	public void unValorBuitEsError() {
		assertEquals(DocumentExclosEstatEnum.ERROR, helper.resol("  ").estat);
		assertEquals(DocumentExclosEstatEnum.ERROR, helper.resol(null).estat);
	}

	@Test
	public void elMissatgeQueNoCabEnLaColumnaEsRetalla() {
		String llarg = "x".repeat(3000);

		assertEquals(1000, DocumentExclosMetadadesHelper.retalla(llarg).length());
		assertEquals("curt", DocumentExclosMetadadesHelper.retalla("curt"));
	}

	// --- Dates i text ------------------------------------------------------------------------

	@Test
	public void dataAdmetUnaDateIUnTextISO() {
		LocalDateTime esperada = LocalDateTime.of(2021, 5, 4, 9, 30);
		Date date = Date.from(esperada.atZone(ZoneId.systemDefault()).toInstant());

		assertEquals(esperada, DocumentExclosMetadadesHelper.data(date));
		assertEquals(esperada, DocumentExclosMetadadesHelper.data(
				esperada.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()));
	}

	@Test
	public void unaDataIlLegibleNoInvalidaLesDades() {
		assertNull(DocumentExclosMetadadesHelper.data("no és una data"));
		assertNull(DocumentExclosMetadadesHelper.data(null));
		assertNull(DocumentExclosMetadadesHelper.data("  "));
	}

	@Test
	public void textConvertEntersILlistesIDescartaElsBuits() {
		assertEquals("7", DocumentExclosMetadadesHelper.text(7));
		assertEquals("a, b", DocumentExclosMetadadesHelper.text(List.of("a", "b")));
		assertNull(DocumentExclosMetadadesHelper.text("  "));
		assertNull(DocumentExclosMetadadesHelper.text(null));
	}

}
