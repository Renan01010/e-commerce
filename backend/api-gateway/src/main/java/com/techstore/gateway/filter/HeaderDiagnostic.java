package com.techstore.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpHeaders;

final class HeaderDiagnostic {
    static final long WARNING_THRESHOLD = 6_000;
    static final long HIGH_THRESHOLD = 7_500;
    static final long NETTY_THRESHOLD = 8_192;

    private HeaderDiagnostic() {}

    static Report analyze(HttpHeaders headers) {
        List<HeaderInfo> headerInfos = headers.entrySet().stream()
                .map(entry -> analyzeHeader(entry.getKey(), entry.getValue()))
                .toList();
        long totalBytes = headerInfos.stream().mapToLong(HeaderInfo::bytes).sum();
        HeaderInfo largestHeader = headerInfos.stream()
                .max(Comparator.comparingLong(HeaderInfo::bytes))
                .orElse(null);
        return new Report(headers.size(), totalBytes, largestHeader, headerInfos);
    }

    private static HeaderInfo analyzeHeader(String name, List<String> values) {
        long nameBytes = utf8Length(name);
        long valueBytes = values.stream().mapToLong(HeaderDiagnostic::utf8Length).sum();
        long separatorsBytes = values.size() * 4L;
        return new HeaderInfo(name, values.size(), valueBytes, nameBytes + valueBytes + separatorsBytes);
    }

    private static long utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    static String statusFor(long totalBytes) {
        if (totalBytes >= NETTY_THRESHOLD) return "OVER_LIMIT";
        if (totalBytes >= HIGH_THRESHOLD) return "HIGH";
        if (totalBytes >= WARNING_THRESHOLD) return "WARNING";
        return "NORMAL";
    }

    record HeaderInfo(String name, int valueCount, long valueBytes, long bytes) {}

    record Report(int headerCount, long totalBytes, HeaderInfo largestHeader, List<HeaderInfo> headers) {}
}
