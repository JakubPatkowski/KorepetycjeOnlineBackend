package com.example.ekorki.configuration;

import com.example.ekorki.filter.JWTFilter;
import com.example.ekorki.service.AppUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private JWTFilter jwtFilter;

    // Define the security filter chain for HTTP requests
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // Configure Cross-Origin Resource Sharing (CORS)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Disable Cross-Site Request Forgery (CSRF) protection
            .csrf(AbstractHttpConfigurer::disable)
            // Define authorization rules for endpoints
            .authorizeHttpRequests(authorize -> authorize
                // Allow public access to specific endpoints
                .requestMatchers(
                    "/user/register",
                    "/user/login",
                    "/user/verify-email",
                    "/user/access-token",
                    "/points/get-offers",
                    "points/get-withdrawal-offers",
                    "/course/tags/",
                    "/course/get",
                    "/course/search",
                    "/course/get-best",
                    "/course/get-one/**",
                    "/course/tags/search",
                    "/course/get-info/**",
                    "/course/user/*",
                    "course/get-all",
                    "chapter/get-by-course/**",
                    "review/get/course/**",
                    "review/get/chapter/**",
                    "review/get/teacher/**",
                    "user-profile/get/**",
                    "user-profile/get-best",
                    "task/get/**"
                ).permitAll()
                // Restrict access to endpoints based on roles
                .requestMatchers("/course/create").hasAnyAuthority("USER")
                .requestMatchers("/course/update").hasAnyAuthority("USER")
                .requestMatchers("/course/data").hasAnyAuthority("USER")
                .requestMatchers("/course/edit").hasAnyAuthority("USER")
                .requestMatchers("/course/buy").hasAnyAuthority("USER")
                .requestMatchers("/course/get-purchased").hasAnyAuthority("USER")
                .requestMatchers("/course/can-review-teacher/").hasAnyAuthority("USER")
                .requestMatchers("/chapter/get/**").hasAnyAuthority("USER")
                .requestMatchers("/subchapter/get/**").hasAnyAuthority("USER")
                .requestMatchers("course/get-one/**").hasAnyAuthority("USER")

                .requestMatchers("/user/change-email/initiate").hasAnyAuthority("USER")
                .requestMatchers("/user/change-email/complete").hasAnyAuthority("USER")
                .requestMatchers("/user/change-password/initiate").hasAnyAuthority("USER")
                .requestMatchers("/user/change-password/complete").hasAnyAuthority("USER")
                .requestMatchers("/user/resend-verification").hasAnyAuthority("USER")
                .requestMatchers("/user/get").hasAnyAuthority("USER")
                .requestMatchers("/user/upgrade-to-teacher").hasAnyAuthority("USER")
                .requestMatchers("/user/logout").hasAnyAuthority("USER")

                .requestMatchers("/user-profile/update").hasAnyAuthority("USER")
                .requestMatchers("/user-profile/get-logged-in").hasAnyAuthority("USER")

                .requestMatchers("/points/buy").hasAnyAuthority("USER")
                .requestMatchers("/points/withdraw/").hasAnyAuthority("USER")

                .requestMatchers("/review/add/course/**").hasAnyAuthority("USER")
                .requestMatchers("/review/add/chapter/**").hasAnyAuthority("USER")
                .requestMatchers("/review/add/teacher/**").hasAnyAuthority("USER")
                .requestMatchers("/review/delete/**").hasAnyAuthority("USER")
                .requestMatchers("/review/user/course/**").hasAnyAuthority("USER")
                .requestMatchers("/review/user/chapter/**").hasAnyAuthority("USER")
                .requestMatchers("/review/user/teacher/**").hasAnyAuthority("USER")

                .requestMatchers("/payment/create-payment-intent").hasAnyAuthority("USER")
                .requestMatchers("/payment/webhook").hasAnyAuthority("USER")

                .requestMatchers("/task/create").hasAnyAuthority("USER")

                .requestMatchers("/payment-history").hasAnyAuthority("USER")

                // Require authentication for all other requests
                .anyRequest().authenticated()
            )
            // Use basic HTTP authentication
            .httpBasic(Customizer.withDefaults())
            // Configure stateless session management
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Add the JWT filter to the filter chain
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Define the authentication provider for user authentication
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(new BCryptPasswordEncoder(10));
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    // Define the authentication manager
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Define role hierarchy for role-based access control
    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        String hierarchy = "ADMIN > TEACHER \n TEACHER > VERIFIED \n VERIFIED > USER";
        roleHierarchy.setHierarchy(hierarchy);
        return roleHierarchy;
    }

    // Define custom expression handler to support role hierarchy in security expressions
    @Bean
    public DefaultWebSecurityExpressionHandler customWebSecurityExpressionHandler() {
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy());
        return expressionHandler;
    }

    // Define CORS configuration
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("Authorization");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

