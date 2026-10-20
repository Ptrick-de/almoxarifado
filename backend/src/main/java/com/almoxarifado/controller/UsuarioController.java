package com.almoxarifado.controller;

import com.almoxarifado.dto.AlterarSenhaRequest;
import com.almoxarifado.dto.CriarUsuarioRequest;
import com.almoxarifado.dto.RedefinirSenhaRequest;
import com.almoxarifado.exception.RecursoNaoEncontradoException;
import com.almoxarifado.exception.RegraDeNegocioException;
import com.almoxarifado.model.Usuario;
import com.almoxarifado.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/eu")
    public Usuario meuUsuario(Authentication authentication) {
        return (Usuario) authentication.getPrincipal();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Usuario> listar(Authentication authentication) {
        Usuario logado = (Usuario) authentication.getPrincipal();
        return usuarioRepository.findAllByEmpresaId(logado.getEmpresa().getId());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Usuario> criar(@Valid @RequestBody CriarUsuarioRequest request, Authentication authentication) {
        Usuario logado = (Usuario) authentication.getPrincipal();

        if (usuarioRepository.existsByUsuario(request.getUsuario())) {
            throw new RegraDeNegocioException("Já existe um usuário com este nome de usuário");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setUsuario(request.getUsuario());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setPapel(request.getPapel());
        usuario.setAtivo(true);
        // A empresa é sempre a do administrador logado: nunca vem do corpo da requisição,
        // para impedir que um admin crie usuários em outra empresa.
        usuario.setEmpresa(logado.getEmpresa());

        return ResponseEntity.ok(usuarioRepository.save(usuario));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario alterarStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> corpo, Authentication authentication) {
        Usuario logado = (Usuario) authentication.getPrincipal();
        Usuario usuario = buscarDaMesmaEmpresa(id, logado);

        Boolean ativo = corpo.get("ativo");
        if (ativo != null) {
            usuario.setAtivo(ativo);
        }
        return usuarioRepository.save(usuario);
    }

    /** O próprio usuário troca a sua senha, informando a senha atual. */
    @PostMapping("/eu/senha")
    public ResponseEntity<Void> alterarMinhaSenha(@Valid @RequestBody AlterarSenhaRequest request, Authentication authentication) {
        Usuario logado = (Usuario) authentication.getPrincipal();
        if (!passwordEncoder.matches(request.getSenhaAtual(), logado.getSenha())) {
            throw new RegraDeNegocioException("A senha atual está incorreta");
        }
        logado.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        usuarioRepository.save(logado);
        return ResponseEntity.noContent().build();
    }

    /** O administrador define uma nova senha para um usuário da própria empresa (recuperação de senha). */
    @PatchMapping("/{id}/senha")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> redefinirSenha(@PathVariable Long id, @Valid @RequestBody RedefinirSenhaRequest request, Authentication authentication) {
        Usuario logado = (Usuario) authentication.getPrincipal();
        Usuario usuario = buscarDaMesmaEmpresa(id, logado);
        usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        usuarioRepository.save(usuario);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication authentication) {
        Usuario logado = (Usuario) authentication.getPrincipal();
        if (logado.getId().equals(id)) {
            throw new RegraDeNegocioException("Você não pode excluir o próprio usuário");
        }
        buscarDaMesmaEmpresa(id, logado);
        usuarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Garante que o usuário-alvo pertence à mesma empresa do administrador logado. */
    private Usuario buscarDaMesmaEmpresa(Long id, Usuario logado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
        if (!usuario.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado: " + id);
        }
        return usuario;
    }
}
