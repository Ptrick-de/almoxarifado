package com.almoxarifado.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tempo de vida e renovação do token (desconexão por inatividade no servidor). */
class JwtServiceTest {

    private JwtService jwt;
    private UserDetails usuario;

    @BeforeEach
    void preparar() {
        jwt = new JwtService();
        ReflectionTestUtils.setField(jwt, "segredo", "segredo-de-teste-com-bem-mais-de-trinta-e-dois-bytes");
        ReflectionTestUtils.setField(jwt, "expiracaoMinutos", 30L);
        ReflectionTestUtils.setField(jwt, "renovarAposMinutos", 5L);
        usuario = User.withUsername("maria").password("x").roles("OPERADOR").build();
    }

    @Test
    void tokenRecemEmitidoEValidoENaoPrecisaSerRenovado() {
        String token = jwt.gerarToken(usuario);
        assertThat(jwt.tokenValido(token, usuario)).isTrue();
        assertThat(jwt.deveRenovar(token)).isFalse();
    }

    @Test
    void tokenQueVenceuPorInatividadeEhRejeitado() {
        ReflectionTestUtils.setField(jwt, "expiracaoMinutos", -1L);
        String token = jwt.gerarToken(usuario);
        assertThatThrownBy(() -> jwt.tokenValido(token, usuario)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenComMaisDeCincoMinutosDeIdadeDeveSerRenovado() {
        String token = jwt.gerarToken(usuario);
        ReflectionTestUtils.setField(jwt, "renovarAposMinutos", 0L);
        assertThat(jwt.deveRenovar(token)).isTrue();
    }
}
