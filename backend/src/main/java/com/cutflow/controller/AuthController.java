package com.cutflow.controller;

import com.cutflow.dto.auth.LoginRequest;
import com.cutflow.dto.auth.RegistroRequest;
import com.cutflow.dto.auth.SessaoResponse;
import com.cutflow.dto.auth.UsuarioResponse;
import com.cutflow.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Autenticacao local (ADR-0005). O login Google nao passa por aqui - e' tratado
 * pelo fluxo OAuth2 do Spring Security (/oauth2/authorization/google). O logout
 * tambem e' do Spring Security (POST /api/v1/auth/logout).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;

    // Persiste o SecurityContext na sessao HTTP apos o login manual - no Spring
    // Security 6 isso deixou de ser automatico.
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    /** Emite o cookie XSRF-TOKEN; a SPA chama antes do primeiro POST. */
    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void csrf() {
        // O CsrfCookieFilter (SecurityConfig) ja forcou o cookie nesta resposta.
    }

    /** Flags de configuracao publicas para a SPA (ex: mostrar botao do Google). */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("googleHabilitado", clientRegistrationRepository.getIfAvailable() != null);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse register(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    public SessaoResponse login(@Valid @RequestBody LoginRequest request,
                                HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        AuthService.normalizarEmail(request.email()), request.senha()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return authService.sessaoAtual();
    }

    @GetMapping("/me")
    public SessaoResponse me() {
        return authService.sessaoAtual();
    }
}
