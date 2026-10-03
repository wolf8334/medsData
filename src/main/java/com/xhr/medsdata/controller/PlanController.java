package com.xhr.medsdata.controller;

import com.xhr.medsdata.common.ApiResponse;
import com.xhr.medsdata.domain.Views.PlanDetail;
import com.xhr.medsdata.domain.Views.PlanItemView;
import com.xhr.medsdata.dto.Requests.NewVersionReq;
import com.xhr.medsdata.dto.Requests.PlanReq;
import com.xhr.medsdata.service.PlanService;
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
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ApiResponse<List<PlanDetail>> list(@RequestParam long personId) {
        return ApiResponse.ok(planService.listByPerson(personId));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlanDetail> get(@PathVariable long id) {
        return ApiResponse.ok(planService.getDetail(id));
    }

    @GetMapping("/effective")
    public ApiResponse<PlanDetail> effective(@RequestParam long personId,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(planService.effectiveDetail(personId, date == null ? LocalDate.now() : date));
    }

    @GetMapping("/{id}/items")
    public ApiResponse<List<PlanItemView>> items(@PathVariable long id) {
        return ApiResponse.ok(planService.items(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody PlanReq req) {
        return ApiResponse.ok(planService.create(req));
    }

    @PostMapping("/{id}/new-version")
    public ApiResponse<Long> newVersion(@PathVariable long id, @RequestBody(required = false) NewVersionReq req) {
        return ApiResponse.ok(planService.newVersion(id, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updatePending(@PathVariable long id, @RequestBody PlanReq req) {
        planService.updatePending(id, req);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        planService.delete(id);
        return ApiResponse.ok();
    }
}
