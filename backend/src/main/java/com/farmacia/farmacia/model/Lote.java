package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;


import java.time.Instant;
import java.time.LocalDate;

@Document("lote")
public class Lote {

    @Id
    private Long id;
    @Version private Long version;

    @DBRef private Medicamento medicamento;

    private String numeroLote;

    private LocalDate validade;

    private int quantidadeAtual;

    private Instant criadoEm;

    protected Lote() {
        // exigido pelo mapeamento
    }

    public Lote(Medicamento medicamento, String numeroLote, LocalDate validade) {
        this.medicamento = medicamento;
        this.numeroLote = numeroLote;
        this.validade = validade;
        this.quantidadeAtual = 0;
        this.criadoEm = Instant.now();
    }

    public void adicionar(int quantidade) {
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        this.quantidadeAtual = Math.addExact(this.quantidadeAtual, quantidade);
    }

    public void remover(int quantidade) {
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
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
