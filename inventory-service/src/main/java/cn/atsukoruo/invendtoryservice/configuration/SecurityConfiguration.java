package cn.atsukoruo.invendtoryservice.configuration;

import cn.atsukoruo.common.config.ErrorCodeConfig;
import cn.atsukoruo.common.config.TokenClaimsConfig;
import cn.atsukoruo.common.utils.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;


@Configuration
public class SecurityConfiguration  {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired
    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.formLogin(AbstractHttpConfigurer::disable);
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(AbstractHttpConfigurer::disable);


        http.authorizeHttpRequests(auth -> {
            auth.anyRequest().access(SecurityConfiguration::check);
        });

        http.addFilterBefore(jwtAuthenticationFilter, BasicAuthenticationFilter.class);
        return http.build();
    }

    static public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier,
                                              RequestAuthorizationContext requestAuthorizationContext) {
        Collection<? extends GrantedAuthority> authorities = authenticationSupplier.get().getAuthorities();
        boolean isGranted = authorities.stream().anyMatch(a -> a.getAuthority().equals("user") || a.getAuthority().equals("admin"));
        return new AuthorizationDecision(isGranted);
    }
}

@Slf4j
@Component
class JwtAuthenticationFilter extends OncePerRequestFilter {
    final private RedissonClient redissonClient;
    final private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    public JwtAuthenticationFilter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Value("${jwt.signing-key}")
    private String signingKey;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String jwt = request.getHeader("cn-atsukoruo-accessToken");


        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        SecretKey key = Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8));
        Claims claims = null;
        try {
            // 解析 JWT，获取用户信息
            claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();

            Integer userId = (Integer) claims.get(TokenClaimsConfig.USER_ID);
            String role = (String) claims.get(TokenClaimsConfig.ROLE);
            String client = (String) claims.get(TokenClaimsConfig.CLIENT);
            Integer batch = (Integer) claims.get(TokenClaimsConfig.BATCH);
            Integer version = (Integer) claims.get(TokenClaimsConfig.VERSION);
            long expiredTime = (long) claims.get(TokenClaimsConfig.EXPIRE_TIME);


            // 检测 AccessToken 是否过期
            if (System.currentTimeMillis() > expiredTime) {
                String str = objectMapper.writeValueAsString(Response.fail(ErrorCodeConfig.EXPIRED_ACCESS_TOKEN, "access token 已过期"));
                response.getOutputStream().println(str);
                response.setStatus(200);
                return;
            }


            // 检测 AccessToken 是否在黑名单中
            String strVersion = (String) redissonClient.getBucket(buildAccessVersionKey(userId,client)).get();
            String strBatch = (String) redissonClient.getBucket(buildAccessBatchKey(userId)).get();
            int bannedVersion = strVersion == null ? 0 : Integer.parseInt(strVersion);
            int bannedBatch = strBatch == null ? 0 : Integer.parseInt(strBatch);
            boolean isBanned = batch <= bannedBatch || version <= bannedVersion;

            if (isBanned) {
                Response<Object> result = Response.fail(
                        ErrorCodeConfig.BANNED_ACCESS_TOKEN,
                        "access token 已封禁");
                String str = objectMapper.writeValueAsString(result);
                response.setStatus(200);
                response.getOutputStream().println(str);
                return;
            }


            // 封装 Authentication 对象
            List<GrantedAuthority> grantedAuthorityList = new ArrayList<>();
            for (String r : role.split(",")) {
                grantedAuthorityList.add(new SimpleGrantedAuthority(r));
            }
            var auth = new UsernamePasswordAuthenticationToken(String.valueOf(userId), "", grantedAuthorityList);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 继续传播下去，要做权限检测的
            filterChain.doFilter(request, response);
        } catch (SignatureException | MalformedJwtException e) {
            log.error("捕获到伪造的 Token", e);
            objectMapper.writeValue(
                    response.getOutputStream(),
                    Response.fail(ErrorCodeConfig.FAKE_TOKEN, "token 是伪造的"));
            response.setStatus(200);
        } catch (Exception e) {
            log.error("未知的错误", e);
            objectMapper.writeValue(
                    response.getOutputStream(),
                    Response.fail(ErrorCodeConfig.UNKNOWN_ERROR, "遇到未知的错误"));
            response.setStatus(200);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().equals("/login");
    }

    private String buildAccessVersionKey(int userId, String client) {
        return "access:" + userId + ":" + client;
    }

    private String buildAccessBatchKey(int userId) {
        return "access:" + userId;
    }
}