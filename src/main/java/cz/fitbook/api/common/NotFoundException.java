package cz.fitbook.api.common;

/** Požadovaný záznam neexistuje → HTTP 404. */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String what, Object id) {
		super(what + " s id " + id + " neexistuje");
	}
}
