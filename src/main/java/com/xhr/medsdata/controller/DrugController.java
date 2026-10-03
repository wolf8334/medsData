package com.xhr.medsdata.controller;

import com.xhr.medsdata.common.ApiResponse;
import com.xhr.medsdata.domain.Drug;
import com.xhr.medsdata.dto.Requests.DrugReq;
import com.xhr.medsdata.service.DrugService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drugs")
public class DrugController {

    private final DrugService drugService;

    public DrugController(DrugService drugService) {
        this.drugService = drugService;
    }

    @GetMapping
    public ApiResponse<List<Drug>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(drugService.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<Drug> get(@PathVariable long id) {
        return ApiResponse.ok(drugService.get(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody DrugReq req) {
        return ApiResponse.ok(drugService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @RequestBody DrugReq req) {
        drugService.update(id, req);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> changeStatus(@PathVariable long id, @RequestParam String status) {
        drugService.changeStatus(id, status);
        return ApiResponse.ok();
    }
}
