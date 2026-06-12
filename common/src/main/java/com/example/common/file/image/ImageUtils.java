package com.example.common.file.image;

import com.example.common.file.FileUtils;
import com.example.common.file.UploadFileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class ImageUtils {

    private static final Map<String, String> MIME_TO_EXT = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg"
    );

    private static final String DEFAULT_IMAGE_CONTENT_TYPE = "image/jpeg";
    private static final String CONTENT_IMAGE_KEY_PREFIX = "content/";
    private static final String PROFILE_IMAGE_KEY_PREFIX = "profile/";
    private static final Pattern CONTENT_IMAGE_KEY_PATTERN = Pattern.compile(
            "content/(?:daily-note|workspace|task|subtask|note|trivia)/[^\\s\"'<>)]*?\\.(?:png|jpe?g)"
    );

    private final FileUtils fileUtils;

    public String generatePresignedGetUrl(final String key) {
        return fileUtils.generatePresignedGetUrl(key);
    }

    public List<String> generatePresignedGetUrl(final List<String> keys) {
        return fileUtils.generatePresignedGetUrl(keys);
    }

    public String generateContentImagePresignedGetUrl(final String key) {
        validateContentImageKey(key);
        return fileUtils.generatePresignedGetUrl(key);
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

        String key = String.format("%s%s/%s.%s", PROFILE_IMAGE_KEY_PREFIX, memberId, UUID.randomUUID(), extension);
        UploadFileResponse uploadFileResponse = fileUtils.generatePresignedPutUrl(key, contentType);
        return new UploadImageResponse(uploadFileResponse.getPresignedUrl(), uploadFileResponse.getKey());
    }

    @Transactional
    public UploadImageResponse generateContentImagePresignedPutUrl(
            final Long memberId,
            final ContentImageTarget target,
            final String contentType
    ) {
        String extension = MIME_TO_EXT.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }

        String key = String.format(
                "%s%s/%s/%s.%s",
                CONTENT_IMAGE_KEY_PREFIX,
                target.getPath(),
                memberId,
                UUID.randomUUID(),
                extension
        );
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

    public void deleteRemovedContentImages(final String oldContent, final String newContent) {
        if (newContent == null) {
            return;
        }

        Set<String> removedImageKeys = extractContentImageKeys(oldContent);
        removedImageKeys.removeAll(extractContentImageKeys(newContent));
        removedImageKeys.forEach(this::deleteImage);
    }

    public void deleteAllContentImages(final String content) {
        extractContentImageKeys(content).forEach(this::deleteImage);
    }

    public Set<String> extractContentImageKeys(final String content) {
        Set<String> imageKeys = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return imageKeys;
        }

        Matcher matcher = CONTENT_IMAGE_KEY_PATTERN.matcher(content);
        while (matcher.find()) {
            String key = matcher.group();
            validateContentImageKey(key);
            imageKeys.add(key);
        }

        return imageKeys;
    }

    public void validateContentImageKey(final String key) {
        if (key == null || key.isBlank() || key.contains("..")) {
            throw new IllegalArgumentException("올바르지 않은 이미지 key 입니다.");
        }

        for (ContentImageTarget target : ContentImageTarget.values()) {
            if (key.startsWith(CONTENT_IMAGE_KEY_PREFIX + target.getPath() + "/")) {
                return;
            }
        }

        throw new IllegalArgumentException("올바르지 않은 이미지 key 입니다.");
    }

}
