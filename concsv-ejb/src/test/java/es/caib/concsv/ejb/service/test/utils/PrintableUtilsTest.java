package es.caib.concsv.ejb.service.test.utils;

import es.caib.concsv.ejb.utils.PrintableUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Prova la classe PrintableUtils.
 */
public class PrintableUtilsTest {

	@Test
	public void test() throws IOException {
		PrintableUtils printableUtils = new PrintableUtils();
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("enllacos.pdf")) {
			Assert.assertNotNull(inputStream);
			byte[] originalPdf = inputStream.readAllBytes();
			byte[] printablePdf = printableUtils.pdfToPrintablePdf(
					originalPdf,
					null,
					"http://localhost:8080/concsvfront/view/ab8728f4d16d502758433e7e1e141477292c4ac5f1c0294e9aadf45c5b94a37e",
					"ab8728f4d16d502758433e7e1e141477292c4ac5f1c0294e9aadf45c5b94a37e",
					"ca");
			Assert.assertNotNull(printablePdf);
			Path outputPath = Path.of("printable.pdf");
			Files.write(outputPath, printablePdf);
		}
	}

}

