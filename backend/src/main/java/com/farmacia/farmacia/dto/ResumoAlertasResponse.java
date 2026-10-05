package com.farmacia.farmacia.dto;

public record ResumoAlertasResponse(
        long medicamentosAtivos,
        long medicamentosSemEstoque,
        long medicamentosComEstoqueBaixo,
        int diasAlertaVencimento,
        long lotesVencendoEmBreve,
        long lotesVencidosComSaldo,
        long unidadesVencidas
) {
}