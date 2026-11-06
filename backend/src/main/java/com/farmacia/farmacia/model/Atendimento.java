package com.farmacia.farmacia.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="atendimento")
public class Atendimento {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="cliente_id",nullable=false) private Cliente cliente;
    @Column(nullable=false,length=100) private String profissional;
    @Column(nullable=false,length=2000) private String observacoes;
    @Column(name="criado_em",nullable=false,updatable=false) private Instant criadoEm;
    protected Atendimento() {}
    public Atendimento(Cliente cliente,String profissional,String observacoes) { this.cliente=cliente; this.profissional=profissional.trim(); this.observacoes=observacoes.trim(); this.criadoEm=Instant.now(); }
    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public String getProfissional() { return profissional; }
    public String getObservacoes() { return observacoes; }
    public Instant getCriadoEm() { return criadoEm; }
}
