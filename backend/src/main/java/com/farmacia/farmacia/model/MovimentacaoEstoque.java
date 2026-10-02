package com.farmacia.farmacia.model;

import java.time.Instant;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacao tipo;

    @Column(nullable = false)
    private int quantidade;

    @Column(length = 200)
    private String motivo;

    @Column(nullable = false, length = 100)
    private String responsavel;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private Instant dataHora;

    protected MovimentacaoEstoque() {
        // exigido pelo JPA
    }

    public MovimentacaoEstoque(Lote lote, TipoMovimentacao tipo, int quantidade,
                               String motivo, String responsavel) {
        this.lote = lote;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.motivo = motivo;
        this.responsavel = responsavel;
        this.dataHora = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Lote getLote() {
        return lote;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public Instant getDataHora() {
        return dataHora;
    }
}