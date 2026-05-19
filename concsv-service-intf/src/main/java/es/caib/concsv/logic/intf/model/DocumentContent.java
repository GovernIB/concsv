package es.caib.concsv.logic.intf.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DocumentContent {

    private byte[] content;
    private String mimeType;
    private String csv;
    private String fileName;

}
