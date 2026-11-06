package com.farmacia.farmacia.model;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;
@Entity @Table(name="receita")
public class Receita {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="cliente_id",nullable=false) private Cliente cliente;
    @Column(nullable=false,length=100) private String prescritor;
    @Column(nullable=false,length=80) private String numero;
    @Column(name="emitida_em",nullable=false) private LocalDate emitidaEm;
    @Column(name="validade_ate",nullable=false) private LocalDate validadeAte;
    @Column(length=2000) private String observacoes;
    @OneToMany(mappedBy="receita",cascade=CascadeType.ALL) private List<ItemReceita> itens=new ArrayList<>();
    protected Receita() {}
    public Receita(Cliente cliente,String prescritor,String numero,LocalDate emitidaEm,LocalDate validadeAte,String observacoes) {
        this.cliente=cliente; this.prescritor=prescritor.trim(); this.numero=numero.trim(); this.emitidaEm=emitidaEm; this.validadeAte=validadeAte; this.observacoes=observacoes;
    }
    public void adicionarItem(Medicamento medicamento,int quantidade) { itens.add(new ItemReceita(this,medicamento,quantidade)); }
    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public String getPrescritor() { return prescritor; }
    public String getNumero() { return numero; }
    public LocalDate getEmitidaEm() { return emitidaEm; }
    public LocalDate getValidadeAte() { return validadeAte; }
    public String getObservacoes() { return observacoes; }
    public List<ItemReceita> getItens() { return Collections.unmodifiableList(itens); }
}
