package org.example.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Проверка входа и прав для всех запросов через gateway.
// 1) Если есть заголовок Authorization: Bearer <JWT> — проверяет подпись токена (секрет jwt.secret из config-repo).
// 2) По методу и пути решает, кому запрос доступен: всем, вошедшим, ADMIN или INSTRUCTOR.
// 3) Передаёт сервисам, кто пришёл: X-User-Id и X-User-Roles. Сервисы по ним проверяют «своё ли это».
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final String PUBLIC = "PUBLIC";         // без входа
    private static final String USER = "USER";             // любой вошедший
    private static final String ADMIN = "ADMIN";
    private static final String INSTRUCTOR = "INSTRUCTOR"; // преподаватель или админ

    private final SecretKey key;

    public AuthFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getURI().getRawPath();

        // 1. Кто пришёл
        Long userId = null;
        List<String> roles = List.of();
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(header.substring(7))
                        .getPayload();
                userId = Long.valueOf(claims.getSubject());
                roles = claims.get("roles", List.class);
            } catch (JwtException | IllegalArgumentException e) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Сессия недействительна или истекла — войдите заново");
            }
        }

        // 2. Можно ли ему сюда
        String access = requiredAccess(method, path, request);
        if (!access.equals(PUBLIC)) {
            if (userId == null) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Нужно войти");
            }
            if (access.equals(ADMIN) && !roles.contains("ADMIN")) {
                return reject(exchange, HttpStatus.FORBIDDEN, "Доступно только администратору");
            }
            if (access.equals(INSTRUCTOR) && !roles.contains("INSTRUCTOR") && !roles.contains("ADMIN")) {
                return reject(exchange, HttpStatus.FORBIDDEN, "Доступно только преподавателю");
            }
        }

        // Чужие уведомления и обращения смотреть нельзя: ?userId= / ?authorId= должен совпадать с вошедшим
        String ownerParam = request.getQueryParams().getFirst("userId");
        if (ownerParam == null) {
            ownerParam = request.getQueryParams().getFirst("authorId");
        }
        boolean staff = roles.contains("ADMIN") || roles.contains("SUPPORT");
        if (ownerParam != null && userId != null && !staff && !ownerParam.equals(String.valueOf(userId))) {
            return reject(exchange, HttpStatus.FORBIDDEN, "Можно смотреть только свои данные");
        }

        // 3. Передаём сервисам, кто пришёл. Заголовки от клиента удаляем — подделать их нельзя
        final Long currentUserId = userId;
        final String currentRoles = String.join(",", roles);
        ServerHttpRequest mutated = request.mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Roles");
                    if (currentUserId != null) {
                        h.add("X-User-Id", String.valueOf(currentUserId));
                        h.add("X-User-Roles", currentRoles);
                    }
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    // Правила доступа — все в одном месте
    private String requiredAccess(String method, String path, ServerHttpRequest request) {
        if (method.equals("OPTIONS") || path.startsWith("/api-docs/")) {
            return PUBLIC;
        }
        // посты курса публикует преподаватель
        if (method.equals("POST") && path.matches("/api/courses/\\d+/posts")) {
            return INSTRUCTOR;
        }
        // создавать, менять и удалять курсы — только администратор; отзыв оставляет любой вошедший
        if (path.startsWith("/api/courses") && !method.equals("GET") && !path.matches("/api/courses/\\d+/reviews")) {
            return ADMIN;
        }
        // каталог, страница курса, проверка сертификата и FAQ открыты всем
        if (method.equals("GET") && (path.startsWith("/api/courses") || path.startsWith("/api/certificates")
                || path.equals("/api/support/faq"))) {
            return PUBLIC;
        }
        if (method.equals("POST") && (path.equals("/api/auth/login") || path.equals("/api/auth/register"))) {
            return PUBLIC;
        }
        // управление пользователями и студентами — только администратор
        if (path.startsWith("/api/auth/users")) {
            return ADMIN;
        }
        if (path.startsWith("/api/students")) {
            boolean allStudents = method.equals("GET") && path.equals("/api/students")
                    && request.getQueryParams().getFirst("userId") == null;
            if (allStudents || method.equals("PUT") || method.equals("DELETE")) {
                return ADMIN;
            }
        }
        if (method.equals("GET") && path.equals("/api/enrollments")
                && request.getQueryParams().getFirst("studentId") == null) {
            return ADMIN;
        }
        // эти запросы делают сами сервисы друг другу (напрямую, не через gateway); снаружи — только админ
        if (method.equals("POST") && (path.equals("/api/notifications") || path.equals("/api/certificates"))) {
            return ADMIN;
        }
        return USER;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        log.warn("{} {} -> {}: {}", exchange.getRequest().getMethod(), exchange.getRequest().getURI().getRawPath(),
                status.value(), message);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // тот же формат, что у ошибок сервисов (Problem Details): сайт читает поле detail
        String body = "{\"status\":" + status.value() + ",\"title\":\"" + status.getReasonPhrase()
                + "\",\"detail\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    // сразу после LoggingFilter: отклонённые запросы тоже попадут в лог
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
