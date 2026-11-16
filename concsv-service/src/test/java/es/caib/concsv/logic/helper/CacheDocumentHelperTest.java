package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.CacheCarpeta;
import es.caib.concsv.logic.intf.model.CacheDocumentResource;
import es.caib.concsv.logic.intf.model.DocumentContent;
import es.caib.concsv.logic.intf.model.DocumentInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests de {@link CacheDocumentHelper}: llistat i esborrat de les entrades de la cache de documents.
 */
public class CacheDocumentHelperTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private ConfigHelper configHelper;
	private CacheDocumentHelper helper;
	private Path fitxers;

	@Before
	public void setUp() throws IOException {
		fitxers = temp.newFolder("fitxers").toPath();
		configHelper = Mockito.mock(ConfigHelper.class);
		Mockito.when(configHelper.getConfig(PropertyConfig.PROP_FITXERS_PATH)).thenReturn(fitxers.toString());
		helper = new CacheDocumentHelper(configHelper);
	}

	private Path fitxer(String carpeta, String nom) throws IOException {
		Path dir = fitxers.resolve("cache").resolve(carpeta);
		Files.createDirectories(dir);
		return Files.write(dir.resolve(nom), new byte[] { 1, 2, 3 });
	}

	/** Fitxer amb un objecte Java serialitzat (com fa {@code CacheHelper}). */
	private Path fitxerSerialitzat(String carpeta, String nom, Serializable objecte) throws IOException {
		Path dir = fitxers.resolve("cache").resolve(carpeta);
		Files.createDirectories(dir);
		Path fitxer = dir.resolve(nom);
		try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(fitxer))) {
			oos.writeObject(objecte);
		}
		return fitxer;
	}

	private CacheDocumentHelper.Entrada entrada(CacheCarpeta carpeta, String nom) {
		return helper.llistar().stream().
				filter(e -> e.getCarpeta() == carpeta && e.getNomFitxer().equals(nom)).
				findFirst().orElseThrow(AssertionError::new);
	}

	@Test
	public void llistarRetornaLesEntradesDeLesTresCarpetes() throws IOException {
		fitxer("INFO", "CSV_a");
		fitxer("INFO", "UUID_b");
		fitxer("ORIGINAL", "a");
		fitxer("IMPRIMIBLE", "a_CA");

		assertEquals(4, helper.llistar().size());
	}

	@Test
	public void llistarSenseLaCacheRetornaBuit() {
		assertTrue(helper.llistar().isEmpty());
	}

	@Test
	public void llistarNoInclouSubcarpetesNiFitxersForaDeLesCarpetes() throws IOException {
		fitxer("INFO", "CSV_a");
		Files.createDirectories(fitxers.resolve("cache").resolve("INFO").resolve("subcarpeta"));
		Files.write(fitxers.resolve("altre.txt"), new byte[] { 1 });

		assertEquals(1, helper.llistar().size());
	}

	@Test
	public void laDataDeDarreraConsultaEsLaDelFitxerILExpiracioHiSumaElTtl() throws IOException {
		Path info = fitxer("INFO", "CSV_a");
		Files.setLastModifiedTime(info, FileTime.fromMillis(System.currentTimeMillis() - 10 * 60_000L));
		Mockito.when(configHelper.getConfig(PropertyConfig.PROP_CACHE_TTL_MINUTS)).thenReturn("45");

		CacheDocumentHelper.Entrada entrada = entrada(CacheCarpeta.INFO, "CSV_a");

		assertEquals(Files.getLastModifiedTime(info).toMillis(), entrada.getDarreraConsulta());
		assertEquals(Long.valueOf(entrada.getDarreraConsulta() + 45 * 60_000L), entrada.getExpiracio());
		assertEquals(3, entrada.getMida());
	}

	@Test
	public void elTtlPerDefecteSonTrentaMinuts() throws IOException {
		fitxer("INFO", "CSV_a");

		CacheDocumentHelper.Entrada entrada = entrada(CacheCarpeta.INFO, "CSV_a");

		assertEquals(Long.valueOf(entrada.getDarreraConsulta() + 30 * 60_000L), entrada.getExpiracio());
	}

	@Test
	public void ambTtlZeroNoHiHaExpiracio() throws IOException {
		fitxer("INFO", "CSV_a");
		Mockito.when(configHelper.getConfig(PropertyConfig.PROP_CACHE_TTL_MINUTS)).thenReturn("0");

		assertNull(entrada(CacheCarpeta.INFO, "CSV_a").getExpiracio());
		assertNull(entrada(CacheCarpeta.INFO, "CSV_a").toResource().getExpiracio());
	}

	@Test
	public void unTtlNoNumericUsaElPerDefecte() throws IOException {
		fitxer("INFO", "CSV_a");
		Mockito.when(configHelper.getConfig(PropertyConfig.PROP_CACHE_TTL_MINUTS)).thenReturn("abc");

		CacheDocumentHelper.Entrada entrada = entrada(CacheCarpeta.INFO, "CSV_a");

		assertEquals(Long.valueOf(entrada.getDarreraConsulta() + 30 * 60_000L), entrada.getExpiracio());
	}

	@Test
	public void infoDistingeUuidICsv() throws IOException {
		fitxer("INFO", "UUID_2s4f-sd54");
		fitxer("INFO", "CSV_abc123");

		CacheDocumentResource uuid = entrada(CacheCarpeta.INFO, "UUID_2s4f-sd54").toResource();
		CacheDocumentResource csv = entrada(CacheCarpeta.INFO, "CSV_abc123").toResource();

		assertEquals("UUID", uuid.getTipusIdentificador());
		assertEquals("2s4f-sd54", uuid.getIdentificador());
		assertEquals("CSV", csv.getTipusIdentificador());
		assertEquals("abc123", csv.getIdentificador());
		assertEquals("INFO:CSV_abc123", csv.getId());
		assertNull(csv.getIdioma());
	}

	@Test
	public void imprimibleTreuLIdiomaDelNom() throws IOException {
		fitxer("IMPRIMIBLE", "2s4f-sd54_CA");

		CacheDocumentResource resource = entrada(CacheCarpeta.IMPRIMIBLE, "2s4f-sd54_CA").toResource();

		assertEquals("2s4f-sd54", resource.getIdentificador());
		assertEquals("CA", resource.getIdioma());
		assertNull(resource.getTipusIdentificador());
	}

	@Test
	public void imprimibleSenseIdiomaMantElIdentificadorSencer() throws IOException {
		fitxer("IMPRIMIBLE", "abc-def");

		CacheDocumentResource resource = entrada(CacheCarpeta.IMPRIMIBLE, "abc-def").toResource();

		assertEquals("abc-def", resource.getIdentificador());
		assertNull(resource.getIdioma());
	}

	@Test
	public void originalUsaElNomDelFitxerComIdentificador() throws IOException {
		fitxer("ORIGINAL", "abc_def");

		CacheDocumentResource resource = entrada(CacheCarpeta.ORIGINAL, "abc_def").toResource();

		assertEquals("abc_def", resource.getIdentificador());
		assertNull(resource.getIdioma());
		assertNull(resource.getTipusIdentificador());
	}

	@Test
	public void infoLlegeixElNomDelDocumentDeLObjecteSerialitzat() throws IOException {
		DocumentInfo documentInfo = new DocumentInfo();
		documentInfo.setDocumentName("Informe trimestral.pdf");
		fitxerSerialitzat("INFO", "CSV_abc123", documentInfo);

		CacheDocumentResource resource = entrada(CacheCarpeta.INFO, "CSV_abc123").toResource();

		assertEquals("Informe trimestral.pdf", resource.getNomDocument());
	}

	@Test
	public void infoAmbUnFitxerNoSerialitzatNoFallaINomDocumentEsNull() throws IOException {
		// fitxer() escriu bytes { 1, 2, 3 }, que no és cap objecte Java serialitzat.
		fitxer("INFO", "CSV_a");

		CacheDocumentResource resource = entrada(CacheCarpeta.INFO, "CSV_a").toResource();

		assertNull(resource.getNomDocument());
	}

	@Test
	public void originalIImprimibleMaiNoDeserialitzenEncaraQueElFitxerSiguiUnDocumentContentValid() throws IOException {
		// DocumentContent inclou el contingut binari del document: deserialitzar-lo només per obtenir
		// el nom seria massa costós amb molts fitxers, així que a ORIGINAL i IMPRIMIBLE no s'intenta.
		DocumentContent documentContent = new DocumentContent();
		documentContent.setFileName("Informe trimestral.pdf");
		fitxerSerialitzat("ORIGINAL", "abc123", documentContent);
		fitxerSerialitzat("IMPRIMIBLE", "abc123_CA", documentContent);

		assertNull(entrada(CacheCarpeta.ORIGINAL, "abc123").toResource().getNomDocument());
		assertNull(entrada(CacheCarpeta.IMPRIMIBLE, "abc123_CA").toResource().getNomDocument());
	}

	@Test
	public void trobarRetornaLEntradaIEmptySiNoExisteix() throws IOException {
		fitxer("INFO", "CSV_a");

		assertTrue(helper.trobar("INFO:CSV_a").isPresent());
		assertFalse(helper.trobar("INFO:CSV_inexistent").isPresent());
	}

	@Test
	public void trobarIgnoraIdentificadorsNoValids() throws IOException {
		fitxer("INFO", "CSV_a");
		Files.write(fitxers.resolve("secret.txt"), new byte[] { 1 });

		for (String id : Arrays.asList(null, "", "CSV_a", "INFO", "INFO:", "XXX:CSV_a", ":CSV_a", "INFO:..",
				"INFO:../../secret.txt", "INFO:a/b", "INFO:a\\b", "../INFO:CSV_a")) {
			assertEquals(id, Optional.empty(), helper.trobar(id));
		}
	}

	@Test
	public void esborrarNomesEsborraLesEntradesIndicades() throws IOException {
		Path a = fitxer("INFO", "CSV_a");
		Path b = fitxer("INFO", "UUID_b");
		Path original = fitxer("ORIGINAL", "a");

		CacheDocumentResource.ResultatEsborrat resultat = helper.esborrar(Arrays.asList("INFO:CSV_a", "ORIGINAL:a"));

		assertEquals(2, resultat.getEsborrats());
		assertEquals(0, resultat.getErrors());
		assertFalse(Files.exists(a));
		assertFalse(Files.exists(original));
		assertTrue(Files.exists(b));
		// Les carpetes es mantenen
		assertTrue(Files.isDirectory(fitxers.resolve("cache").resolve("ORIGINAL")));
	}

	@Test
	public void trobaIEsborraFitxersAmbNomsQueCacheHelperMaiNoEscriuria() throws IOException {
		// Fitxers que no genera CacheHelper (p. ex. copiats a mà per fer proves), amb espais, accents
		// i símbols al nom: l'esborrat no s'ha de limitar al joc de caràcters que fa servir
		// CacheHelper per als CSV/UUID, només ha de protegir que no se surti de la carpeta.
		Path esborrat = fitxer("ORIGINAL", "Exemple ~¡!'ç,.)=.pdf");
		Path mantingut = fitxer("ORIGINAL", "Un altre document.pdf");

		assertTrue(helper.trobar("ORIGINAL:Exemple ~¡!'ç,.)=.pdf").isPresent());

		CacheDocumentResource.ResultatEsborrat resultat =
				helper.esborrar(Collections.singletonList("ORIGINAL:Exemple ~¡!'ç,.)=.pdf"));

		assertEquals(1, resultat.getEsborrats());
		assertEquals(0, resultat.getErrors());
		assertFalse(Files.exists(esborrat));
		assertTrue(Files.exists(mantingut));
	}

	@Test
	public void esborrarUnaEntradaInexistentNoEsCapErrorNiEsComptaComEsborrada() throws IOException {
		fitxer("INFO", "CSV_a");

		CacheDocumentResource.ResultatEsborrat resultat = helper.esborrar(Arrays.asList("INFO:CSV_inexistent", "INFO:CSV_inexistent"));

		assertEquals(0, resultat.getEsborrats());
		assertEquals(0, resultat.getErrors());
	}

	@Test
	public void esborrarComptaComErrorsElsIdentificadorsNoValidsSenseTocarRes() throws IOException {
		Path info = fitxer("INFO", "CSV_a");
		Path fora = Files.write(fitxers.resolve("secret.txt"), new byte[] { 1 });
		Path foraCarpeta = Files.write(fitxers.resolve("cache").resolve("secret2.txt"), new byte[] { 1 });

		CacheDocumentResource.ResultatEsborrat resultat = helper.esborrar(
				Arrays.asList("INFO:../../secret.txt", "INFO:../secret2.txt", "INFO:..", "INFO:", "XXX:a", "INFO:a/b"));

		assertEquals(0, resultat.getEsborrats());
		assertEquals(6, resultat.getErrors());
		assertTrue(Files.exists(info));
		assertTrue(Files.exists(fora));
		assertTrue(Files.exists(foraCarpeta));
	}

	@Test
	public void esborrarSenseIdentificadorsNoFaRes() {
		assertEquals(0, helper.esborrar(null).getEsborrats());
		assertEquals(0, helper.esborrar(Collections.emptyList()).getEsborrats());
	}

	@Test
	public void unaEntradaQueFallaEsComptaIElsAltresSEsborren() throws IOException {
		// Per provocar l'error amb independència del sistema operatiu: un directori no buit amb el
		// nom d'una entrada. No es llista (no és un fitxer regular), però en esborrar-lo per
		// identificador falla perquè no és buit.
		Path dir = fitxers.resolve("cache").resolve("INFO").resolve("CSV_directori");
		Files.createDirectories(dir);
		Files.write(dir.resolve("dins"), new byte[] { 1 });
		fitxer("INFO", "CSV_lliure");

		CacheDocumentResource.ResultatEsborrat resultat = helper.esborrar(Arrays.asList("INFO:CSV_directori", "INFO:CSV_lliure"));

		assertEquals(1, resultat.getEsborrats());
		assertEquals(1, resultat.getErrors());
		assertTrue(Files.exists(dir));
	}

	@Test(expected = IllegalStateException.class)
	public void senseLaPropietatFitxersFalla() {
		Mockito.when(configHelper.getConfig(PropertyConfig.PROP_FITXERS_PATH)).thenReturn(null);

		helper.llistar();
	}

}
