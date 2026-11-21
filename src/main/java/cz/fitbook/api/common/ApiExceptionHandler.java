package cz.fitbook.api.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import cz.fitbook.api.forecast.ForecastUnavailableException;

/** Převádí výjimky na jednotné chybové odpovědi ve formátu RFC 9457 (application/problem+json). */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail notFound(NotFoundException e) {
		return problem(HttpStatus.NOT_FOUND, "Nenalezeno", e.getMessage());
	}

	@ExceptionHandler(BusinessRuleException.class)
	ProblemDetail businessRule(BusinessRuleException e) {
		ProblemDetail pd = problem(HttpStatus.CONFLICT, "Porušení pravidla", e.getMessage());
		pd.setProperty("code", e.code());
		return pd;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail integrity(DataIntegrityViolationException e) {
		// pojistka pro souběžné požadavky – unikátní omezení v databázi
		return problem(HttpStatus.CONFLICT, "Konflikt dat", "Záznam koliduje s existujícími daty");
	}

	@ExceptionHandler(ForecastUnavailableException.class)
	ProblemDetail forecastUnavailable(ForecastUnavailableException e) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "Předpověď není dostupná", e.getMessage());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
		ProblemDetail body = ex.getBody();
		body.setTitle("Neplatný požadavek");
		body.setDetail("Některá pole nejsou vyplněna správně");
		body.setProperty("errors", errors);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
		pd.setTitle(title);
		return pd;
	}
}
