package com.example.cvadvisorplatform.security;

import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // ===== XÁC ĐỊNH ROUTE PUBLIC (không bắt buộc token) =====
        boolean isPublicRoute =
                path.startsWith("/api/auth/")
                        || path.startsWith("/api/v1/ai/")
                        || path.startsWith("/api/public/")
                        || path.startsWith("/uploads/");

        String authHeader = request.getHeader("Authorization");

        // Với route public không có token -> cho đi tiếp luôn
        if (isPublicRoute && (authHeader == null || !authHeader.startsWith("Bearer "))) {
            filterChain.doFilter(request, response);
            return;
        }

        // Với route không public và không có token -> cho đi tiếp (Spring Security sẽ chặn sau)
        if (!isPublicRoute && (authHeader == null || !authHeader.startsWith("Bearer "))) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {

            String email = jwtService.extractUsername(token);

            if (
                    email != null
                            && SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null
            ) {

                User user =
                        userRepository.findByEmail(email)
                                .orElse(null);

                if (user != null) {

                    UserPrincipal principal =
                            new UserPrincipal(user);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    principal.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);
                }
            }

        } catch (Exception e) {

            logger.error(
                    "Could not set user authentication in security context",
                    e
            );
        }

        filterChain.doFilter(request, response);
    }
}