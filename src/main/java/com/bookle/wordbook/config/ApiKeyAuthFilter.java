package com.bookle.wordbook.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";
    private static final String PATH_PREFIX = "/wordbook/";

    @Value("${wordbook.api-key:}")
    private String expectedKey;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        if (expectedKey == null || expectedKey.isBlank()) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "WORDBOOK_API_KEY not configured");
            return;
        }

        String provided = request.getHeader(HEADER);
        if (!expectedKey.equals(provided)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "invalid api key");
            return;
        }

        chain.doFilter(request, response);
    }
}
