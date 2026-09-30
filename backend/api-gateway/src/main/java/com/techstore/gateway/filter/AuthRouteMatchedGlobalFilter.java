package com.techstore.gateway.filter;

import com.techstore.gateway.routes.GatewayRouteStartupLogger;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthRouteMatchedGlobalFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(AuthRouteMatchedGlobalFilter.class);
    private final GatewayRouteStartupLogger routeStartupLogger;

    public AuthRouteMatchedGlobalFilter(GatewayRouteStartupLogger routeStartupLogger) {
        this.routeStartupLogger = routeStartupLogger;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String originalPath = exchange.getRequest().getURI().getRawPath();
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null && AuthRouteDiagnosticWebFilter.isDiagnosticPath(originalPath)) {
            exchange.getAttributes().put(AuthRouteDiagnosticWebFilter.AUTH_ROUTE_MATCHED_ATTRIBUTE, true);
            URI target = route.getUri();
            log.info("[AUTH-ROUTE] MATCHED routeId={} uri={} path={} originalPath={} matchedPath={} predicates={} filters={}",
                    route.getId(), GatewayRouteStartupLogger.safeUri(target), originalPath,
                    originalPath, originalPath, routeStartupLogger.predicatesFor(route.getId()),
                    routeStartupLogger.filtersFor(route.getId()));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}