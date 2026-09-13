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

@Configuration // This tells Spring "hey, this class has a configuration Bean!".
                    // And if it is a Bean... Spring manages it for us!
@EnableWebSecurity // This activates web security, lets us Devs create OUR OWN access rules!
public class SecurityConfig {

    private final SecurityFilter securityFilter;

    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http

                .csrf(csrf -> csrf.disable()) // 1. Disables old form protection
                                               // (we will use JWT Token, so we don't need this)
                .cors(cors -> cors.configure(http)) // 2. Allows API to receive requests from other sites/local front-ends
                                                    // (avoids the famous CORS error)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 3. API with "Amnesia" (Stateless). The server doesn't remember you, each request must send the Token again.
                .authorizeHttpRequests(authorize -> authorize // 4. The Door Security (Rules of who gets in and who stays out)
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll() // Let system errors pass freely
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll() // Anyone can try to log in (Open gate)
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll() // Anyone can try to create an account (Open gate)
                        .anyRequest().authenticated()) // FOR ALL OTHER ROUTES: Must present the JWT Token (VIP only)
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build(); // 5. Assembles and packages all these rules
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
