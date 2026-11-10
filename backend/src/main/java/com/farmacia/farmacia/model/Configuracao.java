package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("configuracao")
public class Configuracao {
    @Id private Long id;
    @Version private Long version;
    private String nomeFarmacia;
    private String telefone;
    private String email;
    private String endereco;
    protected Configuracao() {}
    public void atualizar(String nomeFarmacia,String telefone,String email,String endereco) { this.nomeFarmacia=nomeFarmacia.trim(); this.telefone=telefone; this.email=email; this.endereco=endereco; }
    public String getNomeFarmacia() { return nomeFarmacia; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public String getEndereco() { return endereco; }
}
