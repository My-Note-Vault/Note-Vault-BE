package com.example.common.file.cloudfront;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cloudfront.cookie")
public class CloudFrontCookieProperties {

    private boolean enabled = true;
    private String domainName;
    private String keyPairId;
    private String privateKey = "";
    private String privateKeyPath;
    private String cookieDomain;
    private String path;
    private String sameSite;
    private boolean secure;
    private boolean httpOnly;
    private Duration expiration;
    private String resourcePathPattern;

    public String baseUrl() {
        String trimmedDomainName = requireText(domainName, "CloudFront domain-name은 필수입니다.").trim();
        String baseUrl = hasScheme(trimmedDomainName) ? trimmedDomainName : "https://" + trimmedDomainName;
        return removeTrailingSlash(baseUrl);
    }

    public String resourcePattern() {
        return baseUrl() + normalizePath(resourcePathPattern);
    }

    public String imageUrl(final String key) {
        return baseUrl() + "/" + requireText(key, "이미지 key는 필수입니다.").replaceFirst("^/+", "");
    }

    public void validateForSigning() {
        requireText(domainName, "CloudFront domain-name은 필수입니다.");
        requireText(keyPairId, "CloudFront key-pair-id는 필수입니다.");

        if (isBlank(privateKey) && isBlank(privateKeyPath)) {
            throw new IllegalStateException("CloudFront private-key 또는 private-key-path는 필수입니다.");
        }

        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalStateException("CloudFront cookie expiration은 0보다 커야 합니다.");
        }
    }

    private static boolean hasScheme(final String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private static String removeTrailingSlash(final String value) {
        return value.replaceFirst("/+$", "");
    }

    private static String normalizePath(final String value) {
        String pathValue = requireText(value, "CloudFront resource-path-pattern은 필수입니다.").trim();
        return pathValue.startsWith("/") ? pathValue : "/" + pathValue;
    }

    private static String requireText(final String value, final String message) {
        if (isBlank(value)) {
            throw new IllegalStateException(message);
        }

        return value;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
