package com.farmacia.farmacia.security;

public final class DescritorDeAcao {
    private DescritorDeAcao() {}

    public static String descrever(String metodo, String caminho) {
        if ("POST".equals(metodo)) {
            return switch (caminho) {
                case "/vendas" -> "Registrar venda";
                case "/estoque/entradas" -> "Registrar entrada de estoque";
                case "/estoque/saidas" -> "Registrar saída de estoque";
                case "/estoque/ajustes" -> "Ajustar estoque";
                case "/estoque/perdas" -> "Registrar perda de estoque";
                case "/usuarios" -> "Criar usuário";
                case "/medicamentos" -> "Criar medicamento";
                case "/categorias" -> "Criar categoria";
                case "/fabricantes" -> "Criar fabricante";
                case "/auth/senha" -> "Trocar própria senha";
                default -> acaoComId(metodo, caminho);
            };
        }
        return acaoComId(metodo, caminho);
    }

    private static String acaoComId(String metodo, String caminho) {
        String[] partes = caminho.split("/");
        if (partes.length >= 3 && partes[2].matches("\\d+")) {
            String id = partes[2];
            if (partes.length == 4) {
                String sufixo = partes[3];
                if ("POST".equals(metodo) && "vendas".equals(partes[1]) && "cancelar".equals(sufixo)) return "Cancelar venda #" + id;
                if ("usuarios".equals(partes[1]) && "POST".equals(metodo) && "senha".equals(sufixo)) return "Redefinir senha do usuário #" + id;
                if ("PATCH".equals(metodo) && ("usuarios".equals(partes[1]) || "medicamentos".equals(partes[1]))) {
                    String entidade = "usuarios".equals(partes[1]) ? "usuário" : "medicamento";
                    if ("ativar".equals(sufixo)) return "Ativar " + entidade + " #" + id;
                    if ("desativar".equals(sufixo)) return "Desativar " + entidade + " #" + id;
                }
            }
            if (partes.length == 3 && "PUT".equals(metodo) && "medicamentos".equals(partes[1])) return "Atualizar medicamento #" + id;
        }
        return "Alterar dados";
    }
}
