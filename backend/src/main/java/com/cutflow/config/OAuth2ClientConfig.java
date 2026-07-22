package com.cutflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Registra o cliente Google (OAuth2/OIDC) programaticamente e SOMENTE quando
 * ha credenciais (ADR-0005). Motivo: declarar a registration via
 * spring.security.oauth2.client.registration.google com client-id vazio faz o
 * Spring Boot falhar na inicializacao ("Client id ... must not be empty") em
 * vez de simplesmente ignorar - o que quebraria o app sempre que o Google nao
 * estivesse configurado (dev/local). Com @ConditionalOnExpression, sem
 * client-id nenhum bean e' criado, o OAuth2 do Boot fica inativo e apenas o
 * login local funciona.
 *
 * Usa CommonOAuth2Provider.GOOGLE, que ja traz escopos (openid/email/profile),
 * endpoints e o redirect-uri padrao {baseUrl}/login/oauth2/code/google.
 */
@Configuration
public class OAuth2ClientConfig {

    @Bean
    @ConditionalOnExpression("'${cutflow.google.client-id:}' != ''")
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${cutflow.google.client-id}") String clientId,
            @Value("${cutflow.google.client-secret:}") String clientSecret) {

        ClientRegistration google = CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        return new InMemoryClientRegistrationRepository(google);
    }
}
