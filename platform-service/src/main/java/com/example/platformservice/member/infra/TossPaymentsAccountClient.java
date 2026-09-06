package com.example.platformservice.member.infra;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "tossPaymentsAccountClient",
        url = "${toss-payments.api-url:https://api.tosspayments.com}"
)
public interface TossPaymentsAccountClient {

    @PostMapping("/v2/bank-accounts/validate")
    ValidateAccountResponse validate(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody AccountRequest request
    );

    @PostMapping("/v2/bank-accounts/lookup-holder-name")
    HolderNameResponse lookupHolderName(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody AccountRequest request
    );

    record AccountRequest(String bankCode, String accountNumber) {}
    record ValidateAccountResponse(boolean isValid) {}
    record HolderNameResponse(String holderName) {}
}
