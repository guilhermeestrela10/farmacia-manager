package com.farmacia.farmacia.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "venda")
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 15)
    private FormaPagamento formaPagamento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusVenda status;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(name = "cancelada_em")
    private Instant canceladaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelada_por_id")
    private Usuario canceladaPor;

    @Column(name = "motivo_cancelamento", length = 200)
    private String motivoCancelamento;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL)
    private List<ItemVenda> itens = new ArrayList<>();

    protected Venda() {
        // exigido pelo JPA
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