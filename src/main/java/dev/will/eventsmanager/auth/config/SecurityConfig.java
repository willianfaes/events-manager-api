package dev.will.eventsmanager.auth.config;

import dev.will.eventsmanager.auth.SecurityFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration // Isso diz pro Spring "ei, essa class tem um Bean de configuraçao!". E se é Bean... O Spring gerencia pra gente!
@EnableWebSecurity // Isso ativa a segurança web, deixa nós Devs criarmos as NOSSAS PROPRIAS regras de acesso!
public class SecurityConfig {

    private final SecurityFilter securityFilter;

    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http

                .csrf(csrf -> csrf.disable()) // 1. Desliga proteção antiga de formulários (vamos usar Token JWT, então não precisamos disso)
                .cors(cors -> cors.configure(http)) // 2. Libera a API para receber requisições de outros sites/front-ends locais (evita o famoso erro de CORS)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 3. API com "Amnésia" (Stateless). O servidor não lembra de você, cada requisição tem que mandar o Token de novo.
                .authorizeHttpRequests(authorize -> authorize // 4. O Segurança da Porta (Regras de quem entra e quem fica de fora)
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll() // Deixa os erros do sistema passarem livremente
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll() // Qualquer um pode tentar logar (Catraca livre)
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll() // Qualquer um pode tentar criar conta (Catraca livre)
                        .anyRequest().authenticated()) // PARA TODAS AS OUTRAS ROTAS: Tem que apresentar o Token JWT (VIP apenas)
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build(); // 5. Monta e empacota todas essas regras
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
