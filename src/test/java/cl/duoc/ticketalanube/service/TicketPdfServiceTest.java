package cl.duoc.ticketalanube.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import cl.duoc.ticketalanube.dto.TicketResponse;
import cl.duoc.ticketalanube.model.TicketStatus;

class TicketPdfServiceTest {

	@Test
	void generatesPdfWithTicketDetails() throws IOException {
		TicketResponse ticket = new TicketResponse(
				"ticket-123",
				"evento-1",
				"usuario-7",
				TicketStatus.RESERVADO,
				Instant.parse("2026-11-09T12:00:00Z"),
				"events/evento-1/users/usuario-7/ticket-123.pdf");

		try (var document = Loader.loadPDF(new TicketPdfService().generate(ticket))) {
			String text = new PDFTextStripper().getText(document);
			assertTrue(text.contains("ticket-123"));
			assertTrue(text.contains("evento-1"));
			assertTrue(text.contains("usuario-7"));
			assertTrue(text.contains("RESERVADO"));
		}
	}
}
