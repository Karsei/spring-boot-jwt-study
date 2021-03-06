package kr.pe.karsei.springbootjwtstudy.configs.security;

import kr.pe.karsei.springbootjwtstudy.providers.JwtTokenProvider;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
public class SecurityConfiguration {
    /**
     * 인증 및 전역 관련
     */
    @Configuration
    @Order(97)
    static class AuthAdapter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity httpSecurity) throws Exception {
            httpSecurity
                    .antMatcher("/auth/**")
                    .csrf().disable()
                    .httpBasic().disable()
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .authorizeRequests()
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .anyRequest().permitAll()
                    .and()
                        .exceptionHandling()
                        .authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN));
        }

        /**
         * 정적 자원에 대한 Security 를 설정한다.
         * @param web WebSecurity 객체
         */
        @Override
        public void configure(WebSecurity web) {
            web
                    // 정적 자원은 Security 설정을 적용하지 않는다.
                    .ignoring().requestMatchers(PathRequest.toStaticResources().atCommonLocations());
        }

        /**
         * CORS 관련 설정을 변경한다.
         * @return {@link UrlBasedCorsConfigurationSource} 설정 객체
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.addAllowedOrigin("http://localhost:3000");
            configuration.addAllowedHeader("*");
            configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
            //configuration.addAllowedMethod("*");
            configuration.setAllowCredentials(true);

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }
    }

    /**
     * API 관련
     */
    @Configuration
    @Order(98)
    static class ApiAdapter extends WebSecurityConfigurerAdapter {
        private final JwtTokenProvider jwtTokenProvider;
        public ApiAdapter(JwtTokenProvider jwtTokenProvider) {
            this.jwtTokenProvider = jwtTokenProvider;
        }

        /**
         * Spring Boot 2.x 부터 자동 지원을 하지 않기 때문에 수동으로 등록한다.
         * <p>Component 로 사용하면 Spring Security Filter 와 통합되므로 사용자가 정의한 필터를 사용하기 위해 {@link JwtTokenProvider} 를 통해서 인증 후 {@link org.springframework.security.core.context.SecurityContextHolder} 를 사용한다.</p>
         * <p><a href="https://docs.spring.io/spring-security/reference/6.0/servlet/authentication/architecture.html#servlet-authentication-authenticationmanager">https://docs.spring.io/spring-security/reference/6.0/servlet/authentication/architecture.html#servlet-authentication-authenticationmanager</a></p>
         */
        @Bean
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }

        @Override
        protected void configure(HttpSecurity httpSecurity) throws Exception {
            httpSecurity
                    .antMatcher("/api/**")
                    .csrf().disable()
                    .httpBasic().disable()
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                        .authorizeRequests()
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .anyRequest().authenticated()
                    .and()
                        .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                        .exceptionHandling()
                        .authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN));
        }
    }

    /**
     * Actuator 관련
     */
    @Configuration
    @Order(99)
    static class ActuatorAdapter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity httpSecurity) throws Exception {
            httpSecurity
                    .antMatcher("/actuator/**")
                    .csrf().disable()
                    .httpBasic().and()
                    .authorizeRequests()
                        .antMatchers("/actuator/health").permitAll()
                        .antMatchers("/actuator/**").authenticated()
                    .and()
                        .exceptionHandling()
                        .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN));
        }
    }
}