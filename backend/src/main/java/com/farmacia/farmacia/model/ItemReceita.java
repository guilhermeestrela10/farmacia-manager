package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;


public class ItemReceita {
    @Id private Long id;
    @Transient private Receita receita;
    @DBRef private Medicamento medicamento;
    private int quantidade;
    protected ItemReceita() {}
    public ItemReceita(Receita receita,Medicamento medicamento,int quantidade) { this.receita=receita; this.medicamento=medicamento; this.quantidade=quantidade; }
    public Medicamento getMedicamento() { return medicamento; }
    public int getQuantidade() { return quantidade; }
}
