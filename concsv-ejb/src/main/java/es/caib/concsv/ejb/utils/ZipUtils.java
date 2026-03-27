package es.caib.concsv.ejb.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
	
	ByteArrayOutputStream baos = new ByteArrayOutputStream();	
	ZipOutputStream out;
	
	public ZipUtils() {
		out = new ZipOutputStream(baos);
	}
	
	public void addFile(String fileName, byte[] content) throws IOException {
		ZipEntry zip = new ZipEntry(fileName);
		out.putNextEntry(zip);
		out.write(content);
		out.closeEntry();
	}
	
	public ByteArrayOutputStream generate() throws IOException {
		out.close();
		return baos;
	}
}
