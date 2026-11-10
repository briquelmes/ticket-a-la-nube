package cl.duoc.ticketalanube.dto;

public record EventStatisticsResponse(
		String eventId,
		long totalTickets,
		long reservedTickets,
		long soldTickets,
		long cancelledTickets) {
}
