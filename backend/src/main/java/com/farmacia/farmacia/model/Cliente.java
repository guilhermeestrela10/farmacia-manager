package com.farmacia.farmacia.model;
import jakarta.persistence.*;
@Entity @Table(name="cliente")
public class Cliente {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=100) private String nome;
    @Column(length=30) private String telefone;
    @Column(length=150) private String email;
    @Column(nullable=false) private boolean ativo=true;
    protected Cliente() {}
    public Cliente(String nome,String telefone,String email) { this.nome=nome.trim(); this.telefone=telefone; this.email=email; }
    public void definirAtivo(boolean ativo) { this.ativo=ativo; }
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public boolean isAtivo() { return ativo; }
}
