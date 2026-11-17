package com.almoxarifado.security;

import com.almoxarifado.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Cabeçalho da resposta que traz o token renovado (exposto no CORS para o frontend ler). */
    public static final String HEADER_NOVO_TOKEN = "X-Novo-Token";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String nomeUsuario = jwtService.extrairUsuario(token);

            if (nomeUsuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails usuario = usuarioRepository.findByUsuario(nomeUsuario).orElse(null);

                if (usuario != null && jwtService.tokenValido(token, usuario)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Sessão deslizante: quem está usando o sistema ganha um token novo; quem parou, deixa o token vencer.
                    if (jwtService.deveRenovar(token)) {
                        response.setHeader(HEADER_NOVO_TOKEN, jwtService.gerarToken(usuario));
                    }
                }
            }
        } catch (Exception ex) {
            // Token inválido/expirado: apenas segue sem autenticar; o SecurityConfig cuidará do 401.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
