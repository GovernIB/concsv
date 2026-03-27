package es.caib.concsv.front.interficies;

import es.caib.concsv.service.exception.DocumentNotExistException;
import es.caib.concsv.service.exception.DuplicatedHashException;
import es.caib.concsv.service.exception.GenericServiceException;

import java.io.IOException;

@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws DocumentNotExistException, GenericServiceException, DuplicatedHashException, IOException;
}