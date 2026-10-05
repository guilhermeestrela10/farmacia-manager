package com.farmacia.farmacia.model;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "lote")
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Column(name = "numero_lote", nullable = false, length = 50)
    private String numeroLote;

    @Column(nullable = false)
    private LocalDate validade;

    @Column(name = "quantidade_atual", nullable = false)
    private int quantidadeAtual;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Lote() {
        // exigido pelo JPA
    }

    public Lote(Medicamento medicamento, String numeroLote, LocalDate validade) {
        this.medicamento = medicamento;
        this.numeroLote = numeroLote;
        this.validade = validade;
        this.quantidadeAtual = 0;
        this.criadoEm = Instant.now();
    }

    public void adicionar(int quantidade) {
        this.quantidadeAtual += quantidade;
    }

    public void remover(int quantidade) {
        if (quantidade > this.quantidadeAtual) {
            throw new IllegalStateException("Quantidade maior que o saldo do lote.");
        }
        this.quantidadeAtual -= quantidade;
    }

    public Long getId() {
        return id;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public String getNumeroLote() {
        return numeroLote;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public int getQuantidadeAtual() {
        return quantidadeAtual;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}