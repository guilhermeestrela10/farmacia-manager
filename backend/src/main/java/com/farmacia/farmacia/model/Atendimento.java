package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.Instant;
@Document("atendimento")
public class Atendimento {
    @Id private Long id;
    @Version private Long version;
    @DBRef private Cliente cliente;
    private String profissional;
    private String observacoes;
    private Instant criadoEm;
    protected Atendimento() {}
    public Atendimento(Cliente cliente,String profissional,String observacoes) { this.cliente=cliente; this.profissional=profissional.trim(); this.observacoes=observacoes.trim(); this.criadoEm=Instant.now(); }
    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public String getProfissional() { return profissional; }
    public String getObservacoes() { return observacoes; }
    public Instant getCriadoEm() { return criadoEm; }
}
