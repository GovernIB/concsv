package es.caib.concsv.ejb.annotation;

import es.caib.concsv.ejb.interceptor.ErrorInterceptor;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Anotació per marcar beans al que s'ha d'aplicar el {@link ErrorInterceptor}.
 *
 * @author areus
 */
@Inherited
@InterceptorBinding
@Target({TYPE, METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ErrorInt {
}
