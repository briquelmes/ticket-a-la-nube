package cl.duoc.ticketalanube.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.ticketalanube.dto.CreateTicketRequest;
import cl.duoc.ticketalanube.dto.EventStatisticsResponse;
import cl.duoc.ticketalanube.dto.TicketResponse;
import cl.duoc.ticketalanube.dto.UpdateTicketRequest;
import cl.duoc.ticketalanube.exception.InvalidTicketException;
import cl.duoc.ticketalanube.exception.StorageException;
import cl.duoc.ticketalanube.exception.TicketNotFoundException;
import cl.duoc.ticketalanube.model.TicketStatus;

@Service
public class TicketService {

	private static final String METADATA_PREFIX = "_ticket-metadata/";
	private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9_-]{1,60}");

	private final AwsS3Service s3Service;
	private final EfsService efsService;
	private final TicketPdfService pdfService;
	private final ObjectMapper objectMapper;

	@Value("${ticket.storage.bucket}")
	private String bucket;

	public TicketService(AwsS3Service s3Service, EfsService efsService, TicketPdfService pdfService,
			ObjectMapper objectMapper) {
		this.s3Service = s3Service;
		this.efsService = efsService;
		this.pdfService = pdfService;
		this.objectMapper = objectMapper;
	}

	public TicketResponse create(CreateTicketRequest request) {
		if (request == null) {
			throw new InvalidTicketException("Los datos del ticket son obligatorios");
		}
		validateIdentifier(request.eventId(), "eventId");
		validateIdentifier(request.userId(), "userId");
		TicketStatus status = request.status() == null ? TicketStatus.RESERVADO : request.status();
		String id = UUID.randomUUID().toString();

		TicketResponse ticket = new TicketResponse(
				id,
				request.eventId(),
				request.userId(),
				status,
				Instant.now(),
				ticketKey(request.eventId(), request.userId(), id));

		saveTicket(ticket);
		return ticket;
	}

	public List<TicketResponse> history(String eventId, String userId, TicketStatus status) {
		if (eventId != null && !eventId.isBlank()) {
			validateIdentifier(eventId, "eventId");
		}
		if (userId != null && !userId.isBlank()) {
			validateIdentifier(userId, "userId");
		}

		return s3Service.listKeys(requireBucket(), METADATA_PREFIX).stream()
				.map(this::readMetadata)
				.filter(ticket -> eventId == null || eventId.isBlank() || ticket.eventId().equals(eventId))
				.filter(ticket -> userId == null || userId.isBlank() || ticket.userId().equals(userId))
				.filter(ticket -> status == null || ticket.status() == status)
				.toList();
	}

	public byte[] download(String ticketId) {
		return s3Service.download(requireBucket(), readTicket(ticketId).objectKey());
	}

	public TicketResponse update(String ticketId, UpdateTicketRequest request) {
		TicketResponse current = readTicket(ticketId);
		if (request == null) {
			throw new InvalidTicketException("Los datos para actualizar el ticket son obligatorios");
		}
		validateIdentifier(request.eventId(), "eventId");
		validateIdentifier(request.userId(), "userId");
		if (request.status() == null) {
			throw new InvalidTicketException("El estado del ticket es obligatorio");
		}

		TicketResponse updated = new TicketResponse(
				current.id(),
				request.eventId(),
				request.userId(),
				request.status(),
				current.createdAt(),
				ticketKey(request.eventId(), request.userId(), current.id()));
		String bucketName = requireBucket();
		byte[] pdf = pdfService.generate(updated);
		efsService.save(updated.objectKey(), pdf);
		s3Service.upload(bucketName, updated.objectKey(), pdf, "application/pdf");
		s3Service.upload(bucketName, metadataKey(updated.id()), serialize(updated), "application/json");

		if (!current.objectKey().equals(updated.objectKey())) {
			s3Service.delete(bucketName, current.objectKey());
			efsService.delete(current.objectKey());
		}
		return updated;
	}

	public void delete(String ticketId) {
		TicketResponse ticket = readTicket(ticketId);
		s3Service.delete(requireBucket(), ticket.objectKey());
		s3Service.delete(requireBucket(), metadataKey(ticket.id()));
		efsService.delete(ticket.objectKey());
	}

	public EventStatisticsResponse statistics(String eventId) {
		validateIdentifier(eventId, "eventId");
		List<TicketResponse> tickets = history(eventId, null, null);
		return new EventStatisticsResponse(
				eventId,
				tickets.size(),
				count(tickets, TicketStatus.RESERVADO),
				count(tickets, TicketStatus.VENDIDO),
				count(tickets, TicketStatus.CANCELADO));
	}

	private void saveTicket(TicketResponse ticket) {
		String bucketName = requireBucket();
		byte[] pdf = pdfService.generate(ticket);
		efsService.save(ticket.objectKey(), pdf);
		s3Service.upload(bucketName, ticket.objectKey(), pdf, "application/pdf");
		s3Service.upload(bucketName, metadataKey(ticket.id()), serialize(ticket), "application/json");
	}

	private TicketResponse readTicket(String ticketId) {
		validateIdentifier(ticketId, "ticketId");
		try {
			return objectMapper.readValue(s3Service.download(requireBucket(), metadataKey(ticketId)),
					TicketResponse.class);
		} catch (StorageException exception) {
			if (exception.getStatus() == 404) {
				throw new TicketNotFoundException(ticketId);
			}
			throw exception;
		} catch (IOException exception) {
			throw new StorageException("No se pudo leer la informacion del ticket", 502, exception);
		}
	}

	private TicketResponse readMetadata(String key) {
		try {
			return objectMapper.readValue(s3Service.download(requireBucket(), key), TicketResponse.class);
		} catch (IOException exception) {
			throw new StorageException("No se pudo leer la informacion del ticket", 502, exception);
		}
	}

	private byte[] serialize(TicketResponse ticket) {
		try {
			return objectMapper.writeValueAsBytes(ticket);
		} catch (JsonProcessingException exception) {
			throw new StorageException("No se pudo preparar la informacion del ticket", 500, exception);
		}
	}

	private String requireBucket() {
		if (bucket == null || bucket.isBlank()) {
			throw new IllegalStateException("Configura el nombre del bucket en la variable S3_BUCKET");
		}
		return bucket;
	}

	private String ticketKey(String eventId, String userId, String ticketId) {
		return "events/" + eventId + "/users/" + userId + "/" + ticketId + ".pdf";
	}

	private String metadataKey(String ticketId) {
		return METADATA_PREFIX + ticketId + ".json";
	}

	private void validateIdentifier(String value, String field) {
		if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
			throw new InvalidTicketException(field + " debe tener entre 1 y 60 caracteres alfanumericos, guion o guion bajo");
		}
	}

	private long count(List<TicketResponse> tickets, TicketStatus status) {
		return tickets.stream().filter(ticket -> ticket.status() == status).count();
	}
}
