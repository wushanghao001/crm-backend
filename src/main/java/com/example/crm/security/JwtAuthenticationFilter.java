
package com.example.crm.security;

import com.example.crm.entity.User;
import com.example.crm.mapper.UserMapper;
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

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        System.out.println("=== JwtAuthenticationFilter: Request URI: " + request.getRequestURI());
        System.out.println("=== JwtAuthenticationFilter: Request Method: " + request.getMethod());
        
        String token = extractToken(request);
        System.out.println("=== JwtAuthenticationFilter: Token found: " + (token != null));
        System.out.println("=== JwtAuthenticationFilter: Token value: " + token);
        
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwtUtil.extractUsername(token);
                System.out.println("=== JwtAuthenticationFilter: Extracted username: " + username);
                
                if (username != null && jwtUtil.validateToken(token, username)) {
                    System.out.println("=== JwtAuthenticationFilter: Token validated successfully");
                    User user = userMapper.findByUsername(username);
                    System.out.println("=== JwtAuthenticationFilter: User found: " + (user != null));
                    
                    if (user != null) {
                        System.out.println("=== JwtAuthenticationFilter: User details - ID: " + user.getId() + ", Username: " + user.getUsername() + ", Role: " + user.getRole());
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
                        System.out.println("=== JwtAuthenticationFilter: Authentication set successfully");
                    }
                } else {
                    System.out.println("=== JwtAuthenticationFilter: Token validation failed");
                }
            } catch (Exception e) {
                System.out.println("=== JwtAuthenticationFilter: Exception: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("=== JwtAuthenticationFilter: Token is null or authentication already set");
        }
        
        System.out.println("=== JwtAuthenticationFilter: Continuing filter chain");
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
