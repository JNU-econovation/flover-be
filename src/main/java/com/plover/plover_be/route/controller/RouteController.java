package com.plover.plover_be.route.controller;

import com.plover.plover_be.route.client.RouteEngineClient;
import com.plover.plover_be.route.dto.RouteDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteEngineClient routeEngineClient;

    @GetMapping
    public ResponseEntity<RouteDto.Response> getPloggingRoute(@Valid @ModelAttribute RouteDto.Request request) {
        int distance = (int) ((request.time() / 60.0) * 5000);
        RouteDto.Response response = routeEngineClient.getRoute(
                request.lat(), request.lon(), distance, request.mode());
        return ResponseEntity.ok(response);
    }
}
