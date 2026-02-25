package com.yuz.toplinks.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yuz.toplinks.dto.UspsRateRequest;
import com.yuz.toplinks.dto.UspsRateResponse;
import com.yuz.toplinks.service.UspsService;

/**
 * USPS 运费查询 REST 控制器。
 * <p>
 * 端点: POST /api/shipping/usps/rate
 * </p>
 */
@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    private final UspsService uspsService;

    public ShippingController(UspsService uspsService) {
        this.uspsService = uspsService;
    }

    /**
     * 查询 USPS 运费。
     *
     * @param request 运费请求参数
     * @return 运费结果
     */
    @PostMapping("/usps/rate")
    public ResponseEntity<UspsRateResponse> calculateUspsRate(@Validated @RequestBody UspsRateRequest request) {
        UspsRateResponse response = uspsService.calculateRate(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
