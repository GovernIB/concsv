package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.CacheCarpeta;
import es.caib.concsv.logic.intf.model.ConfigResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests de {@link CacheEsborratHelper}: recompte i esborrat del contingut de les carpetes de la
 * cache de documents.
 */
public class CacheEsborratHelperTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private ConfigHelper configHelper;
	private CacheEsborratHelper helper;
	private Path fitxers;

	@Before
	public void setUp() throws IOException {
		fitxers = temp.newFolder("fitxers").toPath();
		configHelper = Mockito.mock(ConfigHelper.class);
		Mockito.when(configHelper.getConfig(PropertyConfig.PROP_FITXERS_PATH)).thenReturn(fitxers.toString());
		helper = new CacheEsborratHelper(configHelper);
	}

	private Path fitxer(String carpeta, String nom) throws IOException {
		Path dir = fitxers.resolve("cache").resolve(carpeta);
		Files.createDirectories(dir);
		return Files.write(dir.resolve(nom), new byte[] { 1, 2, 3 });
	}

	@Test
	public void comptarCompteFitxersDeLaCarpeta() throws IOException {
		fitxer("INFO", "CSV_a");
		fitxer("INFO", "UUID_b");
		fitxer("ORIGINAL", "a");

		assertEquals(2, helper.comptar(CacheCarpeta.INFO));
		assertEquals(1, helper.comptar(CacheCarpeta.ORIGINAL));
		assertEquals(0, helper.comptar(CacheCarpeta.IMPRIMIBLE));
	}

	@Test
	public void resumRetornaElRecompteDeCadaCarpeta() throws IOException {
		fitxer("INFO", "CSV_a");
		fitxer("IMPRIMIBLE", "a_CA");
		fitxer("IMPRIMIBLE", "a_ES");

		ConfigResource.ResumCache resum = helper.resum();

		assertEquals(1, resum.getInfo());
		assertEquals(0, resum.getOriginal());
		assertEquals(2, resum.getImprimible());
		assertEquals(fitxers.resolve("cache").toString(), resum.getRuta());
	}

	@Test
	public void esborrarBuidaNomesLaCarpetaDemanada() throws IOException {
		fitxer("INFO", "CSV_a");
		fitxer("INFO", "UUID_b");
		Path original = fitxer("ORIGINAL", "a");

		ConfigResource.ResultatEsborratCache resultat = helper.esborrar(CacheCarpeta.INFO);

		assertEquals(2, resultat.getEsborrats());
		assertEquals(0, resultat.getErrors());
		assertEquals(0, helper.comptar(CacheCarpeta.INFO));
		assertTrue(Files.exists(original));
		// La carpeta es manté
		assertTrue(Files.isDirectory(fitxers.resolve("cache").resolve("INFO")));
	}

	@Test
	public void esborrarTotesBuidaLesTresCarpetes() throws IOException {
		fitxer("INFO", "CSV_a");
		fitxer("ORIGINAL", "a");
		fitxer("IMPRIMIBLE", "a_CA");
		fitxer("IMPRIMIBLE", "a_ES");

		ConfigResource.ResultatEsborratCache resultat = helper.esborrarTotes();

		assertEquals(4, resultat.getEsborrats());
		assertEquals(0, resultat.getErrors());
		ConfigResource.ResumCache resum = helper.resum();
		assertEquals(0, resum.getInfo() + resum.getOriginal() + resum.getImprimible());
	}

	@Test
	public void esborrarCarpetaInexistentRetornaZero() {
		ConfigResource.ResultatEsborratCache resultat = helper.esborrar(CacheCarpeta.ORIGINAL);

		assertEquals(0, resultat.getEsborrats());
		assertEquals(0, resultat.getErrors());
	}

	@Test
	public void esborrarNoTocaFitxersForaDeLaCache() throws IOException {
		fitxer("INFO", "CSV_a");
		Path fora = Files.write(fitxers.resolve("altre.txt"), new byte[] { 1 });

		helper.esborrarTotes();

		assertTrue(Files.exists(fora));
	}

	@Test
	public void unFitxerQueFallaEsComptaIElsAltresSEsborren() throws IOException {
		// A Windows un fitxer obert no es pot esborrar; a Linux, un de només lectura sí. Per això
		// el cas d'error es provoca amb un fitxer bloquejat per un flux obert, i només si el sistema
		// ho impedeix realment.
		Path bloquejat = fitxer("INFO", "CSV_bloquejat");
		fitxer("INFO", "CSV_lliure");

		try (java.io.RandomAccessFile obert = new java.io.RandomAccessFile(new File(bloquejat.toString()), "rw")) {
			boolean elSistemaElBloqueja = !bloquejat.toFile().renameTo(new File(bloquejat + ".prova"));
			if (!elSistemaElBloqueja) {
				// No es bloqueja (Linux/macOS): el rename ha funcionat, es desfà i es dona el cas per vàlid
				new File(bloquejat + ".prova").renameTo(bloquejat.toFile());
				return;
			}
			ConfigResource.ResultatEsborratCache resultat = helper.esborrar(CacheCarpeta.INFO);

			assertEquals(1, resultat.getEsborrats());
			assertEquals(1, resultat.getErrors());
			assertTrue(Files.exists(bloquejat));
			assertTrue(obert.getChannel().isOpen());
		}
	}

	@Test(expected = IllegalStateException.class)
	public void sensePropietatFitxersLlancaExcepcio() {
		Mockito.when(configHelper.getConfig(PropertyConfig.PROP_FITXERS_PATH)).thenReturn(null);

		helper.comptar(CacheCarpeta.INFO);
	}

}
