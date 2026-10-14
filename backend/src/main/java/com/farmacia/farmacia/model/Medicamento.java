package com.farmacia.farmacia.model;

import java.math.BigDecimal;
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
@Table(name = "medicamento")
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "codigo_barras", length = 20)
    private String codigoBarras;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fabricante_id", nullable = false)
    private Fabricante fabricante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoMedicamento tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Medicamento() {
        // exigido pelo JPA
    }

    public Medicamento(String nome, String codigoBarras, Categoria categoria,
                       Fabricante fabricante, TipoMedicamento tipo, BigDecimal preco,
                       int estoqueMinimo) {
        this.nome = nome;
        this.codigoBarras = codigoBarras;
        this.categoria = categoria;
        this.fabricante = fabricante;
        this.tipo = tipo;
        this.preco = preco;
        this.estoqueMinimo = estoqueMinimo;
        this.ativo = true;
        this.criadoEm = Instant.now();
    }

    public void atualizar(String nome, String codigoBarras, Categoria categoria,
                          Fabricante fabricante, TipoMedicamento tipo, BigDecimal preco,
                          int estoqueMinimo) {
        this.nome = nome;
        this.codigoBarras = codigoBarras;
        this.categoria = categoria;
        this.fabricante = fabricante;
        this.tipo = tipo;
        this.preco = preco;
        this.estoqueMinimo = estoqueMinimo;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Fabricante getFabricante() {
        return fabricante;
    }

    public TipoMedicamento getTipo() {
        return tipo;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}