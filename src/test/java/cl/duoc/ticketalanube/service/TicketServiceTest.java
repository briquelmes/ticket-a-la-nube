package cl.duoc.ticketalanube.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.ticketalanube.dto.CreateTicketRequest;
import cl.duoc.ticketalanube.dto.EventStatisticsResponse;
import cl.duoc.ticketalanube.dto.TicketResponse;
import cl.duoc.ticketalanube.model.TicketStatus;

class TicketServiceTest {

	private AwsS3Service s3Service;
	private EfsService efsService;
	private TicketService ticketService;

	@BeforeEach
	void setUp() {
		s3Service = org.mockito.Mockito.mock(AwsS3Service.class);
		efsService = org.mockito.Mockito.mock(EfsService.class);
		ticketService = new TicketService(s3Service, efsService, new TicketPdfService(),
				new ObjectMapper().findAndRegisterModules());
		ReflectionTestUtils.setField(ticketService, "bucket", "test-bucket");
	}

	@Test
	void createsTicketAndStoresPdfInEfsAndS3() {
		TicketResponse ticket = ticketService.create(new CreateTicketRequest("evento1", "usuario1", null));

		assertNotNull(ticket.id());
		assertEquals(TicketStatus.RESERVADO, ticket.status());
		assertEquals("events/evento1/users/usuario1/" + ticket.id() + ".pdf", ticket.objectKey());
		verify(efsService).save(eq(ticket.objectKey()), any(byte[].class));
		verify(s3Service).upload(eq("test-bucket"), eq(ticket.objectKey()), any(byte[].class), eq("application/pdf"));
		verify(s3Service).upload(eq("test-bucket"), eq("_ticket-metadata/" + ticket.id() + ".json"),
				any(byte[].class), eq("application/json"));
	}

	@Test
	void countsTicketsByStatusForAnEvent() throws Exception {
		List<TicketResponse> tickets = List.of(
				ticket("ticket1", TicketStatus.RESERVADO),
				ticket("ticket2", TicketStatus.VENDIDO),
				ticket("ticket3", TicketStatus.VENDIDO),
				ticket("ticket4", TicketStatus.CANCELADO));
		when(s3Service.listKeys("test-bucket", "_ticket-metadata/"))
				.thenReturn(List.of("_ticket-metadata/ticket1.json", "_ticket-metadata/ticket2.json",
						"_ticket-metadata/ticket3.json", "_ticket-metadata/ticket4.json"));
		ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
		for (TicketResponse ticket : tickets) {
			when(s3Service.download("test-bucket", "_ticket-metadata/" + ticket.id() + ".json"))
					.thenReturn(mapper.writeValueAsBytes(ticket));
		}

		EventStatisticsResponse statistics = ticketService.statistics("evento1");

		assertEquals(4, statistics.totalTickets());
		assertEquals(1, statistics.reservedTickets());
		assertEquals(2, statistics.soldTickets());
		assertEquals(1, statistics.cancelledTickets());
	}

	private TicketResponse ticket(String id, TicketStatus status) {
		return new TicketResponse(id, "evento1", "usuario1", status, Instant.parse("2026-11-09T12:00:00Z"),
				"events/evento1/users/usuario1/" + id + ".pdf");
	}
}
