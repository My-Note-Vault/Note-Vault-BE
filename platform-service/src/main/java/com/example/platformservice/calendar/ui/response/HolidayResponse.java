package com.example.platformservice.calendar.ui.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class HolidayResponse {

    private Response response;

    @Setter
    @Getter
    public static class Response {
        private Body body;
    }

    @Setter
    @Getter
    public static class Body {
        private Items items;
    }

    @Setter
    @Getter
    public static class Items {
        private List<Item> item;
    }

    @Setter
    @Getter
    public static class Item {
        private String dateName;
        private String locdate;
        private String isHoliday;
    }
}