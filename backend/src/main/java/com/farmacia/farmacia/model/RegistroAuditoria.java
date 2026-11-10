package com.farmacia.farmacia.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;


import java.time.Instant;

@Document("auditoria")
public class RegistroAuditoria {
    @Id private Long id;
    @Version private Long version;
    private Instant dataHora;
    private TipoAuditoria tipo;
    private String usuario;
    private String acao;
    private String metodo;
    private String caminho;
    private int status;
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
