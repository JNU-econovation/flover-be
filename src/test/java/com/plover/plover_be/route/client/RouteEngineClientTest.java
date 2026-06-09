package com.plover.plover_be.route.client;

import com.plover.plover_be.route.dto.RouteDto;
import com.plover.plover_be.route.exception.RouteException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteEngineClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @DisplayName("비슷한 좌표의 성공 응답은 캐시되어 route engine을 한 번만 호출한다")
    @Test
    void get_route_caches_success_response_for_nearby_coordinates() throws IOException {
        // given
        AtomicInteger callCount = new AtomicInteger();
        RouteEngineClient client = createClient(exchange -> {
            callCount.incrementAndGet();
            respond(exchange, 200, routeResponseBody());
        });

        // when
        RouteDto.Response first = client.getRoute(37.5665, 126.9780, 2500, "PLOGGING");
        RouteDto.Response second = client.getRoute(37.56651, 126.97802, 2500, "PLOGGING");

        // then
        assertThat(first.routes()).hasSize(1);
        assertThat(second.routes()).hasSize(1);
        assertThat(callCount.get()).isEqualTo(1);
    }

    @DisplayName("route engine 실패 응답은 캐시하지 않는다")
    @Test
    void get_route_does_not_cache_failure_response() throws IOException {
        // given
        AtomicInteger callCount = new AtomicInteger();
        RouteEngineClient client = createClient(exchange -> {
            int currentCount = callCount.incrementAndGet();
            if (currentCount <= 2) {
                respond(exchange, 500, "{\"message\":\"error\"}");
                return;
            }
            respond(exchange, 200, routeResponseBody());
        });

        // when & then
        assertThatThrownBy(() -> client.getRoute(37.5665, 126.9780, 2500, "PLOGGING"))
                .isInstanceOf(RouteException.class);

        RouteDto.Response response = client.getRoute(37.5665, 126.9780, 2500, "PLOGGING");

        assertThat(response.routes()).hasSize(1);
        assertThat(callCount.get()).isEqualTo(3);
    }

    @DisplayName("mode는 기본값과 대문자로 정규화되어 캐시 키와 route engine 요청에 사용된다")
    @Test
    void get_route_normalizes_mode_for_cache_key_and_engine_request() throws IOException {
        // given
        AtomicInteger callCount = new AtomicInteger();
        AtomicReference<String> rawQuery = new AtomicReference<>();
        RouteEngineClient client = createClient(exchange -> {
            callCount.incrementAndGet();
            rawQuery.set(exchange.getRequestURI().getRawQuery());
            respond(exchange, 200, routeResponseBody());
        });

        // when
        RouteDto.Response first = client.getRoute(37.5665, 126.9780, 2500, "plogging");
        RouteDto.Response second = client.getRoute(37.5665, 126.9780, 2500, null);

        // then
        assertThat(first.routes()).hasSize(1);
        assertThat(second.routes()).hasSize(1);
        assertThat(callCount.get()).isEqualTo(1);
        assertThat(rawQuery.get()).contains("mode=PLOGGING");
    }

    private RouteEngineClient createClient(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/route", handler);
        server.start();
        return new RouteEngineClient("http://localhost:" + server.getAddress().getPort());
    }

    private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private String routeResponseBody() {
        return """
                [
                  {
                    "distanceMeter": 2500.0,
                    "timeMillis": 605000,
                    "encodedPath": "encodedPathData",
                    "ploggingScore": 98
                  }
                ]
                """;
    }
}
