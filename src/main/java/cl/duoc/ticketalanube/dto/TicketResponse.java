package cl.duoc.ticketalanube.dto;

import java.time.Instant;

import cl.duoc.ticketalanube.model.TicketStatus;

public record TicketResponse(
		String id,
		String eventId,
		String userId,
		TicketStatus status,
		Instant createdAt,
		String objectKey) {
}
