package com.techstore.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class HeaderDiagnosticTest {
    @Test
    void classifiesSmallRequestAsNormal() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Host", "localhost:8080");
        headers.set("X-Correlation-ID", "request-123");

        HeaderDiagnostic.Report report = HeaderDiagnostic.analyze(headers);

        assertEquals(2, report.headerCount());
        assertEquals("NORMAL", HeaderDiagnostic.statusFor(report.totalBytes()));
    }

    @Test
    void measuresLargeAuthorizationWithoutExposingItsValue() {
        HttpHeaders headers = new HttpHeaders();
        String token = "Bearer " + "secret-token".repeat(100);
        headers.set("Authorization", token);

        HeaderDiagnostic.Report report = HeaderDiagnostic.analyze(headers);
        HeaderDiagnostic.HeaderInfo authorization = report.headers().getFirst();
        String logLine = HeaderDiagnosticWebFilter.formatHeaderLog(authorization);

        assertEquals(token.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                authorization.valueBytes());
        assertTrue(logLine.contains("name=Authorization"));
        assertTrue(logLine.contains("bytes=" + authorization.bytes()));
        assertFalse(logLine.contains(token));
        assertFalse(logLine.contains("secret-token"));
    }

    @Test
    void measuresCookieAndForwardedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "c".repeat(6_000));
        headers.set("Forwarded", "for=192.0.2.1;host=example.test;proto=https");
        headers.set("X-Forwarded-For", "192.0.2.1, 198.51.100.1");

        HeaderDiagnostic.Report report = HeaderDiagnostic.analyze(headers);

        assertEquals("Cookie", report.largestHeader().name());
        assertTrue(report.totalBytes() >= HeaderDiagnostic.WARNING_THRESHOLD);
        assertEquals("WARNING", HeaderDiagnostic.statusFor(report.totalBytes()));
    }

    @Test
    void countsMultipleValuesForOneHeader() {
        HttpHeaders headers = new HttpHeaders();
        String firstValue = "for=192.0.2.1";
        String secondValue = "for=198.51.100.1";
        headers.add("Forwarded", firstValue);
        headers.add("Forwarded", secondValue);

        HeaderDiagnostic.Report report = HeaderDiagnostic.analyze(headers);
        HeaderDiagnostic.HeaderInfo forwarded = report.headers().getFirst();

        assertEquals(1, report.headerCount());
        assertEquals(2, forwarded.valueCount());
        assertEquals(firstValue.length() + secondValue.length(), forwarded.valueBytes());
    }

    @Test
    void classifiesWarningHighAndOverLimitThresholds() {
        HttpHeaders warningHeaders = new HttpHeaders();
        warningHeaders.set("Cookie", "x".repeat(5_990));
        HttpHeaders highHeaders = new HttpHeaders();
        highHeaders.set("Cookie", "x".repeat(7_490));
        HttpHeaders overLimitHeaders = new HttpHeaders();
        overLimitHeaders.set("Cookie", "x".repeat(8_182));

        assertEquals(6_000, HeaderDiagnostic.analyze(warningHeaders).totalBytes());
        assertEquals(7_500, HeaderDiagnostic.analyze(highHeaders).totalBytes());
        assertEquals(8_192, HeaderDiagnostic.analyze(overLimitHeaders).totalBytes());
        assertEquals("WARNING", HeaderDiagnostic.statusFor(6_000));
        assertEquals("HIGH", HeaderDiagnostic.statusFor(7_500));
        assertEquals("OVER_LIMIT", HeaderDiagnostic.statusFor(8_192));
        assertEquals("NORMAL", HeaderDiagnostic.statusFor(5_999));
        assertNotNull(HeaderDiagnostic.statusFor(8_193));
    }
}
