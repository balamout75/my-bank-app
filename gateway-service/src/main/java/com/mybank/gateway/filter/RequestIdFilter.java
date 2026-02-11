package com.mybank.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Глобальный фильтр для добавления уникального Request ID ко всем запросам
 * 
 * Request ID используется для:
 * - Трассировки запросов через микросервисы
 * - Корреляции логов между сервисами
 * - Отладки проблем в production
 * 
 * Header: X-Request-ID
 */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        
        // Проверяем есть ли уже Request ID (от клиента)
        String incomingRequestId = headers.getFirst(REQUEST_ID_HEADER);

        // Если нет - генерируем новый
        final String requestId = (incomingRequestId == null || incomingRequestId.isEmpty())
                ? UUID.randomUUID().toString()
                : incomingRequestId;

        // Добавляем Request ID в request (для микросервисов)
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(builder -> builder.header(REQUEST_ID_HEADER, requestId))
                .build();
        
        // Добавляем Request ID в response (для клиента)
        modifiedExchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);
        
        return chain.filter(modifiedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;  // Выполняется первым
    }
}
