package cl.duoc.ticketalanube.exception;

public class TicketNotFoundException extends RuntimeException {

	public TicketNotFoundException(String ticketId) {
		super("No se encontro el ticket: " + ticketId);
	}
}
