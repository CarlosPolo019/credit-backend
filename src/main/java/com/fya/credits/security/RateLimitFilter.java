package com.fya.credits.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final long loginLimit;
  private final long createLimit;
  private final long listLimit;

  public RateLimitFilter(
      @Value("${app.rate-limit.login-per-minute}") long loginLimit,
      @Value("${app.rate-limit.create-credit-per-minute}") long createLimit,
      @Value("${app.rate-limit.list-credit-per-minute}") long listLimit) {
    this.loginLimit = loginLimit;
    this.createLimit = createLimit;
    this.listLimit = listLimit;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    LimitScope scope = resolveScope(request);
    if (scope == null) {
      filterChain.doFilter(request, response);
      return;
    }
    String key = scope.name() + ":" + resolveActor(request, scope);
    Bucket bucket = buckets.computeIfAbsent(key, ignored -> createBucket(scope.capacity));
    if (!bucket.tryConsume(1)) {
      response.setStatus(429);
      response.setContentType("application/json");
      response.getWriter().write("""
          {"status":429,"code":"TOO_MANY_REQUESTS","message":"Rate limit exceeded"}
          """);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private Bucket createBucket(long capacity) {
    return Bucket.builder()
        .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, Duration.ofMinutes(1)))
        .build();
  }

  private LimitScope resolveScope(HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();
    if (HttpMethod.POST.matches(method)
        && ("/api/v1/auth/login".equals(path) || "/api/v1/auth/register".equals(path))) {
      return new LimitScope("login", loginLimit);
    }
    if (HttpMethod.POST.matches(method) && "/api/v1/credits".equals(path)) {
      return new LimitScope("credit-create", createLimit);
    }
    if (HttpMethod.GET.matches(method) && "/api/v1/credits".equals(path)) {
      return new LimitScope("credit-list", listLimit);
    }
    return null;
  }

  private String resolveActor(HttpServletRequest request, LimitScope scope) {
    if ("login".equals(scope.name())) {
      return request.getRemoteAddr();
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null ? authentication.getName() : request.getRemoteAddr();
  }

  private record LimitScope(String name, long capacity) {
  }
}
