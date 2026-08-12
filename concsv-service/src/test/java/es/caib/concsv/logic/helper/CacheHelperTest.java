package es.caib.concsv.logic.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import es.caib.concsv.logic.intf.enums.DocumentLocation;
import es.caib.concsv.logic.intf.model.DocumentContent;
import es.caib.concsv.logic.intf.model.DocumentInfo;

/**
 * Tests de la cache de documents en sistema de fitxers.
 *
 * <p>Es comprova sobretot que cap error llegint una entrada de la cache no arriba al
 * consultor: l'entrada s'elimina i es retorna empty perquè el document es torni a
 * obtenir de l'origen.</p>
 */
public class CacheHelperTest {

	private static final String UUID = "8eaef09d-5dbf-416a-86cf-211c5bbe6b4f";
	private static final String CSV = "b4e15fbae8e26dadba7e2cfd480e33ea89e83bf3cf737ea5d8223bfa8b627178";

	private Path fitxersPath;
	private CacheHelper cacheHelper;

	@Before
	public void setUp() throws IOException {
		fitxersPath = Files.createTempDirectory("concsv-cache-test");
		cacheHelper = new CacheHelper();
		cacheHelper.setFitxersPath(fitxersPath.toString());
		// 0 = sense expiració, els tests no depenen del temps
		cacheHelper.setCacheTtlMinuts(0);
	}

	@After
	public void tearDown() throws IOException {
		if (Files.exists(fitxersPath)) {
			try (Stream<Path> paths = Files.walk(fitxersPath)) {
				List<Path> ordenats = paths
					.sorted(Comparator.reverseOrder())
					.collect(Collectors.toList());
				for (Path path: ordenats) {
					Files.deleteIfExists(path);
				}
			}
		}
	}

	@Test
	public void testGetInfoRetornaElDocumentDesat() throws IOException {
		cacheHelper.setInfo(UUID, true, documentInfoDeProva());

		Optional<DocumentInfo> resultat = cacheHelper.getInfo(UUID, true);

		assertTrue("Hauria de recuperar el document desat a la cache", resultat.isPresent());
		assertEquals(CSV, resultat.get().getHash());
		assertEquals(UUID, resultat.get().getDocumentCode());
	}

	@Test
	public void testGetInfoRetornaBuitSiNoHiHaEntrada() {
		assertFalse(cacheHelper.getInfo(UUID, true).isPresent());
	}

	/**
	 * Simula el cas real: la classe DocumentInfo ha canviat des que es va desar l'entrada, de manera
	 * que el serialVersionUID de l'stream no coincideix amb el de la classe local i la deserialització
	 * llança InvalidClassException. La consulta no ha de fallar i el fitxer s'ha d'eliminar.
	 */
	@Test
	public void testGetInfoRetornaBuitISEliminaElFitxerSiLaClasseHaCanviat() throws Exception {
		cacheHelper.setInfo(UUID, true, documentInfoDeProva());
		Path fitxer = unicFitxerDeLaCache();
		modificarSerialVersionUid(fitxer, DocumentInfo.class);

		Optional<DocumentInfo> resultat = cacheHelper.getInfo(UUID, true);

		assertFalse("Una entrada incompatible s'ha de tractar com si no hi fos", resultat.isPresent());
		assertFalse("L'entrada incompatible s'ha d'haver eliminat", Files.exists(fitxer));
	}

	/** Després d'eliminar l'entrada incompatible s'hi ha de poder desar la nova al seu lloc. */
	@Test
	public void testSetInfoSobreUnaEntradaIncompatible() throws Exception {
		cacheHelper.setInfo(UUID, true, documentInfoDeProva());
		Path fitxer = unicFitxerDeLaCache();
		modificarSerialVersionUid(fitxer, DocumentInfo.class);
		cacheHelper.getInfo(UUID, true);

		cacheHelper.setInfo(UUID, true, documentInfoDeProva());

		Optional<DocumentInfo> resultat = cacheHelper.getInfo(UUID, true);
		assertTrue(resultat.isPresent());
		assertEquals(CSV, resultat.get().getHash());
	}

	@Test
	public void testGetInfoRetornaBuitISEliminaElFitxerSiEstaCorrupte() throws IOException {
		cacheHelper.setInfo(CSV, false, documentInfoDeProva());
		Path fitxer = unicFitxerDeLaCache();
		Files.write(fitxer, "això no és un objecte serialitzat".getBytes(StandardCharsets.UTF_8));

		Optional<DocumentInfo> resultat = cacheHelper.getInfo(CSV, false);

		assertFalse(resultat.isPresent());
		assertFalse("L'entrada corrupta s'ha d'haver eliminat", Files.exists(fitxer));
	}

