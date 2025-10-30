package com.example.common.file;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ImageUtils {

    private static final Map<String, String> MIME_TO_EXT = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg"
    );

    @Value("${todo}")
    private String bucket;

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;


    public String generatePresignedGetUrl(final String key) {
        GetObjectPresignRequest objectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
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


    @Transactional
    public UploadImageResponse generatePresignedPutUrl(final Long memberId) {
        String key = String.format("%s/%s.jpeg", memberId, UUID.randomUUID());

        PutObjectPresignRequest objectPresignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(
                        putRequest -> putRequest
                                .bucket(bucket)
                                .key(key)
                                .contentType("image/jpeg")
                                .build()
                )
                .build();
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(objectPresignRequest);

        String presignedUrl = presignedRequest.url().toString();
        return new UploadImageResponse(presignedUrl, key);
    }




    @Transactional
    public void confirmImage() {

    }

    public void deleteImage(final String key) {
        s3Client.deleteObject(deleteRequest -> deleteRequest.bucket(bucket).key(key));
        //TODO: image 테이블에서도 삭제를 해줘야한다
    }

}
