package cl.duoc.ticketalanube.exception;

public class StorageException extends RuntimeException {

	private final int status;

	public StorageException(String message, int status, Throwable cause) {
		super(message, cause);
		this.status = status;
	}

	public int getStatus() {
		return status;
	}
}
