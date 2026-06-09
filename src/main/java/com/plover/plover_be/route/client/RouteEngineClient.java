package com.plover.plover_be.route.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.plover.plover_be.route.dto.RouteDto;
import com.plover.plover_be.route.exception.RouteErrorCode;
import com.plover.plover_be.route.exception.RouteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

@Slf4j
@Component
public class RouteEngineClient {

    private static final int MAX_RETRIES = 2;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final long RETRY_DELAY_MILLIS = 300L;
    private static final double COORDINATE_BUCKET_SIZE = 0.0005;
    private static final long ROUTE_CACHE_MAX_SIZE = 1_000L;
    private static final Duration ROUTE_CACHE_TTL = Duration.ofMinutes(5);

    private final RestClient restClient;
    private final Cache<RouteCacheKey, RouteDto.Response> routeCache = Caffeine.newBuilder()
            .maximumSize(ROUTE_CACHE_MAX_SIZE)
            .expireAfterWrite(ROUTE_CACHE_TTL)
            .build();

    public RouteEngineClient(@Value("${route.engine.url}") String routeEngineUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl(routeEngineUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public RouteDto.Response getRoute(double lat, double lon, int distance, String mode) {
        RouteCacheKey key = RouteCacheKey.from(lat, lon, distance, mode);
        return routeCache.get(key, ignored -> requestRouteFromEngine(lat, lon, distance, mode));
    }

    private RouteDto.Response requestRouteFromEngine(double lat, double lon, int distance, String mode) {
        Exception lastException = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                log.debug("Requesting routes from engine (Attempt {}): lat={}, lon={}, distance={}",
                        i + 1, lat, lon, distance);
                RouteDto.RouteInfo[] routeInfos = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v1/route")
                                .queryParam("lat", lat)
                                .queryParam("lon", lon)
                                .queryParam("distance", distance)
                                .queryParam("mode", mode)
                                .build())
                        .retrieve()
                        .body(RouteDto.RouteInfo[].class);
                return new RouteDto.Response(
                        routeInfos != null ? Arrays.asList(routeInfos) : Collections.emptyList()
                );

            } catch (ResourceAccessException | HttpServerErrorException e) {
                log.warn("Route Engine API call failed on attempt {}: {}", i + 1, e.getMessage());
                lastException = e;
                if (i < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MILLIS);
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

    private record RouteCacheKey(long latBucket, long lonBucket, int distance, String mode) {

        private static RouteCacheKey from(double lat, double lon, int distance, String mode) {
            return new RouteCacheKey(toBucket(lat), toBucket(lon), distance, mode);
        }

        private static long toBucket(double coordinate) {
            return Math.round(coordinate / COORDINATE_BUCKET_SIZE);
        }
    }
}