	@Test
	public void testGetContentRetornaBuitISEliminaElFitxerSiEstaCorrupte() throws IOException {
		DocumentContent documentContent = new DocumentContent();
		documentContent.setContent(new byte[] { 1, 2, 3 });
		documentContent.setMimeType("application/pdf");
		cacheHelper.setContent(CSV, CacheHelper.CacheType.ORIGINAL, null, documentContent);
		Path fitxer = unicFitxerDeLaCache();
		Files.write(fitxer, new byte[] { 0, 0, 0, 0 });

		Optional<DocumentContent> resultat = cacheHelper.getContent(CSV, CacheHelper.CacheType.ORIGINAL, null);

		assertFalse(resultat.isPresent());
		assertFalse("L'entrada corrupta s'ha d'haver eliminat", Files.exists(fitxer));
	}

	/** Una entrada nul·la (document no trobat) no ha de provocar cap error. */
	@Test
	public void testGetInfoRetornaBuitSiSHaDesatUnValorNul() throws IOException {
		cacheHelper.setInfo(CSV, false, null);

		assertFalse(cacheHelper.getInfo(CSV, false).isPresent());
	}

	private DocumentInfo documentInfoDeProva() {
		DocumentInfo documentInfo = new DocumentInfo();
		documentInfo.setDocumentLocation(DocumentLocation.NewDigitalArchive);
		documentInfo.setDocumentCode(UUID);
		documentInfo.setHash(CSV);
		documentInfo.setDocumentName("document.pdf");
		documentInfo.setExtensionFormato("pdf");
		return documentInfo;
	}

	/** Cerca l'únic fitxer que hi ha d'haver a la carpeta de cache. */
	private Path unicFitxerDeLaCache() throws IOException {
		List<Path> fitxers;
		try (Stream<Path> paths = Files.walk(fitxersPath)) {
			fitxers = paths
				.filter(Files::isRegularFile)
				.collect(Collectors.toList());
		}
		assertEquals("S'esperava un únic fitxer a la cache", 1, fitxers.size());
		return fitxers.get(0);
	}

	/**
	 * Modifica el serialVersionUID que hi ha dins l'stream serialitzat del fitxer perquè no
	 * coincideixi amb el de la classe local, tal com passa quan la classe canvia entre desplegaments.
	 */
	private void modificarSerialVersionUid(Path fitxer, Class<? extends Serializable> classe) throws IOException {
		byte[] contingut = Files.readAllBytes(fitxer);
		long serialVersionUid = ObjectStreamClass.lookup(classe).getSerialVersionUID();
		byte[] serialVersionUidBytes = ByteBuffer.allocate(Long.BYTES).putLong(serialVersionUid).array();
		int posicio = indexOf(contingut, serialVersionUidBytes);
		assertTrue(
			"No s'ha trobat el serialVersionUID de " + classe.getSimpleName() + " dins l'stream",
			posicio >= 0);
		contingut[posicio] = (byte) (contingut[posicio] ^ 0xFF);
		Files.write(fitxer, contingut);
	}

	private int indexOf(byte[] contingut, byte[] cerca) {
		for (int i = 0; i <= contingut.length - cerca.length; i++) {
			boolean trobat = true;
			for (int j = 0; j < cerca.length; j++) {
				if (contingut[i + j] != cerca[j]) {
					trobat = false;
					break;
				}
			}
			if (trobat) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Comprova que la utilitat de test reprodueix realment l'error de producció
	 * ("local class incompatible") i no un error qualsevol de lectura.
	 */
	@Test
	public void testLaModificacioDelSerialVersionUidProvocaInvalidClassException() throws Exception {
		cacheHelper.setInfo(UUID, true, documentInfoDeProva());
		Path fitxer = unicFitxerDeLaCache();
		modificarSerialVersionUid(fitxer, DocumentInfo.class);

		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(fitxer))) {
			ois.readObject();
			fail("S'esperava una InvalidClassException");
		} catch (InvalidClassException ex) {
			assertTrue(
				"Missatge inesperat: " + ex.getMessage(),
				ex.getMessage().contains("local class incompatible"));
		}
	}

}
