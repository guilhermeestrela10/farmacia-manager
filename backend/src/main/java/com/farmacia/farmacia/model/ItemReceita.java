package com.farmacia.farmacia.model;
import jakarta.persistence.*;
@Entity @Table(name="item_receita")
public class ItemReceita {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="receita_id",nullable=false) private Receita receita;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="medicamento_id",nullable=false) private Medicamento medicamento;
    @Column(nullable=false) private int quantidade;
    protected ItemReceita() {}
    public ItemReceita(Receita receita,Medicamento medicamento,int quantidade) { this.receita=receita; this.medicamento=medicamento; this.quantidade=quantidade; }
    public Medicamento getMedicamento() { return medicamento; }
    public int getQuantidade() { return quantidade; }
}
