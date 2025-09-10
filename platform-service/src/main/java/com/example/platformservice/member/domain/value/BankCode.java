package com.example.platformservice.member.domain.value;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BankCode {

    KB("004", "국민은행"),
    SHINHAN("088", "신한은행"),
    HANA("081", "하나은행"),
    KAKAO("090", "카카오뱅크"),
    TOSS("092", "토스뱅크"),
    K_BANK("089", "케이뱅크");

    private final String code;
    private final String name;
}
