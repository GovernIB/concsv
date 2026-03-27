/**
 * 
 */
package es.caib.concsv.front.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import es.caib.concsv.service.exception.DocumentNotExistException;
import lombok.extern.slf4j.Slf4j;

/**
 * Tractament global de les excepcions en els controladors.
 * 
 * @author Límit Tecnologies
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	public static final String REQUEST_PARAM_TRACE = "trace";

	@Value("${reflectoring.trace:true}")
	private boolean printStackTrace;

	@ExceptionHandler(DocumentNotExistException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ResponseEntity<Object> handleDocumentNotExistException(
			DocumentNotExistException ex,
			WebRequest request) {
		log.warn(ex.getMessage());
		return buildErrorResponse(
				ex,
				ex.getMessage(),
				HttpStatus.NOT_FOUND,
				request);
	}

	@ExceptionHandler(RuntimeException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ResponseEntity<Object> handleAllUncaughtException(
			RuntimeException ex,
			WebRequest request) {
		log.error("Uncaught exception", ex);
		return buildErrorResponse(
				ex,
				ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "Unknown error",
				HttpStatus.INTERNAL_SERVER_ERROR,
				request);
	}

	@Override
	public ResponseEntity<Object> handleExceptionInternal(
			Exception ex,
			Object body,
			HttpHeaders headers,
			HttpStatus status,
			WebRequest request) {
		log.error("Internal exception", ex);
		return buildErrorResponse(ex, status, request);
	}

	private ResponseEntity<Object> buildErrorResponse(
			Exception ex,
			HttpStatus httpStatus,
			WebRequest request) {
		return buildErrorResponse(ex, ex.getMessage(), httpStatus, request);
	}

	private ResponseEntity<Object> buildErrorResponse(
			Exception ex,
			String message,
			HttpStatus httpStatus,
			WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(httpStatus.value(), message);
		if (printStackTrace && isTraceOn(request)) {
			errorResponse.setStackTrace(getStackTrace(ex));
		}
		return toErrorResponseEntity(httpStatus, errorResponse);
	}

	private boolean isTraceOn(WebRequest request) {
		String[] value = request.getParameterValues(REQUEST_PARAM_TRACE);
		return Objects.nonNull(value) && value.length > 0 && (value[0].isEmpty() || value[0].contentEquals("true"));
	}

	private String getStackTrace(Throwable th) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		th.printStackTrace(pw);
		return sw.toString();
	}
	
	private ResponseEntity<Object> toErrorResponseEntity(
			HttpStatus httpStatus,
			ErrorResponse errorResponse) {
		return ResponseEntity.
				status(httpStatus).
				header("Content-Type", "application/problem+json; charset=utf-8").
				body(errorResponse);
	}

}
