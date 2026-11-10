package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.Instant;
@Document("dispensacao")
public class Dispensacao {
    @Id private Long id;
    @Version private Long version;
    @DBRef private Receita receita;
    @DBRef private Venda venda;
    private Instant criadaEm;
    protected Dispensacao() {}
    public Dispensacao(Receita receita,Venda venda) { this.receita=receita; this.venda=venda; this.criadaEm=Instant.now(); }
    public Long getId() { return id; }
    public Receita getReceita() { return receita; }
    public Venda getVenda() { return venda; }
    public Instant getCriadaEm() { return criadaEm; }
}
