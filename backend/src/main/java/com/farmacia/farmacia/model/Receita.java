package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.LocalDate;
import java.util.*;
@Document("receita")
public class Receita {
    @Id private Long id;
    @Version private Long version;
    @DBRef private Cliente cliente;
    private String prescritor;
    private String numero;
    private LocalDate emitidaEm;
    private LocalDate validadeAte;
    private String observacoes;
    private List<ItemReceita> itens=new ArrayList<>();
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
