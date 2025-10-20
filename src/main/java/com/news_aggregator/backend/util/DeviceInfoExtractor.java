package com.news_aggregator.backend.util;

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import eu.bitwalker.useragentutils.Version;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper methods for extracting user device metadata and IP addresses.
 */
public final class DeviceInfoExtractor {

    private static final String LOOPBACK_IPV6_LONG = "0:0:0:0:0:0:0:1";
    private static final String LOOPBACK_IPV6_SHORT = "::1";
    private static final String LOOPBACK_IPV4 = "127.0.0.1";
    private static final Pattern IPV4_PATTERN = Pattern.compile("(?<![\\d.])(\\d{1,3}(?:\\.\\d{1,3}){3})(?![\\d.])");

    private DeviceInfoExtractor() {
        // Utility class
    }

    /**
     * Resolve the client IP from common proxy headers or the raw remote address.
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return LOOPBACK_IPV4;
        }

        return firstValidHeaderValue(request, "X-Forwarded-For")
                .or(() -> firstValidHeaderValue(request, "X-Real-IP"))
                .orElseGet(request::getRemoteAddr);
    }

    /**
     * Produce a user-friendly summary of the device identified by the user-agent string.
     */
    public static String parseDeviceInfo(String userAgentHeader) {
        String userAgentString = userAgentHeader == null ? "" : userAgentHeader.trim();
        if (userAgentString.isEmpty()) {
            return "Unknown device";
        }

        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
        OperatingSystem os = userAgent.getOperatingSystem();
        Browser browser = userAgent.getBrowser();
        Version version = userAgent.getBrowserVersion();

        String osName = os != null ? os.getName() : "Unknown OS";
        String browserName = browser != null ? browser.getName() : "Unknown Browser";
        String browserVersion = version != null ? version.getVersion() : "";

        StringBuilder deviceInfo = new StringBuilder(osName);
        deviceInfo.append(" / ").append(browserName);
        if (!browserVersion.isBlank()) {
            deviceInfo.append(' ').append(browserVersion);
        }

        return deviceInfo.toString();
    }

    /**
     * Normalises loopback addresses to IPv4 for display purposes. Leaves other addresses intact.
     */
    public static String normalizeIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return LOOPBACK_IPV4;
        }

        String trimmed = ipAddress.trim();
        if (trimmed.equals(LOOPBACK_IPV6_LONG) || trimmed.equals(LOOPBACK_IPV6_SHORT)) {
            return LOOPBACK_IPV4;
        }

        Matcher matcher = IPV4_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return trimmed;
    }

    private static Optional<String> firstValidHeaderValue(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue == null || headerValue.isBlank()) {
            return Optional.empty();
        }

        for (String candidate : headerValue.split(",")) {
            if (candidate != null) {
                String trimmed = candidate.trim();
                if (!trimmed.isEmpty() && !"unknown".equalsIgnoreCase(trimmed)) {
                    return Optional.of(trimmed);
                }
            }
        }
        return Optional.empty();
    }
}
