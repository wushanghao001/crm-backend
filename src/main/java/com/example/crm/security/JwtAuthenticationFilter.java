package com.example.crm.security;

import com.example.crm.entity.User;
import com.example.crm.entity.UserSession;
import com.example.crm.mapper.UserMapper;
import com.example.crm.service.SessionService;
import com.example.crm.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final SessionService sessionService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserMapper userMapper, SessionService sessionService) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);
        boolean isLogoutRequest = request.getRequestURI().equals("/api/auth/logout");

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwtUtil.extractUsername(token);

                if (username != null && jwtUtil.validateToken(token, username)) {
                    User user = userMapper.findByUsername(username);

                    if (user != null) {
                        UserSession session = sessionService.getActiveSessionByToken(token);

                        if (session == null) {
                            if (!isLogoutRequest) {
                                response.setHeader("X-Session-Invalidated", "true");
                                response.setStatus(HttpServletResponse.SC_OK);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"code\":401,\"message\":\"你的账号已于" + java.time.LocalDateTime.now().toString().substring(0, 19) + "在其他设备登录，若非本人操作，请立即修改登录密码，保障账号安全！\",\"data\":null}");
                                return;
                            }
                        }

                        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

                        if (user.getPermissions() != null) {
                            for (String permission : user.getPermissions().split(",")) {
                                authorities.add(new SimpleGrantedAuthority(permission.trim()));
                            }
                        }

                        JwtAuthenticationToken authentication = new JwtAuthenticationToken(user, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // Token validation failed, continue to next filter
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
