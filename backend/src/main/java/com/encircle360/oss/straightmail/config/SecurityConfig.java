package com.encircle360.oss.straightmail.config;

import com.encircle360.oss.straightmail.tenant.filter.ApiKeyAuthenticationFilter;
import com.encircle360.oss.straightmail.tenant.filter.ApiKeyTenantResolutionFilter;
import com.encircle360.oss.straightmail.tenant.filter.JwtTenantResolutionFilter;
import com.encircle360.oss.straightmail.tenant.filter.NoAuthTenantResolutionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Spring Security configuration for straightmail.
 *
 * <p>Supports three mutually exclusive authentication modes, selected via the {@code auth.mode} property:
 *
 * <ul>
 *   <li><b>OIDC / JWT mode ({@code auth.mode=oidc}, default)</b>: configures an OAuth2 resource
 *       server. JWTs are validated against the OIDC issuer at {@code auth.issuer-uri} and the
 *       {@code aud} claim is verified against {@code spring.security.oauth2.resourceserver.jwt.audiences}.
 *       Roles are extracted from the {@code realm_access.roles} JWT claim and mapped to {@code ROLE_*}
 *       Spring authorities. The {@link JwtTenantResolutionFilter} is added after
 *       {@link BearerTokenAuthenticationFilter} to populate {@link com.encircle360.oss.straightmail.tenant.TenantContext}.</li>
 *   <li><b>API-key mode ({@code auth.mode=api-key})</b>: Spring Security authentication is disabled at
 *       the framework level. The {@link ApiKeyAuthenticationFilter} enforces API-key validation and
 *       sets {@code ROLE_ADMIN} for the global key or {@code ROLE_USER} for per-tenant keys, allowing
 *       {@code @PreAuthorize} to work uniformly across all modes.</li>
 *   <li><b>No-auth mode ({@code auth.mode=none})</b>: no authentication required. All anonymous
 *       requests are granted {@code ROLE_ADMIN}, so admin endpoints remain accessible. If the
 *       {@code database} profile is active, the {@link NoAuthTenantResolutionFilter} resolves the
 *       tenant from the {@code X-Tenant-ID} header.</li>
 * </ul>
 *
 * <p>Custom filters are registered via {@link FilterRegistrationBean} with {@code enabled=false} to
 * prevent Spring Boot from auto-registering them outside the security filter chain.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${api.prefix:/api}")
    private String apiPrefix;

    private final ObjectProvider<JwtTenantResolutionFilter> jwtTenantResolutionFilter;
    private final ObjectProvider<ApiKeyAuthenticationFilter> apiKeyAuthenticationFilter;
    private final ObjectProvider<ApiKeyTenantResolutionFilter> apiKeyTenantResolutionFilter;
    private final ObjectProvider<NoAuthTenantResolutionFilter> noAuthTenantResolutionFilter;

    /**
     * Creates the {@link JwtDecoder} used to validate incoming JWT tokens in OIDC mode.
     *
     * <p>Two modes are supported:
     * <ul>
     *   <li><b>Split-URL mode</b> (when {@code auth.jwk-uri} is set): fetches JWK keys from the
     *       internal {@code auth.jwk-uri} (e.g. a Docker-internal service name) while validating
     *       the {@code iss} claim against the public {@code auth.issuer-uri} (e.g. {@code localhost}).
     *       Use this when the backend container cannot reach the public Keycloak hostname.</li>
     *   <li><b>Standard mode</b> (when {@code auth.jwk-uri} is empty): performs full OIDC discovery
     *       via {@code auth.issuer-uri}, which must be reachable from the backend at startup.</li>
     * </ul>
     *
     * @param issuerUri the public OIDC issuer URI; used for {@code iss} claim validation
     * @param jwkUri    optional internal JWK set URI; when set, OIDC discovery is skipped
     * @return a {@link JwtDecoder} configured for the given issuer
     */
    @Bean
    @ConditionalOnProperty(name = "auth.mode", havingValue = "oidc", matchIfMissing = true)
    public JwtDecoder jwtDecoder(
            @Value("${auth.issuer-uri}") String issuerUri,
            @Value("${auth.jwk-uri:}") String jwkUri
    ) {

        if (StringUtils.hasText(jwkUri)) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkUri).build();
            OAuth2TokenValidator<Jwt> validators = JwtValidators.createDefaultWithIssuer(issuerUri);
            decoder.setJwtValidator(validators);
            return decoder;
        }

        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    /**
     * Creates a {@link JwtAuthenticationConverter} that maps Keycloak {@code realm_access.roles}
     * to Spring Security {@code ROLE_*} granted authorities.
     *
     * @return the configured {@link JwtAuthenticationConverter}
     */
    @Bean
    @ConditionalOnProperty(name = "auth.mode", havingValue = "oidc", matchIfMissing = true)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) return Collections.emptyList();
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null) return Collections.emptyList();
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });
        return converter;
    }

    /**
     * Configures the security filter chain for OIDC / JWT authentication mode ({@code auth.mode=oidc}).
     *
     * <p>All {@code /api/**} paths require authentication. JWT audience is validated against
     * {@code spring.security.oauth2.resourceserver.jwt.audiences}. The {@link JwtTenantResolutionFilter}
     * is injected after JWT validation when the {@code database} profile is active.
     *
     * @param http                       the {@link HttpSecurity} builder
     * @param jwtDecoder                 the JWT decoder bean
     * @param jwtAuthenticationConverter the authority converter bean
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the filter chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "auth.mode", havingValue = "oidc", matchIfMissing = true)
    public SecurityFilterChain securedFilterChain(HttpSecurity http, JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(apiPrefix + "/v1/info").permitAll()
                        .requestMatchers(apiPrefix + "/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                );

        JwtTenantResolutionFilter jwtFilter = jwtTenantResolutionFilter.getIfAvailable();
        if (jwtFilter != null) {
            http.addFilterAfter(jwtFilter, BearerTokenAuthenticationFilter.class);
        }

        return http.build();
    }

    /**
     * Configures the security filter chain for API-key authentication mode ({@code auth.mode=api-key}).
     *
     * <p>Spring Security authentication is disabled at the framework level. The
     * {@link ApiKeyAuthenticationFilter} enforces key validation and sets {@code ROLE_ADMIN}
     * (global key) or {@code ROLE_USER} (per-tenant key) in the {@link org.springframework.security.core.context.SecurityContext},
     * ensuring that {@code @PreAuthorize} annotations work uniformly across all modes.
     *
     * @param http the {@link HttpSecurity} builder
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the filter chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "auth.mode", havingValue = "api-key")
    public SecurityFilterChain apiKeyFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        ApiKeyAuthenticationFilter apiKeyFilter = apiKeyAuthenticationFilter.getIfAvailable();
        ApiKeyTenantResolutionFilter apiKeyTenantFilter = apiKeyTenantResolutionFilter.getIfAvailable();
        if (apiKeyFilter != null) {
            http.addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        }
        if (apiKeyTenantFilter != null && apiKeyFilter != null) {
            http.addFilterAfter(apiKeyTenantFilter, ApiKeyAuthenticationFilter.class);
        }

        return http.build();
    }

    /**
     * Configures the security filter chain for no-authentication mode ({@code auth.mode=none}).
     *
     * <p>All requests are permitted without any authentication. Anonymous requests are granted
     * {@code ROLE_ADMIN} so that {@code @PreAuthorize("hasRole('ADMIN')")} on admin endpoints
     * is satisfied. If the {@code database} profile is active, the {@link NoAuthTenantResolutionFilter}
     * resolves the tenant from the {@code X-Tenant-ID} header.
     *
     * @param http the {@link HttpSecurity} builder
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the filter chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "auth.mode", havingValue = "none")
    public SecurityFilterChain openFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .anonymous(anon -> anon.authorities("ROLE_ADMIN"))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        NoAuthTenantResolutionFilter noAuthFilter = noAuthTenantResolutionFilter.getIfAvailable();
        if (noAuthFilter != null) {
            http.addFilterBefore(noAuthFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    @ConditionalOnBean(JwtTenantResolutionFilter.class)
    public FilterRegistrationBean<JwtTenantResolutionFilter> jwtTenantResolutionFilterRegistration(
            JwtTenantResolutionFilter filter) {
        FilterRegistrationBean<JwtTenantResolutionFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    @ConditionalOnBean(ApiKeyAuthenticationFilter.class)
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyAuthenticationFilterRegistration(
            ApiKeyAuthenticationFilter filter) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    @ConditionalOnBean(ApiKeyTenantResolutionFilter.class)
    public FilterRegistrationBean<ApiKeyTenantResolutionFilter> apiKeyTenantResolutionFilterRegistration(
            ApiKeyTenantResolutionFilter filter) {
        FilterRegistrationBean<ApiKeyTenantResolutionFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    @ConditionalOnBean(NoAuthTenantResolutionFilter.class)
    public FilterRegistrationBean<NoAuthTenantResolutionFilter> noAuthTenantResolutionFilterRegistration(
            NoAuthTenantResolutionFilter filter) {
        FilterRegistrationBean<NoAuthTenantResolutionFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}
