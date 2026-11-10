package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Document("venda")
public class Venda {

    @Id
    private Long id;
    @Version private Long version;

    @DBRef private Usuario vendedor;

    private FormaPagamento formaPagamento;

    @Field(targetType = FieldType.DECIMAL128) private BigDecimal total;

    private StatusVenda status;

    private Instant criadaEm;

    private Instant canceladaEm;

    @DBRef private Usuario canceladaPor;

    private String motivoCancelamento;

    private List<ItemVenda> itens = new ArrayList<>();

    protected Venda() {
        // exigido pelo mapeamento
    }

    public Venda(Usuario vendedor, FormaPagamento formaPagamento) {
        this.vendedor = vendedor;
        this.formaPagamento = formaPagamento;
        this.total = BigDecimal.ZERO;
        this.status = StatusVenda.CONCLUIDA;
        this.criadaEm = Instant.now();
    }

    public void adicionarItem(Medicamento medicamento, int quantidade, BigDecimal precoUnitario) {
        ItemVenda item = new ItemVenda(this, medicamento, quantidade, precoUnitario);
        this.itens.add(item);
        this.total = this.total.add(item.getSubtotal());
    }

    public void cancelar(Usuario responsavel, String motivo) {
        this.status = StatusVenda.CANCELADA;
        this.canceladaEm = Instant.now();
        this.canceladaPor = responsavel;
        this.motivoCancelamento = motivo;
    }

    public Long getId() {
        return id;
    }

    public Usuario getVendedor() {
        return vendedor;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public StatusVenda getStatus() {
        return status;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getCanceladaEm() {
        return canceladaEm;
    }

    public Usuario getCanceladaPor() {
        return canceladaPor;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public List<ItemVenda> getItens() {
        return Collections.unmodifiableList(itens);
    }
}