package es.caib.concsv.logic.intf.resourcevalidation;

import es.caib.concsv.logic.intf.base.model.FileReference;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

/**
 * Validador de {@link ValidImageFile}.
 *
 * @author Límit Tecnologies
 */
public class ImageFileValidator implements ConstraintValidator<ValidImageFile, FileReference> {

	private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
			"image/jpeg",
			"image/png",
			"image/gif",
			"image/bmp",
			"image/webp");

	private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
			"jpg", "jpeg", "png", "gif", "bmp", "webp");

	@Override
	public boolean isValid(FileReference fileReference, ConstraintValidatorContext context) {
		// Si és null es considera vàlid (s'ha d'usar @NotNull si és obligatori)
		if (fileReference == null) {
			return true;
		}
		String mimeType = fileReference.getContentType();
		boolean senseTipus = mimeType == null || mimeType.isBlank();
		if (fileReference.getContent() != null && fileReference.getContent().length > 0 && senseTipus) {
			// Peticions PATCH o UPDATE: no hi ha informació del tipus de fitxer
			return true;
		}
		String fileName = fileReference.getName();
		if (fileName != null) {
			String extension = fileName.contains(".") ?
					fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() :
					"";
			if (!ALLOWED_EXTENSIONS.contains(extension)) {
				return false;
			}
		}
		return senseTipus || ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase());
	}

}
