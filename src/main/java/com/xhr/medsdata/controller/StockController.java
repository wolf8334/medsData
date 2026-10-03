package com.xhr.medsdata.controller;

import com.xhr.medsdata.common.ApiResponse;
import com.xhr.medsdata.domain.StockLog;
import com.xhr.medsdata.domain.StockView;
import com.xhr.medsdata.dto.Requests.StockOpReq;
import com.xhr.medsdata.service.StockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public ApiResponse<List<StockView>> list() {
        return ApiResponse.ok(stockService.list());
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<StockView>> lowStock() {
        return ApiResponse.ok(stockService.lowStock());
    }

    @GetMapping("/logs")
    public ApiResponse<List<StockLog>> logs(@RequestParam(required = false) Long drugId,
                                            @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(stockService.logs(drugId, limit));
    }

    @PostMapping("/change")
    public ApiResponse<StockView> change(@RequestBody StockOpReq req) {
        return ApiResponse.ok(stockService.change(req));
    }
}
