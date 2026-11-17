package es.caib.concsv.persist.entity;

import es.caib.concsv.logic.intf.model.DocumentExclosOrigenEnum;

import javax.persistence.AttributeConverter;

/**
 * Converteix l'origen ENI entre l'enumerat i el valor que es guarda a la base de dades, que és el de
 * la metadada {@code eni:origen}: {@code 0} (ciutadà) o {@code 1} (administració). Qualsevol altre
 * valor es llegeix com a origen desconegut.
 *
 * @author Límit Tecnologies
 */
public class DocumentExclosOrigenConverter implements AttributeConverter<DocumentExclosOrigenEnum, String> {

	@Override
	public String convertToDatabaseColumn(DocumentExclosOrigenEnum origen) {
		if (origen == null) {
			return null;
		}
		return origen == DocumentExclosOrigenEnum.CIUTADA ? "0" : "1";
	}

	@Override
	public DocumentExclosOrigenEnum convertToEntityAttribute(String valor) {
		if ("0".equals(valor)) {
			return DocumentExclosOrigenEnum.CIUTADA;
		}
		if ("1".equals(valor)) {
			return DocumentExclosOrigenEnum.ADMINISTRACIO;
		}
		return null;
	}
}
