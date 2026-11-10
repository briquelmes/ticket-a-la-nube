package cl.duoc.ticketalanube.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ticketalanube.dto.CreateTicketRequest;
import cl.duoc.ticketalanube.dto.EventStatisticsResponse;
import cl.duoc.ticketalanube.dto.TicketResponse;
import cl.duoc.ticketalanube.dto.UpdateTicketRequest;
import cl.duoc.ticketalanube.model.TicketStatus;
import cl.duoc.ticketalanube.service.TicketService;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

	private final TicketService ticketService;

	public TicketController(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	@PostMapping
	public ResponseEntity<TicketResponse> create(@RequestBody CreateTicketRequest request) {
		return ResponseEntity.status(201).body(ticketService.create(request));
	}

	@GetMapping
	public List<TicketResponse> history(
			@RequestParam(required = false) String eventId,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) TicketStatus status) {
		return ticketService.history(eventId, userId, status);
	}

	@GetMapping("/{ticketId}/download")
	public ResponseEntity<byte[]> download(@PathVariable String ticketId) {
		byte[] content = ticketService.download(ticketId);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(ticketId + ".pdf").build().toString())
				.body(content);
	}

	@PutMapping("/{ticketId}")
	public TicketResponse update(@PathVariable String ticketId, @RequestBody UpdateTicketRequest request) {
		return ticketService.update(ticketId, request);
	}

	@DeleteMapping("/{ticketId}")
	public ResponseEntity<Void> delete(@PathVariable String ticketId) {
		ticketService.delete(ticketId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/events/{eventId}/statistics")
	public EventStatisticsResponse statistics(@PathVariable String eventId) {
		return ticketService.statistics(eventId);
	}
}
