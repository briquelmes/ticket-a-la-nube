package cl.duoc.ticketalanube.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import cl.duoc.ticketalanube.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(InvalidTicketException.class)
	public ResponseEntity<ErrorResponse> handleInvalidTicket(InvalidTicketException exception, WebRequest request) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
	}

	@ExceptionHandler(TicketNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleTicketNotFound(TicketNotFoundException exception, WebRequest request) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(StorageException.class)
	public ResponseEntity<ErrorResponse> handleStorageError(StorageException exception, WebRequest request) {
		log.error("Error de almacenamiento: {}", exception.getMessage(), exception);
		HttpStatus status = HttpStatus.valueOf(exception.getStatus());
		return error(status, exception.getMessage(), request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRequest(HttpMessageNotReadableException exception,
			WebRequest request) {
		return error(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es valido", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception exception, WebRequest request) {
		log.error("Error inesperado al procesar la solicitud", exception);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error inesperado", request);
	}

	private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, WebRequest request) {
		ErrorResponse body = new ErrorResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getDescription(false).replace("uri=", ""));
		return ResponseEntity.status(status).body(body);
	}
}
