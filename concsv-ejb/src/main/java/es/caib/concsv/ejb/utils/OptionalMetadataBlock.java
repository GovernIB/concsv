package es.caib.concsv.ejb.utils;

import org.apache.log4j.Logger;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OptionalMetadataBlock {

	private static Logger log = Logger.getLogger(OptionalMetadataBlock.class);
	private static final String ext = "optionalMetadata_(ca|es).properties";
	private static final String propertyOptionalMetadataPath = "es.caib.concsv.optionalLabelMetadata.path";
	private static HashMap<String, LinkedHashMap<String, String>> matadataLabels;
	private static String propertiesPath = null;
	
	static {
		try {
			propertiesPath = ConfigProvider.getConfig().getOptionalValue(propertyOptionalMetadataPath, String.class).orElse(null);
		} catch(Throwable th) {
			log.error("Error inicialitzant el config provider: " + th.getMessage(), th);
		}
	}
	
	public OptionalMetadataBlock(){
		try {
			if (propertiesPath != null) {
				File file = new File(propertiesPath);
				if (!file.exists())
					log.error("La propietat especificada: " + propertyOptionalMetadataPath + ": " + propertiesPath + " El directori no existeix.");
				
				if (file.exists() && matadataLabels == null) {			
					matadataLabels = new HashMap<String,LinkedHashMap<String, String>>();			
					File[] listFiles = file.listFiles(new MyFileNameFilter(ext));
					if (listFiles.length > 0) {
						for(File f : listFiles) {
							LinkedHashMap<String, String> idiomProperties = getPropertiesFromFile(f);
							String fileName = f.getName();
							log.debug("Llegint les etiquetes de les metadades: " + fileName);
							String idiom = f.getName().substring(fileName.indexOf('_') + 1, fileName.indexOf('.'));
							matadataLabels.put(idiom, idiomProperties);
							log.info("Etiquetes de idioma carregades: " + idiom);
						}
					}else log.info("No s'ha trobat cap fitxer de metadades opcionals al directori: " + propertiesPath);
				}
			} else log.info("No s'ha inicialitzat la propietat: es.caib.concsv.optionalLabelMetadata.path");
		}catch(Exception e) {
			log.error(e);
		}
		
	}
	
	private LinkedHashMap<String, String> getPropertiesFromFile(File f) throws Exception{
		LinkedHashMap<String, String> prop = new LinkedHashMap<String, String>();
		
		FileInputStream fis = new FileInputStream(f);       
		Scanner sc = new Scanner(fis);  

		while(sc.hasNextLine())  
		{  
			String line = sc.nextLine().trim();
			String txt[] = line.split("=");
			if (!"".equals(line)) {
				if (txt.length == 2)
					prop.put(txt[0], txt[1]);
				else
					log.error(String.format("Error: el fitxer %s conte una parella que no es \"xxxx=zzzz\": %s",
							f.getAbsolutePath(), line));
			}
		}  
		sc.close();     //closes the scanner  
		return prop;
	} 
	
	public ArrayList<Entry<String, String>> getMetadataLabels(String lang, Map<String, Object> metadata) {		
		ArrayList<Entry<String, String>> labelValue = new ArrayList<Entry<String,String>>();
		if (matadataLabels == null || matadataLabels.size() == 0 || metadata == null || metadata.size() == 0) {
			log.info("No hi ha etiquetes de metadades carregades: " + lang);
			return labelValue;
		}
		if (matadataLabels.containsKey(lang)) {
			LinkedHashMap<String, String> idiom = matadataLabels.get(lang);
			//log.debug("Expedient: " + metadata.toString());
			log.debug("Traduint etiquetes metadades opcionals: " + lang);
			for (Entry e : idiom.entrySet()) {
				log.debug("Metadada key: " + e.getKey());
				if (metadata.containsKey(e.getKey())) {
					String name = idiom.get(e.getKey());
					String value = metadata.get(e.getKey()).toString();
					log.debug(String.format("Etiqueta clau: %s traduccio: %s dada: %s",e.getKey(), name, value));
					labelValue.add(new SimpleEntry<String, String>(name, value));
				}
			}
			log.debug("etiquetes parsejades: " + labelValue.size());
		} else log.info("No te metadades opcionals no te l'idioma sol·licitat: " + lang);
		return labelValue;
	}
	

	
	private static class MyFileNameFilter implements FilenameFilter {
		Pattern patternFileName; 

		public MyFileNameFilter(String extension) {
			 patternFileName = Pattern.compile(extension);//. represents single character  
		}

		@Override
		public boolean accept(File dir, String name) {
			Matcher m = patternFileName.matcher(name);  
			return m.matches();
		}

	}	

}
