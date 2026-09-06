package com.example.platformservice.member.application;

import com.example.platformservice.member.domain.value.BankCode;
import com.example.platformservice.member.infra.TossPaymentsAccountClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayoutAccountVerificationService {

    private final TossPaymentsAccountClient tossPaymentsAccountClient;

    @Value("${toss-payments.secret-key:}")
    private String secretKey;

    public VerifiedAccount verify(final BankCode bankCode, final String accountNumber) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("계좌 인증 서비스가 설정되지 않았습니다");
        }

        TossPaymentsAccountClient.AccountRequest request =
                new TossPaymentsAccountClient.AccountRequest(bankCode.getCode(), accountNumber);
        String authorization = basicAuthorization();

        try {
            TossPaymentsAccountClient.ValidateAccountResponse validation =
                    tossPaymentsAccountClient.validate(authorization, UUID.randomUUID().toString(), request);
            if (validation == null || !validation.isValid()) {
                throw new IllegalArgumentException("유효하지 않은 계좌입니다");
            }

            TossPaymentsAccountClient.HolderNameResponse holder =
                    tossPaymentsAccountClient.lookupHolderName(
                            authorization,
                            UUID.randomUUID().toString(),
                            request
                    );
            if (holder == null || holder.holderName() == null || holder.holderName().isBlank()) {
                throw new IllegalStateException("계좌 예금주를 확인하지 못했습니다");
            }
            return new VerifiedAccount(holder.holderName());
        } catch (FeignException exception) {
            String response = exception.contentUTF8();
            if (response.contains("NOT_AVAILABLE_BANK_ACCOUNT_VERIFICATION")) {
                throw new IllegalStateException("은행 점검 중입니다. 잠시 후 다시 시도해 주세요");
            }
            if (exception.status() == 401 || exception.status() == 403) {
                throw new IllegalStateException("계좌 인증 서비스의 인증 설정을 확인해 주세요");
            }
            if (exception.status() == 429) {
                throw new IllegalStateException("계좌 확인 요청이 많습니다. 잠시 후 다시 시도해 주세요");
            }
            if (exception.status() >= 400 && exception.status() < 500) {
                throw new IllegalArgumentException("은행과 계좌번호를 다시 확인해 주세요");
            }
            throw new IllegalStateException("계좌 인증 서비스에 연결할 수 없습니다");
        }
    }

    private String basicAuthorization() {
        String credentials = secretKey + ":";
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    public record VerifiedAccount(String holderName) {}
}
