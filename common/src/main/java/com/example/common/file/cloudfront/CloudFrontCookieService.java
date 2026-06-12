package com.example.common.file.cloudfront;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class CloudFrontCookieService {

    private static final String POLICY_COOKIE_NAME = "CloudFront-Policy";
    private static final String SIGNATURE_COOKIE_NAME = "CloudFront-Signature";
    private static final String KEY_PAIR_COOKIE_NAME = "CloudFront-Key-Pair-Id";

    private final CloudFrontCookieProperties properties;

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public CloudFrontSignedCookie createSignedCookie() {
        return createSignedCookieForUrl(properties.baseUrl());
    }

    public CloudFrontSignedCookie createSignedCookie(final String key) {
        return createSignedCookieForUrl(properties.imageUrl(key));
    }

    public String baseUrl() {
        return properties.baseUrl();
    }

    public String imageUrl(final String key) {
        return properties.imageUrl(key);
    }

    public long expirationSeconds() {
        return properties.getExpiration().toSeconds();
    }

    private CloudFrontSignedCookie createSignedCookieForUrl(final String imageUrl) {
        properties.validateForSigning();

        String policy = createPolicy(properties.resourcePattern(), expiresAt());
        String encodedPolicy = cloudFrontBase64(policy.getBytes(StandardCharsets.UTF_8));
        String encodedSignature = cloudFrontBase64(sign(policy));

        return new CloudFrontSignedCookie(
                imageUrl,
                List.of(
                        responseCookie(POLICY_COOKIE_NAME, encodedPolicy),
                        responseCookie(SIGNATURE_COOKIE_NAME, encodedSignature),
                        responseCookie(KEY_PAIR_COOKIE_NAME, properties.getKeyPairId())
                )
        );
    }

    private long expiresAt() {
        return Instant.now().plus(properties.getExpiration()).getEpochSecond();
    }

    private String createPolicy(final String resourcePattern, final long expiresAt) {
        return String.format(
                "{\"Statement\":[{\"Resource\":\"%s\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":%d}}}]}",
                resourcePattern,
                expiresAt
        );
    }

    private byte[] sign(final String policy) {
        try {
            Signature signer = Signature.getInstance("SHA1withRSA");
            signer.initSign(readPrivateKey());
            signer.update(policy.getBytes(StandardCharsets.UTF_8));
            return signer.sign();
        } catch (Exception exception) {
            throw new IllegalStateException("CloudFront cookie 서명 생성에 실패했습니다.", exception);
        }
    }

    private PrivateKey readPrivateKey() throws Exception {
        String privateKey = readPrivateKeyPem()
                .replace("\\n", "\n")
                .trim();

        byte[] keyBytes;
        if (privateKey.contains("BEGIN RSA PRIVATE KEY")) {
            keyBytes = wrapPkcs1PrivateKey(decodePem(privateKey));
        } else {
            keyBytes = decodePem(privateKey);
        }

        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private String readPrivateKeyPem() throws Exception {
        if (properties.getPrivateKeyPath() != null && !properties.getPrivateKeyPath().isBlank()) {
            return Files.readString(resolvePrivateKeyPath(properties.getPrivateKeyPath()));
        }

        return properties.getPrivateKey();
    }

    private Path resolvePrivateKeyPath(final String privateKeyPath) throws Exception {
        for (Path candidate : privateKeyPathCandidates(privateKeyPath)) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }

            if (Files.isDirectory(candidate)) {
                Path keyFile = findPrivateKeyFile(candidate);
                if (keyFile != null) {
                    return keyFile;
                }
            }
        }

        throw new IllegalStateException("CloudFront private-key-path 파일을 찾을 수 없습니다: " + privateKeyPath);
    }

    private List<Path> privateKeyPathCandidates(final String privateKeyPath) {
        List<Path> candidates = new ArrayList<>();
        Path configuredPath = Path.of(privateKeyPath);
        candidates.add(configuredPath);

        if (!configuredPath.isAbsolute()) {
            candidates.add(Path.of("/", privateKeyPath));
        }

        if (privateKeyPath.matches("^[a-zA-Z]/.*")) {
            candidates.add(Path.of(privateKeyPath.charAt(0) + ":/" + privateKeyPath.substring(2)));
        }

        return candidates;
    }

    private Path findPrivateKeyFile(final Path directory) throws Exception {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::looksLikePrivateKeyFile)
                    .findFirst()
                    .orElse(null);
        }
    }

    private boolean looksLikePrivateKeyFile(final Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".pem")
                || fileName.endsWith(".key")
                || fileName.contains("private");
    }

    private byte[] decodePem(final String pem) {
        String normalized = pem
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");

        return Base64.getDecoder().decode(normalized);
    }

    private byte[] wrapPkcs1PrivateKey(final byte[] pkcs1PrivateKey) {
        byte[] version = new byte[]{0x02, 0x01, 0x00};
        byte[] algorithmIdentifier = new byte[]{
                0x30, 0x0d,
                0x06, 0x09,
                0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01,
                0x05, 0x00
        };
        byte[] privateKey = der(0x04, pkcs1PrivateKey);

        return der(0x30, concat(version, algorithmIdentifier, privateKey));
    }

    private byte[] der(final int tag, final byte[] value) {
        return concat(new byte[]{(byte) tag}, derLength(value.length), value);
    }

    private byte[] derLength(final int length) {
        if (length < 128) {
            return new byte[]{(byte) length};
        }

        int value = length;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        while (value > 0) {
            bytes.write(value & 0xff);
            value >>= 8;
        }

        byte[] reversed = bytes.toByteArray();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(0x80 | reversed.length);
        for (int index = reversed.length - 1; index >= 0; index--) {
            result.write(reversed[index]);
        }

        return result.toByteArray();
    }

    private byte[] concat(final byte[]... arrays) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] array : arrays) {
            output.writeBytes(array);
        }

        return output.toByteArray();
    }

    private String cloudFrontBase64(final byte[] value) {
        return Base64.getEncoder()
                .encodeToString(value)
                .replace('+', '-')
                .replace('=', '_')
                .replace('/', '~');
    }

    private ResponseCookie responseCookie(final String name, final String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path(properties.getPath())
                .maxAge(properties.getExpiration())
                .secure(properties.isSecure())
                .httpOnly(properties.isHttpOnly());

        if (properties.getSameSite() != null && !properties.getSameSite().isBlank()) {
            builder.sameSite(properties.getSameSite());
        }

        if (properties.getCookieDomain() != null && !properties.getCookieDomain().isBlank()) {
            builder.domain(properties.getCookieDomain());
        }

        return builder.build();
    }
}
