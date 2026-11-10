package cl.duoc.ticketalanube.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import cl.duoc.ticketalanube.dto.TicketResponse;

@Service
public class TicketPdfService {

	public byte[] generate(TicketResponse ticket) {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage();
			document.addPage(page);

			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				writeLine(content, 50, 740, "Ticket a la Nube");
				writeLine(content, 50, 700, "Ticket: " + ticket.id());
				writeLine(content, 50, 675, "Evento: " + ticket.eventId());
				writeLine(content, 50, 650, "Usuario: " + ticket.userId());
				writeLine(content, 50, 625, "Estado: " + ticket.status());
				writeLine(content, 50, 600, "Fecha: " + ticket.createdAt());
			}

			document.save(output);
			return output.toByteArray();
		} catch (IOException exception) {
			throw new IllegalStateException("No se pudo generar el PDF del ticket", exception);
		}
	}

	private void writeLine(PDPageContentStream content, float x, float y, String text) throws IOException {
		content.beginText();
		content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
		content.newLineAtOffset(x, y);
		content.showText(text);
		content.endText();
	}
}
