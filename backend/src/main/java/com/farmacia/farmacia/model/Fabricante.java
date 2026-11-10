package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;


@Document("fabricante")
public class Fabricante {

    @Id
    private Long id;
    @Version private Long version;

    private String nome;

    protected Fabricante() {
        // exigido pelo mapeamento
    }

    public Fabricante(String nome) {
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}