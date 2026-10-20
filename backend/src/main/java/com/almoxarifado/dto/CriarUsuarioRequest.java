package com.almoxarifado.dto;

import com.almoxarifado.model.Papel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CriarUsuarioRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Usuário é obrigatório")
    @Size(min = 3, max = 60, message = "O usuário deve ter entre 3 e 60 caracteres")
    private String usuario;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
    private String senha;

    @NotNull(message = "Papel é obrigatório")
    private Papel papel;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public Papel getPapel() { return papel; }
    public void setPapel(Papel papel) { this.papel = papel; }
}
