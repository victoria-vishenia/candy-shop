package com.inn.inventoryservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
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
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.oauth2ResourceServer(config -> config.jwt(Customizer.withDefaults()))
                .oauth2Login(Customizer.withDefaults())
                .logout(Customizer.withDefaults())
                .authorizeHttpRequests(request -> request.requestMatchers("/error").permitAll()
                        .anyRequest().hasRole("ADMIN"));
        return httpSecurity.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(){
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter grantedConverter = new JwtGrantedAuthoritiesConverter();
        converter.setPrincipalClaimName("preferred_username");
     converter.setJwtGrantedAuthoritiesConverter(jwt ->
     {
         Collection < GrantedAuthority> authorities = grantedConverter.convert(jwt);
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
    public OAuth2UserService<OidcUserRequest, OidcUser> oAuth2UserService(){
       OidcUserService oidcUserService = new OidcUserService();
       return userRequest -> {
           OidcUser oidcUser = oidcUserService.loadUser(userRequest);
           List<String> roles = oidcUser.getClaimAsStringList("security_roles");

           List <GrantedAuthority> authorities = Stream.concat(oidcUser.getAuthorities().stream(),
                   roles.stream()
                   .filter(role -> role.startsWith("ROLE_"))
                   .map(SimpleGrantedAuthority::new)
                   .map(GrantedAuthority.class::cast))
                 .toList();

           return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
       };

    }
}
