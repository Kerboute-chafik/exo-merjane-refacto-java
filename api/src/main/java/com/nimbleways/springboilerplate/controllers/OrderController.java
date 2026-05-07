package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Positive;

@Slf4j
@Validated
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("{orderId}/processOrder")
    public ResponseEntity<ProcessOrderResponse> processOrder(@PathVariable @Positive Long orderId) {
        log.info("Processing order id={}", orderId);
        ProcessOrderResponse response = orderService.processOrder(orderId);
        log.info("Order id={} processed successfully", orderId);

        return ResponseEntity.ok(response);
    }
}
