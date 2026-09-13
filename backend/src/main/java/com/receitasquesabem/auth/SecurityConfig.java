package com.receitasquesabem.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String cookieSameSite;
    private final boolean cookieSecure;

    public SecurityConfig(@Value("${server.servlet.session.cookie.same-site:Lax}") String cookieSameSite,
                          @Value("${server.servlet.session.cookie.secure:false}") boolean cookieSecure) {
        this.cookieSameSite = cookieSameSite;
        this.cookieSecure = cookieSecure;
    }

    /**
     * BCrypt: gera um hash diferente de cada vez para a mesma password
     * (por causa do sal aleatório) e é propositadamente lento, para
     * tornar caro tentar adivinhar passwords em força bruta.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** Onde o contexto de segurança é guardado: na sessão HTTP. */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // O cookie do CSRF NÃO é HttpOnly de propósito: tem de poder ser lido.
        // Como o frontend está noutro domínio, ele não consegue ler o cookie
        // diretamente — por isso o token também é devolvido por GET /api/auth/csrf.
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie
                .sameSite(cookieSameSite)
                .secure(cookieSecure));

        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                .cors(cors -> {
                })
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/api/auth/csrf",
                                "/api/auth/login", "/api/auth/register")
                        .permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                // Sem isto, o Spring responde com uma página de login em HTML.
                // Numa API queremos apenas o código 401.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }
}
