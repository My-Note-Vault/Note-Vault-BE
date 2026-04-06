package com.example.common.file;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FileUtils {

    private static final Duration GET_URL_DURATION = Duration.ofMinutes(5);
    private static final Duration PUT_URL_DURATION = Duration.ofMinutes(10);

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    public String generatePresignedGetUrl(final String key) {
        GetObjectPresignRequest objectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(GET_URL_DURATION)
                .getObjectRequest(getObjectRequest -> getObjectRequest.bucket(bucket).key(key))
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(objectPresignRequest);
        return presignedRequest.url().toString();
    }

    public List<String> generatePresignedGetUrl(final List<String> keys) {
        return keys.stream()
                .map(this::generatePresignedGetUrl)
                .toList();
    }

    public UploadFileResponse generatePresignedPutUrl(final String key, final String contentType) {
        PutObjectPresignRequest objectPresignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PUT_URL_DURATION)
                .putObjectRequest(putObjectRequest -> putObjectRequest
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build())
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(objectPresignRequest);
        return new UploadFileResponse(presignedRequest.url().toString(), key);
    }

    public void deleteFile(final String key) {
        s3Client.deleteObject(deleteRequest -> deleteRequest.bucket(bucket).key(key));
    }
}
