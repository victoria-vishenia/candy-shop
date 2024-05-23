package com.inn.orderservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.oauth2ResourceServer(oauth2ResourceServer ->
                        oauth2ResourceServer
                                .jwt(Customizer.withDefaults()))
                .oauth2Login(Customizer.withDefaults())
                .authorizeHttpRequests(request -> request.requestMatchers("/error").permitAll()
                        .requestMatchers("/order/all").hasRole("ADMIN")
                        .anyRequest().authenticated());

        return httpSecurity.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter grantedConverter = new JwtGrantedAuthoritiesConverter();
        converter.setPrincipalClaimName("sub");
        converter.setJwtGrantedAuthoritiesConverter(jwt ->
        {
            Collection<GrantedAuthority> authorities = grantedConverter.convert(jwt);
            List<String> roles = jwt.getClaimAsStringList("security_roles");

            return Stream.concat(authorities.stream(),
                            roles.stream()
                                    .filter(role -> role.startsWith("ROLE_"))
                                    .map(SimpleGrantedAuthority::new)
                                    .map(GrantedAuthority.class::cast))
                    .toList();
        });
        return converter;
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oAuth2UserService() {
        OidcUserService oidcUserService = new OidcUserService();

        return userRequest -> {
            OidcUser oidcUser = oidcUserService.loadUser(userRequest);
            List<String> roles = oidcUser.getClaimAsStringList("security_roles");

            List<GrantedAuthority> authorities = Stream.concat(oidcUser.getAuthorities().stream(),
                            roles.stream()
                                    .filter(role -> role.startsWith("ROLE_"))
                                    .map(SimpleGrantedAuthority::new)
                                    .map(GrantedAuthority.class::cast))
                    .toList();
            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }
}

//    @Bean
//    public JwtDecoder customJwtDecoder() throws Exception {
//        return new CustomJwtDecoder();
//    }
//}


//    @Bean
//    public JwtDecoder customJwtDecoder() {
//        return new CustomJwtDecoder();
//    }
//
//    static class CustomJwtDecoder implements JwtDecoder {
//        private final JwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation("https://localhost:8080/realms/candy-shop-realm");
//
//        @Override
//        public Jwt decode(String token) {
//            Jwt jwt = jwtDecoder.decode(token);
//            String issuer = jwt.getIssuer().toString();
//
//            if ("http://localhost:8080/realms/candy-shop-realm".equals(issuer)) {
//                try {
//                    // Modify the issuer claim
//                    Claims claims = Jwts.parserBuilder().build().parseClaimsJws(token).getBody();
//                    claims.put("iss", "https://keycloak:8080/realms/candy-shop-realm");
//
//                    byte[] secretBytes = "XzGzFrYej8LppcOFAxkeaucxtJlroVsZ".getBytes();
//
//// Generate a SecretKey from the byte array
//                    SecretKey secretKey = new SecretKeySpec(secretBytes, SignatureAlgorithm.HS256.getJcaName());
//
//                    // Reconstruct the token with modified claims
//                    token = Jwts.builder()
//                            .setClaims(claims)
//                            .signWith(secretKey)// Not signing it again, assuming it's already signed
//                            .compact();
//
//                    // Decode the modified token
//                    return jwtDecoder.decode(token);
//                } catch (JwtException e) {
//                    // Handle JWT parsing or validation errors
//                    throw new RuntimeException("Error decoding JWT token", e);
//                }
//            }
//            return jwt;
//        }
//    }
//
//    @Bean
//    public JwtDecoder customJwtDecoder() {
//        return new CustomJwtDecoder();
//    }
//
//    static class CustomJwtDecoder implements JwtDecoder {
//
//        @Override
//        public Jwt decode(String token) {
//
//            try {
//
//                byte[] secretBytes = "XzGzFrYej8LppcOFAxkeaucxtJlroVsZ".getBytes();
//
//
//                SecretKey secretKey = new SecretKeySpec(secretBytes, SignatureAlgorithm.HS256.getJcaName());
//
//                Claims claims = Jwts.parserBuilder()
//                        .setSigningKey(secretKey)
//                        .build()
//                        .parseClaimsJws(token)
//                        .getBody();
//
//                claims.remove("iss");
//
//                // Rebuild the token without `iss` if needed
//                String modifiedToken = Jwts.builder()
//                        .setClaims(claims)
//                        .signWith(secretKey, SignatureAlgorithm.HS256)
//                        .compact();
//
//                // Re-parse the token to create a Jwt object
//                claims = Jwts.parserBuilder()
//                        .setSigningKey(secretKey)
//                        .build()
//                        .parseClaimsJws(modifiedToken)
//                        .getBody();
//
//                // Decode the modified token
//                return new Jwt(modifiedToken, claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant(), claims, claims);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
//}


