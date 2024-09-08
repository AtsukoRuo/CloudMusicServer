package cn.atsukoruo.societyservice.Configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


@Configuration
public class SecurityConfiguration  {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.formLogin(AbstractHttpConfigurer::disable);
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(AbstractHttpConfigurer::disable);

        // 放行所有端点的授权
        http.authorizeHttpRequests(auth -> {
            auth.anyRequest().access(SecurityConfiguration::check);
        });
        return http.build();
    }

    static public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier,
                                              RequestAuthorizationContext requestAuthorizationContext) {
        Collection<? extends GrantedAuthority> authorities = authenticationSupplier.get().getAuthorities();
        Map<String, String> variables = requestAuthorizationContext.getVariables();
        HttpServletRequest request = requestAuthorizationContext.getRequest();

        boolean isGranted = true;
        return new AuthorizationDecision(isGranted);
    }
}

@Component
class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.signing.key}")
    private String signingKey;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String jwt = request.getHeader("Authorization");
        SecretKey key = Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8));
        // 解析 JWT，获取用户信息
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt)
                .getBody();

        // 这里可以实现一个检测的逻辑
        String username = String.valueOf(claims.get("username"));
        GrantedAuthority authority = new SimpleGrantedAuthority("user");

        // 将 Authentication 封装到 SecurityContextHolder 中
        // 这样在本次请求中，可以让 Controller 获取到用户的信息
        // 在权限检测中，也要从 SecurityContextHolder 中获取 Authentication
        var auth = new UsernamePasswordAuthenticationToken(username, null, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 继续传播下去，要做权限检测的
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().equals("/login");
    }
}