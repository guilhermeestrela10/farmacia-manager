package com.farmacia.farmacia.model;
import jakarta.persistence.*;
@Entity @Table(name="configuracao")
public class Configuracao {
    @Id private Long id;
    @Column(name="nome_farmacia",nullable=false,length=100) private String nomeFarmacia;
    @Column(length=30) private String telefone;
    @Column(length=150) private String email;
    @Column(length=200) private String endereco;
    protected Configuracao() {}
    public void atualizar(String nomeFarmacia,String telefone,String email,String endereco) { this.nomeFarmacia=nomeFarmacia.trim(); this.telefone=telefone; this.email=email; this.endereco=endereco; }
    public String getNomeFarmacia() { return nomeFarmacia; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public String getEndereco() { return endereco; }
}
