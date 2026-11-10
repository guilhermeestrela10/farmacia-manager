package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;


import java.time.Instant;

@Document("usuario")
public class Usuario {

    @Id
    private Long id;
    @Version private Long version;

    private String nome;

    private String login;

    private String senhaHash;

    private Perfil perfil;

    private boolean ativo;

    private Instant criadoEm;

    private Instant senhaAlteradaEm;

    protected Usuario() {
        // exigido pelo mapeamento
    }

    public Usuario(String nome, String login, String senhaHash, Perfil perfil) {
        this.nome = nome;
        this.login = login;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = true;
        this.criadoEm = Instant.now();
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void definirSenha(String novoHash) {
        this.senhaHash = novoHash;
        this.senhaAlteradaEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getLogin() {
        return login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getSenhaAlteradaEm() {
        return senhaAlteradaEm;
    }
}