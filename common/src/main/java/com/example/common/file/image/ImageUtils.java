package com.example.common.file.image;

import com.example.common.file.FileUtils;
import com.example.common.file.UploadFileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String DEFAULT_IMAGE_CONTENT_TYPE = "image/jpeg";

    private final FileUtils fileUtils;

    public String generatePresignedGetUrl(final String key) {
        return fileUtils.generatePresignedGetUrl(key);
    }

    public List<String> generatePresignedGetUrl(final List<String> keys) {
        return fileUtils.generatePresignedGetUrl(keys);
    }


    @Transactional
    public UploadImageResponse generatePresignedPutUrl(final Long memberId) {
        return generatePresignedPutUrl(memberId, DEFAULT_IMAGE_CONTENT_TYPE);
    }

    @Transactional
    public UploadImageResponse generatePresignedPutUrl(final Long memberId, final String contentType) {
        String extension = MIME_TO_EXT.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }

        String key = String.format("%s/%s.%s", memberId, UUID.randomUUID(), extension);
        UploadFileResponse uploadFileResponse = fileUtils.generatePresignedPutUrl(key, contentType);
        return new UploadImageResponse(uploadFileResponse.getPresignedUrl(), uploadFileResponse.getKey());
    }




    @Transactional
    public void confirmImage() {

    }

    public void deleteImage(final String key) {
        fileUtils.deleteFile(key);
        //TODO: image 테이블에서도 삭제를 해줘야한다
    }

}
