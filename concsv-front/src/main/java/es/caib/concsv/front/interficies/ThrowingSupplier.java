package es.caib.concsv.front.interficies;

import es.caib.concsv.logic.intf.exception.DocumentNotExistException;
import es.caib.concsv.logic.intf.exception.DuplicatedHashException;
import es.caib.concsv.logic.intf.exception.GenericServiceException;

import java.io.IOException;

@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws DocumentNotExistException, GenericServiceException, DuplicatedHashException, IOException;
}
