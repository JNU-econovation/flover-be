package com.flover.flover_be.route.controller;

import com.flover.flover_be.route.client.RouteEngineClient;
import com.flover.flover_be.route.dto.RouteDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteEngineClient routeEngineClient;

    @GetMapping
    public ResponseEntity<RouteDto.Response> getPloggingRoute(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam int time,
            @RequestParam(defaultValue = "PLOGGING") String mode) {
        
        int distance = (int) ((time / 60.0) * 5000);
        
        RouteDto.Request request = new RouteDto.Request(lat, lon, distance, mode);
        RouteDto.Response response = routeEngineClient.getRoute(request);
        
        return ResponseEntity.ok(response);
    }
}
