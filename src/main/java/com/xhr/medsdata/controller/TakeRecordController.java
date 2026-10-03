package com.xhr.medsdata.controller;

import com.xhr.medsdata.common.ApiResponse;
import com.xhr.medsdata.domain.TakeRecord;
import com.xhr.medsdata.dto.Requests.TakeReq;
import com.xhr.medsdata.dto.Requests.TakeUpdateReq;
import com.xhr.medsdata.service.TakeRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/take-records")
public class TakeRecordController {

    private final TakeRecordService takeRecordService;

    public TakeRecordController(TakeRecordService takeRecordService) {
        this.takeRecordService = takeRecordService;
    }

    @GetMapping
    public ApiResponse<List<TakeRecord>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long personId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (personId != null && from != null && to != null) {
            return ApiResponse.ok(takeRecordService.listByPersonRange(personId, from, to));
        }
        return ApiResponse.ok(takeRecordService.listByDate(date));
    }

    @GetMapping("/{id}")
    public ApiResponse<TakeRecord> get(@PathVariable long id) {
        return ApiResponse.ok(takeRecordService.get(id));
    }

    @PostMapping
    public ApiResponse<Long> take(@RequestBody TakeReq req) {
        return ApiResponse.ok(takeRecordService.take(req));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable long id) {
        takeRecordService.cancel(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @RequestBody TakeUpdateReq req) {
        takeRecordService.update(id, req);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        takeRecordService.delete(id);
        return ApiResponse.ok();
    }
}
