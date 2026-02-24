package com.example.platformservice.calendar.service;

import com.example.platformservice.calendar.domain.Calendar;
import com.example.platformservice.calendar.domain.CalendarRepository;
import com.example.platformservice.calendar.infra.CalendarFeignClient;
import com.example.platformservice.calendar.ui.response.HolidayResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final PlatformTransactionManager transactionManager;

    private final CalendarFeignClient calendarFeignClient;
    private final ObjectMapper objectMapper;

    private static final String SERVICE_KEY = "YOUR_API_KEY";

    public void insertAllHolidayInfo(int year) {
        List<HolidayResponse.Item> result = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            String monthStr = String.format("%02d", month);
            String response = calendarFeignClient.getHoliday(
                    SERVICE_KEY,
                    String.valueOf(year),
                    monthStr,
                    "json"
            );

            try {
                HolidayResponse parsed = objectMapper.readValue(response, HolidayResponse.class);
                if (parsed.getResponse().getBody().getItems().getItem() != null) {
                    result.addAll(parsed.getResponse().getBody().getItems().getItem());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        List<Calendar> calendars = result.stream()
                .map(item -> new Calendar(item.getDateName(), item.getLocdate(), item.getIsHoliday()))
                .toList();
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            calendarRepository.saveAll(calendars);
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
        }
    }
}
