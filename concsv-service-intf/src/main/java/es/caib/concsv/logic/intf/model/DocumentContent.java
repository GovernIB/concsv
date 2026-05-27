package es.caib.concsv.logic.intf.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter @Setter
public class DocumentContent implements Serializable {

    private byte[] content;
    private String mimeType;
    private String csv;
    private String fileName;

}
