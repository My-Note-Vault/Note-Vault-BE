package com.example.platformservice.draw.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@RequiredArgsConstructor
@Component
public class DailyDrawScheduler {
    private final DailyDrawService service;

    @Scheduled(cron = "${draw.daily.cron:0 0 0 * * *}", zone = "${draw.daily.zone:Asia/Seoul}")
    public void drawAtMidnight() {
        drawYesterday();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverMissedDrawAfterStartup() {
        drawYesterday();
    }

    private void drawYesterday() {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        try {
            service.draw(date);
        } catch (Exception exception) {
            log.error("Failed daily draw for {}", date, exception);
        }
    }
}
