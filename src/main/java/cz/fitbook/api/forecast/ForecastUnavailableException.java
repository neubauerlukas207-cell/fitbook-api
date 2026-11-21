package cz.fitbook.api.forecast;

/** Služba předpovědí není nastavená nebo neodpovídá → HTTP 503. */
public class ForecastUnavailableException extends RuntimeException {

	public ForecastUnavailableException(String message) {
		super(message);
	}
}
