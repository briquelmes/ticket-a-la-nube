package cl.duoc.ticketalanube.dto;

import cl.duoc.ticketalanube.model.TicketStatus;

public record CreateTicketRequest(String eventId, String userId, TicketStatus status) {
}
