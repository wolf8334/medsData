package com.xhr.medsdata.controller;

import com.xhr.medsdata.common.ApiResponse;
import com.xhr.medsdata.domain.Views.CalendarDayView;
import com.xhr.medsdata.domain.Views.DayView;
import com.xhr.medsdata.service.HomeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/home")
    public ApiResponse<DayView> home(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(homeService.dayView(date));
    }

    @GetMapping("/calendar")
    public ApiResponse<List<CalendarDayView>> calendar(@RequestParam int year, @RequestParam int month) {
        return ApiResponse.ok(homeService.calendar(year, month));
    }
}
