package com.farmacia.farmacia.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="dispensacao")
public class Dispensacao {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="receita_id",nullable=false) private Receita receita;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="venda_id",nullable=false,unique=true) private Venda venda;
    @Column(name="criada_em",nullable=false,updatable=false) private Instant criadaEm;
    protected Dispensacao() {}
    public Dispensacao(Receita receita,Venda venda) { this.receita=receita; this.venda=venda; this.criadaEm=Instant.now(); }
    public Long getId() { return id; }
    public Receita getReceita() { return receita; }
    public Venda getVenda() { return venda; }
    public Instant getCriadaEm() { return criadaEm; }
}
