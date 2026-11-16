package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.intf.model.CacheDocumentResource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Avalua en memòria el filtre Spring Filter que envia el llistat de la cache de documents.
 * <p>
 * La cache no és a base de dades, així que no es pot traduir el filtre a una consulta. Només
 * s'admet el que fa servir la pantalla: les condicions {@code camp:'valor'} (igual),
 * {@code camp!'valor'} (diferent) i {@code camp~'%valor%'} (conté, amb {@code %} de comodí) sobre
 * els camps {@code carpeta}, {@code identificador}, {@code tipusIdentificador} i {@code idioma};
 * i {@code caducat:true}/{@code caducat:false} (igual, sense cometes, com envia el client per a un
 * camp booleà) sobre el camp calculat {@code caducat} ({@code expiracio} ja ha passat; un document
 * sense expiració no és mai caducat). Les condicions es combinen amb {@code AND}, {@code OR} i
 * parèntesis. Les comparacions de text no distingeixen majúscules. Qualsevol altra cosa és un
 * error, perquè ignorar un filtre mostraria (i deixaria esborrar) més del que l'usuari ha demanat.
 *
 * @author Límit Tecnologies
 */
final class CacheDocumentFiltre {

	private static final Pattern TOKEN = Pattern.compile(
			"\\s*(?:(?<str>'(?:[^'\\\\]|\\\\.)*')|(?<op>[:!~()])|(?<id>[A-Za-z_][A-Za-z0-9_]*))");

	private CacheDocumentFiltre() {}

	/** Camps filtrables i com es llegeixen del recurs. */
	private static final Map<String, Function<CacheDocumentResource, String>> CAMPS = new HashMap<>();
	static {
		CAMPS.put("carpeta", r -> r.getCarpeta() != null ? r.getCarpeta().name() : null);
		CAMPS.put("identificador", CacheDocumentResource::getIdentificador);
		CAMPS.put("tipusIdentificador", CacheDocumentResource::getTipusIdentificador);
		CAMPS.put("idioma", CacheDocumentResource::getIdioma);
	}

	/** Camps booleans calculats (no són cap propietat del recurs) i com s'avaluen. */
	private static final Map<String, Predicate<CacheDocumentResource>> CAMPS_BOOLEANS = new HashMap<>();
	static {
		CAMPS_BOOLEANS.put("caducat", CacheDocumentFiltre::esCaducat);
	}

	/** Un document sense data d'expiració (TTL a 0) no caduca mai. */
	private static boolean esCaducat(CacheDocumentResource resource) {
		Date expiracio = resource.getExpiracio();
		return expiracio != null && expiracio.before(new Date());
	}

	/**
	 * @return el predicat del filtre, o un que ho accepta tot si és buit.
	 * @throws IllegalArgumentException si el filtre no és d'un dels formats admesos.
	 */
	static Predicate<CacheDocumentResource> parse(String filtre) {
		if (filtre == null || filtre.trim().isEmpty()) {
			return r -> true;
		}
		Parser parser = new Parser(filtre);
		Predicate<CacheDocumentResource> predicat = parser.or();
		if (parser.pos < parser.tokens.size()) {
			throw parser.error("text inesperat '" + parser.tokens.get(parser.pos) + "'");
		}
		return predicat;
	}

	/** Filtre ràpid: l'identificador conté el text, sense distingir majúscules. */
	static Predicate<CacheDocumentResource> rapid(String text) {
		if (text == null || text.trim().isEmpty()) {
			return r -> true;
		}
		String cerca = text.trim().toLowerCase(Locale.ROOT);
		return r -> r.getIdentificador() != null && r.getIdentificador().toLowerCase(Locale.ROOT).contains(cerca);
	}

	private static final class Parser {

		private final String input;
		private final List<String> tokens = new ArrayList<>();
		private int pos;

		Parser(String input) {
			this.input = input;
			int fins = 0;
			Matcher m = TOKEN.matcher(input);
			while (fins < input.length() && m.find(fins) && m.start() == fins) {
				tokens.add(m.group().trim());
				fins = m.end();
			}
			if (!input.substring(fins).trim().isEmpty()) {
				throw error("caràcter inesperat a la posició " + fins);
			}
		}

		IllegalArgumentException error(String motiu) {
			return new IllegalArgumentException("Filtre no admès (" + motiu + "): " + input);
		}

		private boolean paraula(String s) {
			return pos < tokens.size() && tokens.get(pos).equalsIgnoreCase(s);
		}

		Predicate<CacheDocumentResource> or() {
			Predicate<CacheDocumentResource> resultat = and();
			while (paraula("OR")) {
				pos++;
				resultat = resultat.or(and());
			}
			return resultat;
		}

		Predicate<CacheDocumentResource> and() {
			Predicate<CacheDocumentResource> resultat = terme();
			while (paraula("AND")) {
				pos++;
				resultat = resultat.and(terme());
			}
			return resultat;
		}

		Predicate<CacheDocumentResource> terme() {
			if (paraula("(")) {
				pos++;
				Predicate<CacheDocumentResource> intern = or();
				if (!paraula(")")) {
					throw error("falta un parèntesi de tancament");
				}
				pos++;
				return intern;
			}
			if (pos + 3 > tokens.size()) {
				throw error("condició incompleta");
			}
			String camp = tokens.get(pos++);
			String operador = tokens.get(pos++);
			String valor = tokens.get(pos++);
			if (CAMPS_BOOLEANS.containsKey(camp)) {
				return termeBoolea(camp, operador, valor);
			}
			Function<CacheDocumentResource, String> lector = CAMPS.get(camp);
			if (lector == null) {
				throw error("camp no filtrable '" + camp + "'");
			}
			if (!valor.startsWith("'")) {
				throw error("el valor de '" + camp + "' ha d'anar entre cometes simples");
			}
			String text = valor.substring(1, valor.length() - 1).replace("\\'", "'").replace("\\\\", "\\");
			switch (operador) {
			case ":":
				return r -> lector.apply(r) != null && lector.apply(r).equalsIgnoreCase(text);
			case "!":
				return r -> lector.apply(r) == null || !lector.apply(r).equalsIgnoreCase(text);
			case "~":
				Pattern patro = comodi(text);
				return r -> lector.apply(r) != null && patro.matcher(lector.apply(r)).matches();
			default:
				throw error("operador no admès '" + operador + "'");
			}
		}

		/** {@code camp:true}/{@code camp:false} (igual) i {@code camp!true}/{@code camp!false} (diferent). */
		private Predicate<CacheDocumentResource> termeBoolea(String camp, String operador, String valor) {
			boolean esperat;
			if ("true".equalsIgnoreCase(valor)) {
				esperat = true;
			} else if ("false".equalsIgnoreCase(valor)) {
				esperat = false;
			} else {
				throw error("el valor de '" + camp + "' ha de ser true o false, sense cometes");
			}
			if ("!".equals(operador)) {
				esperat = !esperat;
			} else if (!":".equals(operador)) {
				throw error("operador no admès per '" + camp + "' ('" + operador + "')");
			}
			Predicate<CacheDocumentResource> comprovacio = CAMPS_BOOLEANS.get(camp);
			return esperat ? comprovacio : comprovacio.negate();
		}

		/** Patró d'un valor amb {@code %} com a comodí. */
		private static Pattern comodi(String text) {
			StringBuilder regex = new StringBuilder();
			for (String tros : text.split("%", -1)) {
				if (regex.length() > 0) {
					regex.append(".*");
				}
				regex.append(Pattern.quote(tros));
			}
			return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		}

	}

}
