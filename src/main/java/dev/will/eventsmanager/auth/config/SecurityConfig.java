package dev.will.eventsmanager.auth.config;

import dev.will.eventsmanager.auth.security.CognitoAccessTokenValidator;
import dev.will.eventsmanager.auth.security.CognitoJwtAuthenticationConverter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // This tells Spring "hey, this class has a configuration Bean!".
                    // And if it is a Bean... Spring manages it for us!
@EnableWebSecurity // This activates web security, lets us Devs create OUR OWN access rules!
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                                                   RestAccessDeniedHandler restAccessDeniedHandler) throws Exception {
        return http

                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configure(http))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()
                        .requestMatchers(EndpointRequest.to("health", "prometheus")).permitAll()
                        // Checked by URL, before the body is read: a route added under /admin is closed to users
                        // even if its controller forgets @PreAuthorize.
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new CognitoJwtAuthenticationConverter()))
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .build();
    }

    // Spring Boot adds every OAuth2TokenValidator<Jwt> bean to the default checks of the decoder it builds from
    // the issuer-uri, so the decoder stays auto-configured.
    @Bean
    public OAuth2TokenValidator<Jwt> cognitoAccessTokenValidator() {
        return new CognitoAccessTokenValidator();
    }
}
