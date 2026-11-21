package cz.fitbook.api.common;

/** Požadavek je platný, ale porušuje obchodní pravidlo (plná lekce, pozdní storno…) → HTTP 409. */
public class BusinessRuleException extends RuntimeException {

	private final String code;

	public BusinessRuleException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
