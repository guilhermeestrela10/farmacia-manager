package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;


import java.time.Instant;

@Document("movimentacao_estoque")
public class MovimentacaoEstoque {

    @Id
    private Long id;
    @Version private Long version;

    @DBRef private Lote lote;

    private TipoMovimentacao tipo;

    private int quantidade;

    private String motivo;

    private String responsavel;

    private Instant dataHora;

    private Long vendaId;

    protected MovimentacaoEstoque() {
        // exigido pelo mapeamento
    }

    public MovimentacaoEstoque(Lote lote, TipoMovimentacao tipo, int quantidade,
                               String motivo, String responsavel) {
        this(lote, tipo, quantidade, motivo, responsavel, null);
    }

    public MovimentacaoEstoque(Lote lote, TipoMovimentacao tipo, int quantidade,
                               String motivo, String responsavel, Long vendaId) {
        this.lote = lote;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.motivo = motivo;
        this.responsavel = responsavel;
        this.vendaId = vendaId;
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

    public Long getVendaId() {
        return vendaId;
    }
}