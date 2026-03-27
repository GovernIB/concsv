package es.caib.concsv.ejb.utils;

public class StrUtils {

    public synchronized static String normalizeString(String value) {
        return value.replaceAll("[^a-zA-ZñÑÁáÉéÍíÓóÚúÀàÈèÌìÒòÙù0-9.-]", "_");
    }

}
