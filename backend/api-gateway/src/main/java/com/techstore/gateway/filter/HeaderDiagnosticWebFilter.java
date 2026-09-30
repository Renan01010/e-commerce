package com.techstore.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class HeaderDiagnosticWebFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(HeaderDiagnosticWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var request = exchange.getRequest();
        var report = HeaderDiagnostic.analyze(request.getHeaders());
        var totalBytes = report.totalBytes();
        var status = HeaderDiagnostic.statusFor(totalBytes);
        var requestId = request.getHeaders().getFirst(CorrelationIdFilter.HEADER);
        var queryString = request.getURI().getRawQuery();

        logSummary(request.getMethod().name(), request.getURI().getRawPath(), queryString,
                requestId, report, status);
        report.headers().forEach(header -> logHeader(header));

        return chain.filter(exchange);
    }

    private static void logSummary(String method, String path, String queryString, String requestId,
                                   HeaderDiagnostic.Report report, String status) {
        String marker = switch (status) {
            case "OVER_LIMIT" -> "[HEADER-DIAGNOSTIC-OVER-LIMIT]";
            case "HIGH" -> "[HEADER-DIAGNOSTIC-HIGH]";
            case "WARNING" -> "[HEADER-DIAGNOSTIC-WARNING]";
            default -> "[HEADER-DIAGNOSTIC]";
        };
        log.info("{} requestId={} method={} path={} queryString={} headerCount={} totalHeadersBytes={} largestHeader={} largestHeaderBytes={} status={}",
                marker, safe(requestId), method, path, safe(queryString), report.headerCount(),
                report.totalBytes(), largestName(report), largestBytes(report), status);
    }

    private static void logHeader(HeaderDiagnostic.HeaderInfo header) {
        log.info(formatHeaderLog(header));
    }

    static String formatHeaderLog(HeaderDiagnostic.HeaderInfo header) {
        return "[HEADER] name=" + header.name() + " valueCount=" + header.valueCount()
                + " bytes=" + header.bytes();
    }

    private static String largestName(HeaderDiagnostic.Report report) {
        return report.largestHeader() == null ? "none" : report.largestHeader().name();
    }

    private static long largestBytes(HeaderDiagnostic.Report report) {
        return report.largestHeader() == null ? 0 : report.largestHeader().bytes();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }
}
