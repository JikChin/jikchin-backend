package com.jikchin.jikchinbackend.global.security.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

  private static final int MINIMUM_SECRET_KEY_LENGTH = 32;

  @Bean
  public SecretKey jwtSecretKey(JwtProperties properties) {
    byte[] secret = Base64.getDecoder().decode(properties.secret());

    if (secret.length < MINIMUM_SECRET_KEY_LENGTH) {
      throw new IllegalStateException("JWT 비밀키는 256비트 이상이어야 합니다.");
    }

    return new SecretKeySpec(secret, MacAlgorithm.HS256.getName());
  }

  @Bean
  public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
    ImmutableSecret<SecurityContext> secret = new ImmutableSecret<>(jwtSecretKey);
    return new NimbusJwtEncoder(secret);
  }

  @Bean
  public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties properties) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
    return decoder;
  }
}
