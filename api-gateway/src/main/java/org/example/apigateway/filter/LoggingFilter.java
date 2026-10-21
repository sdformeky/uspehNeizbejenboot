package org.example.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// Пишет в лог каждый запрос, который прошёл через gateway: метод, путь, статус и время ответа
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getRawPath();

        // doFinally срабатывает, когда ответ уже отправлен (в том числе при ошибке)
        return chain.filter(exchange).doFinally(signal -> {
            long time = System.currentTimeMillis() - start;
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            log.info("{} {} -> {} ({} мс)", method, path, status != null ? status.value() : "-", time);
        });
    }

    // самый первый фильтр — чтобы время считалось за весь путь запроса
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
