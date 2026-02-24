package com.example.platformservice.calendar.infra;

import com.example.platformservice.FeignXmlConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "holidayClient",
        url = "http://apis.data.go.kr",
        configuration = FeignXmlConfig.class
)
public interface CalendarFeignClient {

    @GetMapping("/B090041/openapi/service/SpcdeInfoService/getRestDeInfo")
    String getHoliday(
            @RequestParam("serviceKey") String serviceKey,
            @RequestParam("solYear") String year,
            @RequestParam("solMonth") String month,
            @RequestParam("_type") String type
    );
}
