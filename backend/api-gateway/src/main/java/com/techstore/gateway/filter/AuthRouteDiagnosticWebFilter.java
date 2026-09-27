package com.techstore.gateway.filter;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthRouteDiagnosticWebFilter implements WebFilter {
    static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    static final String AUTH_ROUTE_MATCHED_ATTRIBUTE = AuthRouteDiagnosticWebFilter.class.getName() + ".matched";
    private static final Logger log = LoggerFactory.getLogger(AuthRouteDiagnosticWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getRawPath();
        if (!isDiagnosticPath(path)) return chain.filter(exchange);

        String method = exchange.getRequest().getMethod().name();
        String requestId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (requestId == null || requestId.isBlank()) requestId = UUID.randomUUID().toString();
        String diagnosticRequestId = requestId;

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(CORRELATION_ID_HEADER, diagnosticRequestId))
                .build();
        ServerWebExchange diagnosticExchange = exchange.mutate().request(request).build();
        diagnosticExchange.getAttributes().put(AUTH_ROUTE_MATCHED_ATTRIBUTE, false);
        diagnosticExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, diagnosticRequestId);

        long startedAtNanos = System.nanoTime();
        AtomicBoolean noRouteLogged = new AtomicBoolean();
        AtomicBoolean responseLogged = new AtomicBoolean();

        log.info("[AUTH-ROUTE] INCOMING method={} path={} requestId={} timestamp={}",
                method, path, diagnosticRequestId, Instant.now());

        diagnosticExchange.getResponse().beforeCommit(() -> {
            HttpStatusCode status = diagnosticExchange.getResponse().getStatusCode();
            int statusCode = status == null ? 200 : status.value();
            if (statusCode == 404 && !routeMatched(diagnosticExchange)) {
                logNoRoute(method, path, diagnosticRequestId, noRouteLogged);
            }
            logResponse(method, path, diagnosticRequestId, statusCode, startedAtNanos, responseLogged);
            return Mono.empty();
        });

        return chain.filter(diagnosticExchange)
                .doOnError(error -> {
                    if (!routeMatched(diagnosticExchange) && isNotFound(error)) {
                        logNoRoute(method, path, diagnosticRequestId, noRouteLogged);
                    }
                    String routeId = routeId(diagnosticExchange);
                    log.warn("[AUTH-ROUTE] ROUTING ERROR method={} path={} requestId={} routeId={} exceptionClass={} message={}",
                            method, path, diagnosticRequestId, routeId, error.getClass().getName(),
                            sanitize(error.getMessage()));
                })
                .doFinally(signal -> {
                    if (!responseLogged.get()) {
                        HttpStatusCode status = diagnosticExchange.getResponse().getStatusCode();
                        int statusCode = status == null ? 500 : status.value();
                        if (statusCode == 404 && !routeMatched(diagnosticExchange)) {
                            logNoRoute(method, path, diagnosticRequestId, noRouteLogged);
                        }
                        logResponse(method, path, diagnosticRequestId, statusCode,
                                startedAtNanos, responseLogged);
                    }
                });
    }

    static boolean isDiagnosticPath(String path) {
        return path.equals("/api/auth") || path.startsWith("/api/auth/")
                || path.equals("/api/users") || path.startsWith("/api/users/");
    }

    static boolean routeMatched(ServerWebExchange exchange) {
        return Boolean.TRUE.equals(exchange.getAttribute(AUTH_ROUTE_MATCHED_ATTRIBUTE));
    }

    static String routeId(ServerWebExchange exchange) {
        Object route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route instanceof org.springframework.cloud.gateway.route.Route matchedRoute
                ? matchedRoute.getId() : "none";
    }

    private static boolean isNotFound(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ResponseStatusException responseStatusException) {
                return responseStatusException.getStatusCode().value() == 404;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void logNoRoute(String method, String path, String requestId, AtomicBoolean logged) {
        if (logged.compareAndSet(false, true)) {
            log.warn("[AUTH-ROUTE] NO_ROUTE method={} path={} requestId={}", method, path, requestId);
        }
    }

    private static void logResponse(String method, String path, String requestId, int status,
                                    long startedAtNanos, AtomicBoolean logged) {
        if (logged.compareAndSet(false, true)) {
            long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
            log.info("[AUTH-ROUTE] RESPONSE method={} path={} status={} durationMs={} requestId={}",
                    method, path, status, durationMs, requestId);
        }
    }

    static String sanitize(String value) {
        if (value == null || value.isBlank()) return "none";
        return value
                .replaceAll("(?i)Bearer\\s+[^\\s,;]+", "Bearer [REDACTED]")
                .replaceAll("(?i)(password|secret|token|authorization|cookie)=([^&\\s,;]+)", "$1=[REDACTED]")
                .replaceAll("(?i)(https?://)[^/@\\s]+:[^/@\\s]+@", "$1[REDACTED]@");
    }
}