package com.almoxarifado.controller;

import com.almoxarifado.dto.LoginRequest;
import com.almoxarifado.dto.LoginResponse;
import com.almoxarifado.exception.RegraDeNegocioException;
import com.almoxarifado.model.Usuario;
import com.almoxarifado.repository.UsuarioRepository;
import com.almoxarifado.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager,
                           UsuarioRepository usuarioRepository,
                           JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsuario(), request.getSenha()));
        } catch (DisabledException ex) {
            throw new RegraDeNegocioException("Este usuário está desativado. Contate o administrador.");
        } catch (AuthenticationException ex) {
            throw new RegraDeNegocioException("Usuário ou senha inválidos");
        }

        Usuario usuario = usuarioRepository.findByUsuario(request.getUsuario())
                .orElseThrow(() -> new RegraDeNegocioException("Usuário ou senha inválidos"));

        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioException("Este usuário está desativado. Contate o administrador.");
        }

        String token = jwtService.gerarToken(usuario);
        return new LoginResponse(
                token,
                usuario.getId(),
                usuario.getNome(),
                usuario.getUsuario(),
                usuario.getPapel().name(),
                usuario.getEmpresa().getId(),
                usuario.getEmpresa().getNome()
        );
    }
}
