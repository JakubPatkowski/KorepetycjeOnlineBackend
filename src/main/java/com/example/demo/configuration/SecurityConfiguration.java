package com.example.demo.configuration;

import com.example.demo.filter.JWTFilter;
import com.example.demo.service.AppUserDetailsService;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS configuration
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/user/register",
                                "/user/login",
                                "/user/verify-email",
                                "/user/access-token",
                                "/points/get-offers",
                                "/course/tags/",
                                "/course/get",
                                "/course/get-one/**",
                                "/course/tags/search",
                                "/course/get-info/**",
                                "/course/user/*",
                                "course/get-all",
                                "chapter/get-by-course/**",
                                "review/get/course/**",
                                "review/get/chapter/**"


                                ).permitAll()
                        //for testing all user
                        .requestMatchers("/course/create").hasAnyAuthority("USER")
                        .requestMatchers("/course/update").hasAnyAuthority("USER")
                        .requestMatchers("/course/data").hasAnyAuthority("USER")
                        .requestMatchers("/course/edit").hasAnyAuthority("USER")
                        .requestMatchers("/course/buy").hasAnyAuthority("USER")
                        .requestMatchers("/course/get-purchased").hasAnyAuthority("USER")
                        .requestMatchers("/chapter/get/**").hasAnyAuthority("USER")
                        .requestMatchers("/subchapter/get/**").hasAnyAuthority("USER")
//                        .requestMatchers("course/get-one/**").hasAnyAuthority("USER")

                        .requestMatchers("/user/change-email/initiate").hasAnyAuthority("USER")
                        .requestMatchers("/user/change-email/complete").hasAnyAuthority("USER")
                        .requestMatchers("/user/change-password/initiate").hasAnyAuthority("USER")
                        .requestMatchers("/user/change-password/complete").hasAnyAuthority("USER")
                        .requestMatchers("/user/resend-verification").hasAnyAuthority("USER")
                        .requestMatchers("/user/get").hasAnyAuthority("USER")
                        .requestMatchers("/user/upgrade-to-teacher").hasAnyAuthority("USER")
                        .requestMatchers("/user/logout").hasAnyAuthority("USER")

                        .requestMatchers("/user-profile/update").hasAnyAuthority("USER")
                        .requestMatchers("/user-profile/get").hasAnyAuthority("USER")

                        .requestMatchers("/points/buy").hasAnyAuthority("USER")

                        .requestMatchers("/review/add/course/**").hasAnyAuthority("USER")
                        .requestMatchers("/review/add/chapter/**").hasAnyAuthority("USER")
                        .requestMatchers("/review/delete/**").hasAnyAuthority("USER")
                        .requestMatchers("/review/user/course/**").hasAnyAuthority("USER")
                        .requestMatchers("/review/user/chapter/**").hasAnyAuthority("USER")

                        .anyRequest().authenticated()

                )

                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(new BCryptPasswordEncoder(10));
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        String hierarchy = "ADMIN > TEACHER \n TEACHER > VERIFIED \n VERIFIED > USER";
        roleHierarchy.setHierarchy(hierarchy);
        return roleHierarchy;
    }

    @Bean
    public DefaultWebSecurityExpressionHandler customWebSecurityExpressionHandler() {
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy());
        return expressionHandler;
    }

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

