package com.almoxarifado.dto;

public class LoginResponse {
    private String token;
    private Long id;
    private String nome;
    private String usuario;
    private String papel;
    private Long empresaId;
    private String empresaNome;

    public LoginResponse(String token, Long id, String nome, String usuario, String papel, Long empresaId, String empresaNome) {
        this.token = token;
        this.id = id;
        this.nome = nome;
        this.usuario = usuario;
        this.papel = papel;
        this.empresaId = empresaId;
        this.empresaNome = empresaNome;
    }

    public String getToken() { return token; }
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getUsuario() { return usuario; }
    public String getPapel() { return papel; }
    public Long getEmpresaId() { return empresaId; }
    public String getEmpresaNome() { return empresaNome; }
}
