package cl.duoc.ticketalanube.dto;

import cl.duoc.ticketalanube.model.TicketStatus;

public record UpdateTicketRequest(String eventId, String userId, TicketStatus status) {
}
