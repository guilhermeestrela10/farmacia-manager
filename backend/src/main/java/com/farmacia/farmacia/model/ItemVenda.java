package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;


import java.math.BigDecimal;


public class ItemVenda {

    @Id
    private Long id;

    @Transient private Venda venda;

    @DBRef private Medicamento medicamento;

    private int quantidade;

    @Field(targetType = FieldType.DECIMAL128) private BigDecimal precoUnitario;

    protected ItemVenda() {
        // exigido pelo mapeamento
    }

    public ItemVenda(Venda venda, Medicamento medicamento, int quantidade,
                     BigDecimal precoUnitario) {
        this.venda = venda;
        this.medicamento = medicamento;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public Long getId() {
        return id;
    }

    public Venda getVenda() {
        return venda;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}