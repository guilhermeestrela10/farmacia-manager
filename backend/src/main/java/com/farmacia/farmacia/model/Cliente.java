package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("cliente")
public class Cliente {
    @Id private Long id;
    @Version private Long version;
    private String nome;
    private String telefone;
    private String email;
    private boolean ativo=true;
    protected Cliente() {}
    public Cliente(String nome,String telefone,String email) { this.nome=nome.trim(); this.telefone=telefone; this.email=email; }
    public void definirAtivo(boolean ativo) { this.ativo=ativo; }
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public boolean isAtivo() { return ativo; }
}
