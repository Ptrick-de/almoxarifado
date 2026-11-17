package com.almoxarifado.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String segredo;

    /** Tempo de vida de cada token. Como o token é renovado a cada uso, na prática é o tempo máximo de inatividade. */
    @Value("${jwt.expiracao-minutos:30}")
    private long expiracaoMinutos;

    /** Um token com mais de X minutos é trocado por um novo na resposta seguinte (sessão deslizante). */
    @Value("${jwt.renovar-apos-minutos:5}")
    private long renovarAposMinutos;

    private SecretKey chave() {
        // A chave precisa ter pelo menos 256 bits para o algoritmo HS256.
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                "jwt.secret é muito curto. Defina uma chave com pelo menos 32 caracteres (idealmente via variável de ambiente JWT_SECRET).");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String gerarToken(UserDetails usuario) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMinutos * 60_000);

        return Jwts.builder()
                .subject(usuario.getUsername())
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave())
                .compact();
    }

    /** Indica se o token já tem idade suficiente para ser trocado por um novo (renovação a cada uso). */
    public boolean deveRenovar(String token) {
        Date emitidoEm = extrairClaim(token, Claims::getIssuedAt);
        return emitidoEm.getTime() + renovarAposMinutos * 60_000 <= System.currentTimeMillis();
    }

    public String extrairUsuario(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public boolean tokenValido(String token, UserDetails usuario) {
        String nomeUsuario = extrairUsuario(token);
        return nomeUsuario.equals(usuario.getUsername()) && !tokenExpirado(token);
    }

    private boolean tokenExpirado(String token) {
        return extrairClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extrairClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(chave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
