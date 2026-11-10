package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;


import java.math.BigDecimal;
import java.time.Instant;

@Document("medicamento")
public class Medicamento {

    @Id
    private Long id;
    @Version private Long version;

    private String nome;

    private String codigoBarras;

    @DBRef private Categoria categoria;

    @DBRef private Fabricante fabricante;

    private TipoMedicamento tipo;

    @Field(targetType = FieldType.DECIMAL128) private BigDecimal preco;

    private int estoqueMinimo;

    private boolean ativo;

    private Instant criadoEm;

    protected Medicamento() {
        // exigido pelo mapeamento
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