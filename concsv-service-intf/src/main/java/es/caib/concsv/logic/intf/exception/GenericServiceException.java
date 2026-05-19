package es.caib.concsv.logic.intf.exception;

import lombok.Getter;

public class GenericServiceException extends Exception {

    @Getter
    private boolean loggable = true;

	public GenericServiceException() {
		super();
	}

    public GenericServiceException(String message, Throwable cause, boolean loggable) {
        super(message, cause);
        this.loggable = loggable;
    }

	public GenericServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public GenericServiceException(String message) {
		super(message);
	}

	public GenericServiceException(Throwable cause) {
		super(cause);
	}


}
