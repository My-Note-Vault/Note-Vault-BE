package com.example.gateway;


import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;


@Component
public class JwtFilter implements Filter {

    private final JwtService jwtService;
    private final AntPathMatcher matcher = new AntPathMatcher();

    static final List<WhiteListRule> WHITE_LIST = List.of(
            new WhiteListRule("/api/v1/login", "*"),
            new WhiteListRule("/api/v1/signup", "*"),
            new WhiteListRule("/api/v1/note-info", "GET")
    );

    public  JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String method = req.getMethod();

        // 화이트리스트는 검증 패스
        if (isWhiteListed(path, method)) {
            chain.doFilter(request, response);
            return;
        }

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = header.substring(7);

        if (!jwtService.validateToken(token)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 필요하면 Request Attribute 전달
        req.setAttribute("X-User-Id", jwtService.getUserPK(token));
        chain.doFilter(request, response);
    }

    private boolean isWhiteListed(String uri, String method) {
        return WHITE_LIST.stream().anyMatch(rule ->
                (rule.getMethod().equals("*") || rule.getMethod().equalsIgnoreCase(method))
                        && matcher.match(rule.getPattern(), uri)
        );
    }


}
