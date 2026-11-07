package ru.nes.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }

        String finalTraceId = traceId;

        // ИСПРАВЛЕНИЕ: используем writableHttpHeaders для предотвращения UnsupportedOperationException
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> HttpHeaders.writableHttpHeaders(h).set("X-Request-Id", finalTraceId))
                .build();

        // Добавляем заголовок в ответ
        exchange.getResponse().getHeaders().add("X-Request-Id", traceId);

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }
}