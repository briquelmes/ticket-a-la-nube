package cl.duoc.ticketalanube.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.duoc.ticketalanube.exception.StorageException;

@Service
public class EfsService {

	private final Path root;

	public EfsService(@Value("${ticket.storage.efs-path}") String efsPath) {
		this.root = Path.of(efsPath).toAbsolutePath().normalize();
	}

	public void save(String key, byte[] content) {
		Path destination = root.resolve(key).normalize();
		if (!destination.startsWith(root)) {
			throw new IllegalArgumentException("La ruta del archivo no es valida");
		}

		try {
			Files.createDirectories(destination.getParent());
			Files.write(destination, content);
		} catch (IOException exception) {
			throw new StorageException("No se pudo guardar el ticket en EFS", 500, exception);
		}
	}

	public void delete(String key) {
		Path destination = root.resolve(key).normalize();
		if (!destination.startsWith(root)) {
			throw new IllegalArgumentException("La ruta del archivo no es valida");
		}

		try {
			Files.deleteIfExists(destination);
		} catch (IOException exception) {
			throw new StorageException("No se pudo eliminar el ticket de EFS", 500, exception);
		}
	}
}
