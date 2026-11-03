package com.farmacia.farmacia.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auditoria")
public class RegistroAuditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "data_hora", nullable = false, updatable = false)
    private Instant dataHora;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TipoAuditoria tipo;
    @Column(nullable = false, length = 50, updatable = false)
    private String usuario;
    @Column(nullable = false, length = 500, updatable = false)
    private String acao;
    @Column(nullable = false, length = 10, updatable = false)
    private String metodo;
    @Column(nullable = false, length = 200, updatable = false)
    private String caminho;
    @Column(nullable = false, updatable = false)
    private int status;
    @Column(nullable = false, length = 45, updatable = false)
    private String ip;

    protected RegistroAuditoria() {}

    public RegistroAuditoria(TipoAuditoria tipo, String usuario, String acao,
                             String metodo, String caminho, int status, String ip) {
        this.dataHora = Instant.now();
        this.tipo = tipo;
        this.usuario = limitar(usuario, 50);
        this.acao = limitar(acao, 500);
        this.metodo = limitar(metodo, 10);
        this.caminho = limitar(caminho, 200);
        this.status = status;
        this.ip = limitar(ip, 45);
    }

    private static String limitar(String valor, int maximo) {
        if (valor == null) return "";
        return valor.substring(0, Math.min(valor.length(), maximo));
    }

    public Long getId() { return id; }
    public Instant getDataHora() { return dataHora; }
    public TipoAuditoria getTipo() { return tipo; }
    public String getUsuario() { return usuario; }
    public String getAcao() { return acao; }
    public String getMetodo() { return metodo; }
    public String getCaminho() { return caminho; }
    public int getStatus() { return status; }
    public String getIp() { return ip; }
}
