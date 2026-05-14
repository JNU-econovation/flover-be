package com.flover.flover_be.route.client;

import com.flover.flover_be.route.dto.RouteDto;
import com.flover.flover_be.route.exception.RouteErrorCode;
import com.flover.flover_be.route.exception.RouteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Slf4j
@Component
public class RouteEngineClient {

    private final RestClient restClient;

    public RouteEngineClient(
            @Value("${route.engine.url}") String routeEngineUrl,
            RestClient.Builder restClientBuilder) {

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        
        this.restClient = restClientBuilder
                .baseUrl(routeEngineUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public RouteDto.Response getRoute(RouteDto.Request request) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                log.info("Requesting route from engine (Attempt {}): lat={}, lon={}, distance={}", 
                         i + 1, request.lat(), request.lon(), request.distance());
                         
                return restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v1/route")
                                .queryParam("lat", request.lat())
                                .queryParam("lon", request.lon())
                                .queryParam("distance", request.distance())
                                .queryParam("mode", request.mode())
                                .build())
                        .retrieve()
                        .body(RouteDto.Response.class);
                        
            } catch (RestClientException e) {
                log.warn("Route Engine API call failed on attempt {}: {}", i + 1, e.getMessage());
                if (i == maxRetries - 1) {
                    log.error("All {} attempts failed for Route Engine API.", maxRetries, e);
                    throw new RouteException(RouteErrorCode.ROUTE_ENGINE_CONNECTION_FAILED);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RouteException(RouteErrorCode.ROUTE_CALCULATION_FAILED);
                }
            }
        }
        throw new RouteException(RouteErrorCode.ROUTE_ENGINE_CONNECTION_FAILED);
    }
}
