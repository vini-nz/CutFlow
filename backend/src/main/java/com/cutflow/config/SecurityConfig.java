package com.cutflow.config;

import com.cutflow.security.CutflowOidcUserService;
import com.cutflow.security.UsuarioDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autenticacao e autorizacao do CutFlow (ADR-0005).
 *
 * - Login local por e-mail/senha (BCrypt) estabelecendo SESSAO (cookie
 *   JSESSIONID httpOnly) - sem JWT: mais simples e mais seguro para uma SPA
 *   de mesmo dominio, sem token acessivel a JavaScript.
 * - Login Google via OAuth2/OIDC (habilitado so quando GOOGLE_CLIENT_ID esta
 *   configurado - o app sobe normalmente sem ele).
 * - CSRF por cookie legivel (XSRF-TOKEN) + header (X-XSRF-TOKEN), padrao para
 *   SPA; o CsrfCookieFilter forca o cookie a ser emitido a cada resposta.
 * - Qualquer rota fora de /auth/login|register|csrf e do fluxo OAuth exige
 *   autenticacao; o entry point devolve 401 (nunca redireciona para uma pagina
 *   de login de servidor, que nao existe numa SPA).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;
    private final CutflowOidcUserService cutflowOidcUserService;

    @Value("${cutflow.frontend-url}")
    private String frontendUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // Evita "user enumeration timing": responde sempre credenciais invalidas.
        provider.setHideUserNotFoundExceptions(true);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository) throws Exception {

        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        http
                .cors(cors -> {}) // usa o CorsConfigurationSource definido em WebConfig
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/csrf",
                                "/api/v1/auth/config").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Detalhes de um convite sao publicos (ADR-0006): mostram
                        // "Fulano te convidou..." antes do login. So o GET; o
                        // POST /aceitar tem um segmento a mais e continua exigindo sessao.
                        .requestMatchers(HttpMethod.GET, "/api/v1/convites/*").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado")))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        // Login Google so entra na cadeia quando ha client configurado - assim
        // o backend sobe em dev/local sem credenciais do Google.
        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(cutflowOidcUserService))
                    .successHandler((request, response, authentication) ->
                            response.sendRedirect(frontendUrl))
                    .failureHandler((request, response, exception) ->
                            response.sendRedirect(frontendUrl + "/login?erro=google")));
        }

        return http.build();
    }

    /**
     * Forca o token CSRF (carregado de forma "deferida" no Spring Security 6) a
     * ser resolvido, garantindo que o cookie XSRF-TOKEN seja enviado em toda
     * resposta - inclusive no GET /auth/csrf que a SPA chama antes de logar.
     */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
