package com.fya.credits.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  // A browser tab opened directly on the PDF URL can't attach an
  // Authorization header, so that one route also accepts the token as a
  // query param. Kept narrow on purpose: every other route still requires
  // the header.
  private static final String PDF_PATH_PATTERN = "/api/v1/credits/*/pdf";
  private final AntPathMatcher pathMatcher = new AntPathMatcher();
  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    String token = null;
    if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
      token = header.substring(7);
    } else if (pathMatcher.match(PDF_PATH_PATTERN, request.getRequestURI())) {
      String queryToken = request.getParameter("token");
      if (StringUtils.hasText(queryToken)) {
        token = queryToken;
      }
    }
    if (token != null) {
      try {
        JwtService.TokenClaims claims = jwtService.validate(token);
        String role = claims.role() != null ? claims.role() : "USER";
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                claims.subject(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (RuntimeException ignored) {
        SecurityContextHolder.clearContext();
      }
    }
    filterChain.doFilter(request, response);
  }
}
