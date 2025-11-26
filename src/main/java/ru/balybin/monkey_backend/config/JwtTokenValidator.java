package ru.balybin.monkey_backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

//Проверяем наличие и валидность токена,если всё ок,то аутентификация устанавливается в контекст спринга
public class JwtTokenValidator extends OncePerRequestFilter {

    private final SecretKey key;

    public JwtTokenValidator(String jwtSecret) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = request.getHeader(JwtConstant.JWT_HEADER); // Используем константу

        if (jwt != null && jwt.startsWith("Bearer ")) {
            try {
                jwt = jwt.substring(7);//удаление bearer

                Claims claims = Jwts.parser()
                        .verifyWith(key)//указываем ключ,которым проверяем подпись токена
                        .build()//возвращает готовый к работе объект
                        .parseSignedClaims(jwt)//разбор и проверка токена,возвращает заголовок,тело и подпись
                        .getPayload();//проверяем подпись токена и извлекаем тело

                String username = claims.get("email", String.class);
                String authorities = claims.get("authorities", String.class);

                //Если authorities null, устанавливаем пустой список
                List<GrantedAuthority> grantedAuthorities = (authorities != null)
                        ? AuthorityUtils.commaSeparatedStringToAuthorityList(authorities)
                        : AuthorityUtils.NO_AUTHORITIES;

                Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, grantedAuthorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);/* Создаем аутентификационный
                объект с именем пользователя и правами, затем устанавливаем его в контекст безопасности.*/

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid token received\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}