package com.example.platformservice.member.domain;

import com.example.common.Auditable;
import com.example.platformservice.member.domain.value.BankCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Account extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BankCode bankCode;
    @Column(nullable = false)
    private String accountNumber;
    @Column(nullable = false)
    private String accountHolderName;

    public Account(
            final Member member,
            final BankCode bankCode,
            final String accountNumber,
            final String accountHolderName
    ) {
        this.member = member;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
    }
}
