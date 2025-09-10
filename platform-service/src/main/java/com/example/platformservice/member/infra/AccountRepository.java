package com.example.platformservice.member.infra;

import com.example.platformservice.member.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
