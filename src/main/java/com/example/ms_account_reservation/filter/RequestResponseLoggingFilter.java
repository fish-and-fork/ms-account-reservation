package com.example.ms_account_reservation.filter;

import com.example.ms_account_reservation.config.LoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private final LoggingProperties props;

    private final List<PathPattern> includePatterns;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, props.getMaxBodyLength());
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        long start = System.currentTimeMillis();

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            try {
                logExchange(wrappedRequest, wrappedResponse, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("Failed to log HTTP exchange", e);
            } finally {
                wrappedResponse.copyBodyToResponse(); // без copyBodyToResponse будет пустой ответ
            }
        }
    }

    public RequestResponseLoggingFilter(LoggingProperties props) {
        this.props = props;
        PathPatternParser parser = new PathPatternParser();
        this.includePatterns = props.getIncludePaths().stream()
                .map(parser::parse)
                .toList();
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        PathContainer path = PathContainer.parsePath(extractRelativePath(request));
        return includePatterns.stream().noneMatch(p -> p.matches(path));
    }

    private void logExchange(ContentCachingRequestWrapper req,
                             ContentCachingResponseWrapper res,
                             long duration) {

        log.info("{} {} -> {} ({} ms)",
                req.getMethod(), req.getRequestURI(), res.getStatus(), duration);

        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug("""
                        >>> {} {}
                          headers: {}
                          body:    {}
                        <<< {}
                          headers: {}
                          body:    {}
                    """,
                req.getMethod(), req.getRequestURI(),
                filterHeaders(req),
                abbreviate(req.getContentAsByteArray()),
                res.getStatus(),
                filterHeaders(res),
                abbreviate(res.getContentAsByteArray()));
    }

    private String filterHeaders(HttpServletRequest req) {
        List<String> ignored = props.getIgnoredHeaders();
        return Collections.list(req.getHeaderNames()).stream()
                .filter(name -> ignored.stream().noneMatch(name::equalsIgnoreCase))
                .map(name -> name + "=" + req.getHeader(name))
                .collect(Collectors.joining(", "));
    }

    private String filterHeaders(HttpServletResponse res) {
        List<String> ignored = props.getIgnoredHeaders();
        return res.getHeaderNames().stream()
                .filter(name -> ignored.stream().noneMatch(name::equalsIgnoreCase))
                .map(name -> name + "=" + String.join("; ", res.getHeaders(name)))
                .collect(Collectors.joining(", "));
    }

    // Байты тела в строку с кодировкой и учётом max-body-length
    private String abbreviate(byte[] content) {
        if (content == null || content.length == 0) {
            return "";
        }
        int limit = props.getMaxBodyLength(); // в байтах
        if (content.length <= limit) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return new String(content, 0, limit, StandardCharsets.UTF_8)
                + "...[truncated " + (content.length - limit) + " bytes]";
    }

    private String extractRelativePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath.isEmpty() || !uri.startsWith(contextPath)) {
            return uri.isEmpty() ? "/" : uri;
        }
        String relative = uri.substring(contextPath.length());
        return relative.isEmpty() ? "/" : relative;
    }
}
