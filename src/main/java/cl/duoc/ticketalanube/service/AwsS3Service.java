package cl.duoc.ticketalanube.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.ticketalanube.exception.StorageException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Service
public class AwsS3Service {

	private final S3Client s3Client;

	public AwsS3Service(S3Client s3Client) {
		this.s3Client = s3Client;
	}

	public void upload(String bucket, String key, byte[] content, String contentType) {
		try {
			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.contentType(contentType)
					.contentLength((long) content.length)
					.build();
			s3Client.putObject(request, RequestBody.fromBytes(content));
		} catch (S3Exception exception) {
			throw storageError("No se pudo guardar el archivo en S3", exception);
		}
	}

	public byte[] download(String bucket, String key) {
		try {
			GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();
			ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
			return response.asByteArray();
		} catch (S3Exception exception) {
			throw storageError("No se pudo descargar el archivo desde S3", exception);
		}
	}

	public List<String> listKeys(String bucket, String prefix) {
		try {
			List<String> keys = new ArrayList<>();
			String continuationToken = null;
			boolean hasMore;

			do {
				ListObjectsV2Request.Builder request = ListObjectsV2Request.builder()
						.bucket(bucket)
						.prefix(prefix);
				if (continuationToken != null) {
					request.continuationToken(continuationToken);
				}

				ListObjectsV2Response response = s3Client.listObjectsV2(request.build());
				response.contents().forEach(object -> keys.add(object.key()));
				continuationToken = response.nextContinuationToken();
				hasMore = Boolean.TRUE.equals(response.isTruncated());
			} while (hasMore);

			return keys;
		} catch (S3Exception exception) {
			throw storageError("No se pudo consultar el historial en S3", exception);
		}
	}

	public void delete(String bucket, String key) {
		try {
			s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
		} catch (S3Exception exception) {
			throw storageError("No se pudo eliminar el archivo de S3", exception);
		}
	}

	private StorageException storageError(String message, S3Exception exception) {
		int status = exception.statusCode() == 403 ? 403 : exception.statusCode() == 404 ? 404 : 502;
		return new StorageException(message, status, exception);
	}
}
