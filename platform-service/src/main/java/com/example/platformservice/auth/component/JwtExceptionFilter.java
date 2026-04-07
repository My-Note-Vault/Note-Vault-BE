package com.example.platformservice.auth.component;

import com.example.common.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (UnauthorizedException e) {
            setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
        } catch (Exception e) {
            setErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", e.getMessage());
        }
    }

    private void setErrorResponse(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "code", code,
                        "message", message
                )
        );

        response.getWriter().write(body);
    }
}
