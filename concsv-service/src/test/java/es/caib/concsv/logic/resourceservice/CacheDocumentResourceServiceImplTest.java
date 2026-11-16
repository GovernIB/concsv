package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.helper.CacheDocumentHelper;
import es.caib.concsv.logic.helper.ConfigHelper;
import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.CacheCarpeta;
import es.caib.concsv.logic.intf.model.CacheDocumentResource;
import es.caib.concsv.persist.base.entity.NoDatabaseResourceEntity;
import es.caib.concsv.logic.base.service.BaseReadonlyResourceService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests de {@link CacheDocumentResourceServiceImpl} i del filtre en memòria {@link CacheDocumentFiltre}.
 */
public class CacheDocumentResourceServiceImplTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private Path fitxers;
	private CacheDocumentResourceServiceImpl service;

	@Before
	public void setUp() throws IOException {
		fitxers = temp.newFolder("fitxers").toPath();
		ConfigHelper configHelper = Mockito.mock(ConfigHelper.class);
		Mockito.when(configHelper.getConfig(PropertyConfig.PROP_FITXERS_PATH)).thenReturn(fitxers.toString());
		service = new CacheDocumentResourceServiceImpl(new CacheDocumentHelper(configHelper));
	}

	/** Fitxer amb la darrera modificació fa {@code minuts} minuts. */
	private void fitxer(String carpeta, String nom, long minuts) throws IOException {
		Path dir = fitxers.resolve("cache").resolve(carpeta);
		Files.createDirectories(dir);
		Path fitxer = Files.write(dir.resolve(nom), new byte[] { 1, 2, 3 });
		Files.setLastModifiedTime(fitxer, FileTime.fromMillis(System.currentTimeMillis() - minuts * 60_000L));
	}

	private void escenari() throws IOException {
		fitxer("INFO", "CSV_aaa", 5);
		fitxer("INFO", "UUID_bbb", 20);
		fitxer("ORIGINAL", "aaa", 10);
		fitxer("IMPRIMIBLE", "aaa_CA", 1);
		fitxer("IMPRIMIBLE", "ccc_ES", 30);
	}

	private List<String> ids(Page<NoDatabaseResourceEntity<CacheDocumentResource, String>> pagina) {
		return pagina.getContent().stream().map(NoDatabaseResourceEntity::getId).collect(Collectors.toList());
	}

	private Page<NoDatabaseResourceEntity<CacheDocumentResource, String>> cerca(String rapid, String filtre, Pageable pageable) {
		return service.entityRepositoryFindEntities(rapid, filtre, null, pageable);
	}

	// --- Servei -----------------------------------------------------------------------------

	@Test
	public void senseOrdreNiFiltreEsMostraTotAmbLaDarreraConsultaPrimer() throws IOException {
		escenari();

		Page<NoDatabaseResourceEntity<CacheDocumentResource, String>> pagina = cerca(null, null, Pageable.unpaged());

		assertEquals(5, pagina.getTotalElements());
		assertEquals(
				Arrays.asList("IMPRIMIBLE:aaa_CA", "INFO:CSV_aaa", "ORIGINAL:aaa", "INFO:UUID_bbb", "IMPRIMIBLE:ccc_ES"),
				ids(pagina));
	}

	@Test
	public void filtraPerCarpeta() throws IOException {
		escenari();

		Page<NoDatabaseResourceEntity<CacheDocumentResource, String>> pagina = cerca(null, "carpeta : 'INFO'", Pageable.unpaged());

		assertEquals(Arrays.asList("INFO:CSV_aaa", "INFO:UUID_bbb"), ids(pagina));
	}

	@Test
	public void elFiltreRapidCercaALIdentificadorSenseDistingirMajuscules() throws IOException {
		escenari();

		Page<NoDatabaseResourceEntity<CacheDocumentResource, String>> pagina = cerca("AAA", null, Pageable.unpaged());

		assertEquals(Arrays.asList("IMPRIMIBLE:aaa_CA", "INFO:CSV_aaa", "ORIGINAL:aaa"), ids(pagina));
	}

	@Test
	public void elFiltreRapidIElFiltreEsCombinenAmbAnd() throws IOException {
		escenari();

		Page<NoDatabaseResourceEntity<CacheDocumentResource, String>> pagina = cerca("aaa", "carpeta:'ORIGINAL'", Pageable.unpaged());

		assertEquals(Collections.singletonList("ORIGINAL:aaa"), ids(pagina));
	}

	@Test
	public void ordenaPelCampDemanat() throws IOException {
		escenari();

		Pageable perMida = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("identificador"), Sort.Order.desc("carpeta")));

		assertEquals(
				Arrays.asList("ORIGINAL:aaa", "INFO:CSV_aaa", "IMPRIMIBLE:aaa_CA", "INFO:UUID_bbb", "IMPRIMIBLE:ccc_ES"),
				ids(cerca(null, null, perMida)));
	}

	@Test
	public void ordenaPerExpiracioAscendentCoincideixAmbLaDarreraConsultaAscendent() throws IOException {
		escenari();

		Page<NoDatabaseResourceEntity<CacheDocumentResource, String>> pagina =
				cerca(null, null, PageRequest.of(0, 10, Sort.by("expiracio").ascending()));

		assertEquals("IMPRIMIBLE:ccc_ES", ids(pagina).get(0));
		assertEquals("IMPRIMIBLE:aaa_CA", ids(pagina).get(4));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ordenarPerUnCampDesconegutEsUnError() throws IOException {
		escenari();

		cerca(null, null, PageRequest.of(0, 10, Sort.by("secret")));
	}

	@Test
	public void paginaIRetornaElTotalSenceri() throws IOException {
		escenari();

		Page<NoDatabaseResourceEntity<CacheDocumentResource, String>> primera = cerca(null, null, PageRequest.of(0, 2));
		Page<NoDatabaseResourceEntity<CacheDocumentResource, String>> darrera = cerca(null, null, PageRequest.of(2, 2));
		Page<NoDatabaseResourceEntity<CacheDocumentResource, String>> fora = cerca(null, null, PageRequest.of(9, 2));

		assertEquals(5, primera.getTotalElements());
		assertEquals(Arrays.asList("IMPRIMIBLE:aaa_CA", "INFO:CSV_aaa"), ids(primera));
		assertEquals(Collections.singletonList("IMPRIMIBLE:ccc_ES"), ids(darrera));
		assertTrue(fora.getContent().isEmpty());
	}

	@Test
	public void filtraPerCaducat() throws IOException {
		fitxer("ORIGINAL", "fresc", 5);
		fitxer("ORIGINAL", "vell", 60);

		assertEquals(Collections.singletonList("ORIGINAL:vell"), ids(cerca(null, "caducat:true", Pageable.unpaged())));
		assertEquals(Collections.singletonList("ORIGINAL:fresc"), ids(cerca(null, "caducat:false", Pageable.unpaged())));
		assertEquals(Collections.singletonList("ORIGINAL:fresc"), ids(cerca(null, "caducat!true", Pageable.unpaged())));
		assertEquals(2, cerca(null, "carpeta:'ORIGINAL' and (caducat:true or caducat:false)", Pageable.unpaged()).getTotalElements());
	}

	@Test
	public void ambTtlZeroCapEntradaEsCaducada() throws IOException {
		ConfigHelper configHelper = Mockito.mock(ConfigHelper.class);
		Mockito.when(configHelper.getConfig(PropertyConfig.PROP_FITXERS_PATH)).thenReturn(fitxers.toString());
		Mockito.when(configHelper.getConfig(PropertyConfig.PROP_CACHE_TTL_MINUTS)).thenReturn("0");
		service = new CacheDocumentResourceServiceImpl(new CacheDocumentHelper(configHelper));
		fitxer("ORIGINAL", "moltVell", 10_000);

		assertTrue(cerca(null, "caducat:true", Pageable.unpaged()).getContent().isEmpty());
		assertEquals(1, cerca(null, "caducat:false", Pageable.unpaged()).getTotalElements());
	}

	@Test
	public void trobaUnaEntradaPelSeuIdentificador() throws IOException {
		escenari();

		Optional<NoDatabaseResourceEntity<CacheDocumentResource, String>> trobada = service.entityRepositoryFindOne("INFO:UUID_bbb");

		assertTrue(trobada.isPresent());
		assertEquals("bbb", trobada.get().getResource().getIdentificador());
		assertFalse(service.entityRepositoryFindOne("INFO:UUID_inexistent").isPresent());
		assertFalse(service.entityRepositoryFindOne("INFO:../../x").isPresent());
	}

	@Test
	public void leresEntradesLlistadesTenenLesSevesDades() throws IOException {
		escenari();

		CacheDocumentResource resource = service.entityRepositoryFindOne("IMPRIMIBLE:ccc_ES").get().getResource();

		assertEquals(CacheCarpeta.IMPRIMIBLE, resource.getCarpeta());
		assertEquals("ccc", resource.getIdentificador());
		assertEquals("ES", resource.getIdioma());
		assertEquals(Long.valueOf(3), resource.getMida());
		assertEquals(resource.getDarreraConsulta().getTime() + 30 * 60_000L, resource.getExpiracio().getTime());
	}

	/**
	 * Primer recurs sense base de dades: comprova que la inicialització del framework (que busca un
	 * repositori per a l'entitat) i les consultes de la classe base funcionen sense repositori.
	 */
	@Test
	public void elFrameworkFuncionaSenseRepositori() throws Exception {
		escenari();
		ApplicationContext context = Mockito.mock(ApplicationContext.class);
		Mockito.when(context.getAutowireCapableBeanFactory()).thenReturn(new DefaultListableBeanFactory());
		Field camp = BaseReadonlyResourceService.class.getDeclaredField("applicationContext");
		camp.setAccessible(true);
		camp.set(service, context);

		service.initRepository();
		service.init();

		CacheDocumentResource una = service.getOne("INFO:UUID_bbb", null);
		assertEquals("bbb", una.getIdentificador());
		assertEquals("INFO:UUID_bbb", una.getId());
		Page<CacheDocumentResource> pagina = service.findPage(null, "carpeta:'INFO'", null, null, PageRequest.of(0, 10));
		assertEquals(2, pagina.getTotalElements());
		assertEquals("CSV_aaa", pagina.getContent().get(0).getNomFitxer());
		CacheDocumentResource.ResultatEsborrat resultat = (CacheDocumentResource.ResultatEsborrat)service.artifactActionExec(
				null,
				CacheDocumentResource.ACTION_ESBORRAR_MASSIU_CODE,
				formEsborrar("INFO:UUID_bbb", "ORIGINAL:aaa"));
		assertEquals(2, resultat.getEsborrats());
		assertEquals(3, cerca(null, null, Pageable.unpaged()).getTotalElements());
	}

	private static CacheDocumentResource.FormEsborrarMassiu formEsborrar(String... ids) {
		CacheDocumentResource.FormEsborrarMassiu form = new CacheDocumentResource.FormEsborrarMassiu();
		form.setIds(Arrays.asList(ids));
		return form;
	}

	// --- Filtre en memòria ------------------------------------------------------------------

	private static CacheDocumentResource recurs(CacheCarpeta carpeta, String identificador, String idioma) {
		CacheDocumentResource r = new CacheDocumentResource();
		r.setCarpeta(carpeta);
		r.setIdentificador(identificador);
		r.setIdioma(idioma);
		return r;
	}

	/** Recurs amb l'expiració a {@code minuts} minuts d'ara (negatiu = ja passada). */
	private static CacheDocumentResource recursAmbExpiracio(Long minuts) {
		CacheDocumentResource r = new CacheDocumentResource();
		r.setExpiracio(minuts != null ? new java.util.Date(System.currentTimeMillis() + minuts * 60_000L) : null);
		return r;
	}

	@Test
	public void elFiltreBooleaAdmetIgualIDiferentSenseDistingirMajuscules() {
		CacheDocumentResource caducat = recursAmbExpiracio(-10L);
		CacheDocumentResource noCaducat = recursAmbExpiracio(10L);
		CacheDocumentResource senseExpiracio = recursAmbExpiracio(null);

		assertTrue(CacheDocumentFiltre.parse("caducat:true").test(caducat));
		assertFalse(CacheDocumentFiltre.parse("caducat:true").test(noCaducat));
		assertFalse(CacheDocumentFiltre.parse("caducat:true").test(senseExpiracio));
		assertTrue(CacheDocumentFiltre.parse("caducat:false").test(noCaducat));
		assertTrue(CacheDocumentFiltre.parse("caducat:false").test(senseExpiracio));
		assertTrue(CacheDocumentFiltre.parse("caducat!false").test(caducat));
		// El nom del camp és sensible a majúscules (com a la resta de camps), el valor no.
		assertTrue(CacheDocumentFiltre.parse("caducat:TRUE").test(caducat));
		assertTrue(CacheDocumentFiltre.parse("caducat : true").test(caducat));
	}

	@Test
	public void elFiltreBooleaRefusaCometesIOperadorsNoAdmesos() {
		for (String filtre : new String[] { "caducat:'true'", "caducat~true", "caducat:1", "caducat:'SI'" }) {
			try {
				CacheDocumentFiltre.parse(filtre);
				org.junit.Assert.fail("S'esperava un error amb el filtre: " + filtre);
			} catch (IllegalArgumentException ex) {
				assertTrue(ex.getMessage(), ex.getMessage().contains("Filtre no admès"));
			}
		}
	}

	@Test
	public void elFiltreBuitoNulAcceptaTot() {
		CacheDocumentResource r = recurs(CacheCarpeta.INFO, "abc", null);

		assertTrue(CacheDocumentFiltre.parse(null).test(r));
		assertTrue(CacheDocumentFiltre.parse("  ").test(r));
		assertTrue(CacheDocumentFiltre.rapid(null).test(r));
	}

	@Test
	public void elFiltreAdmetIgualDiferentIConte() {
		CacheDocumentResource r = recurs(CacheCarpeta.INFO, "abc-123", "CA");

		assertTrue(CacheDocumentFiltre.parse("carpeta:'INFO'").test(r));
		assertTrue(CacheDocumentFiltre.parse("carpeta : 'info'").test(r));
		assertFalse(CacheDocumentFiltre.parse("carpeta:'ORIGINAL'").test(r));
		assertTrue(CacheDocumentFiltre.parse("carpeta!'ORIGINAL'").test(r));
		assertTrue(CacheDocumentFiltre.parse("identificador~'%c-1%'").test(r));
		assertTrue(CacheDocumentFiltre.parse("identificador~'abc%'").test(r));
		assertFalse(CacheDocumentFiltre.parse("identificador~'bc'").test(r));
		assertTrue(CacheDocumentFiltre.parse("idioma:'ca'").test(r));
	}

	@Test
	public void elFiltreCombinaAndOrIParentesis() {
		CacheDocumentResource info = recurs(CacheCarpeta.INFO, "abc", null);
		CacheDocumentResource original = recurs(CacheCarpeta.ORIGINAL, "xyz", null);
		Predicate<CacheDocumentResource> p = CacheDocumentFiltre.parse(
				"(carpeta:'INFO' OR carpeta:'ORIGINAL') AND identificador~'%b%'");

		assertTrue(p.test(info));
		assertFalse(p.test(original));
		assertTrue(CacheDocumentFiltre.parse("carpeta:'ORIGINAL' or identificador~'%b%'").test(info));
	}

	@Test
	public void elFiltreAdmetCometesEscapadesIEspecials() {
		CacheDocumentResource r = recurs(CacheCarpeta.INFO, "a.b(c)", null);

		assertTrue(CacheDocumentFiltre.parse("identificador~'%.b(c)%'").test(r));
		assertFalse(CacheDocumentFiltre.parse("identificador~'%xb(c)%'").test(r));
		assertFalse(CacheDocumentFiltre.parse("identificador:'l\\'altre'").test(r));
	}

	@Test
	public void elFiltreRefusaTotElQueNoEntén() {
		for (String filtre : new String[] {
				"mida>3", "secret:'x'", "carpeta:INFO", "carpeta:", "carpeta", "carpeta:'INFO' AND", "(carpeta:'INFO'",
				"carpeta:'INFO')", "carpeta:'INFO' XOR carpeta:'ORIGINAL'", "carpeta='INFO'", "carpeta is null" }) {
			try {
				CacheDocumentFiltre.parse(filtre);
				org.junit.Assert.fail("S'esperava un error amb el filtre: " + filtre);
			} catch (IllegalArgumentException ex) {
				assertTrue(ex.getMessage(), ex.getMessage().contains("Filtre no admès"));
			}
		}
	}

}
