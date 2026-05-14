package com.flover.flover_be.route.client;

import com.flover.flover_be.route.dto.RouteDto;
import com.flover.flover_be.route.exception.RouteErrorCode;
import com.flover.flover_be.route.exception.RouteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Component
public class RouteEngineClient {

    private static final int MAX_RETRIES = 3;

    private final RestClient restClient;

    public RouteEngineClient(
            @Value("${route.engine.url}") String routeEngineUrl,
            RestClient.Builder restClientBuilder) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = restClientBuilder
                .baseUrl(routeEngineUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public RouteDto.Response getRoute(double lat, double lon, int distance, String mode) {
        Exception lastException = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                log.info("Requesting route from engine (Attempt {}): lat={}, lon={}, distance={}",
                         i + 1, lat, lon, distance);

                return restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v1/route")
                                .queryParam("lat", lat)
                                .queryParam("lon", lon)
                                .queryParam("distance", distance)
                                .queryParam("mode", mode)
                                .build())
                        .retrieve()
                        .body(RouteDto.Response.class);

            } catch (ResourceAccessException | HttpServerErrorException e) {
                log.warn("Route Engine API call failed on attempt {}: {}", i + 1, e.getMessage());
                lastException = e;
                if (i < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RouteException(RouteErrorCode.ROUTE_CALCULATION_FAILED);
                    }
                }
            }
        }
        log.error("All {} attempts failed for Route Engine API.", MAX_RETRIES, lastException);
        throw new RouteException(RouteErrorCode.ROUTE_ENGINE_CONNECTION_FAILED);
    }
}
