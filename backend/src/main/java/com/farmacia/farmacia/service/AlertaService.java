package com.farmacia.farmacia.service;

import com.farmacia.farmacia.dto.AlertaEstoqueResponse;
import com.farmacia.farmacia.dto.AlertaLoteResponse;
import com.farmacia.farmacia.dto.ResumoAlertasResponse;
import com.farmacia.farmacia.model.Lote;
import com.farmacia.farmacia.repository.LoteRepository;
import com.farmacia.farmacia.repository.MedicamentoRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlertaService {

    private static final int DIAS_MINIMO = 1;
    private static final int DIAS_MAXIMO = 365;

    private final MedicamentoRepository medicamentoRepository;
    private final LoteRepository loteRepository;

    public AlertaService(MedicamentoRepository medicamentoRepository,
                         LoteRepository loteRepository) {
        this.medicamentoRepository = medicamentoRepository;
        this.loteRepository = loteRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertaEstoqueResponse> listarSemEstoque() {
        return saldosDosAtivos().stream()
                .filter(this::semEstoque)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertaEstoqueResponse> listarEstoqueBaixo() {
        return saldosDosAtivos().stream()
                .filter(this::estoqueBaixo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertaLoteResponse> listarVencimentoProximo(int dias) {
        validarDias(dias);
        LocalDate hoje = LocalDate.now();

        return loteRepository.buscarVencendoEntre(hoje, hoje.plusDays(dias)).stream()
                .map(lote -> toResponse(lote, hoje))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertaLoteResponse> listarVencidos() {
        LocalDate hoje = LocalDate.now();

        return loteRepository.buscarVencidosComSaldo(hoje).stream()
                .map(lote -> toResponse(lote, hoje))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumoAlertasResponse resumo(int dias) {
        validarDias(dias);
        LocalDate hoje = LocalDate.now();

        List<AlertaEstoqueResponse> saldos = saldosDosAtivos();
        List<Lote> vencendo = loteRepository.buscarVencendoEntre(hoje, hoje.plusDays(dias));
        List<Lote> vencidos = loteRepository.buscarVencidosComSaldo(hoje);

        return new ResumoAlertasResponse(
                saldos.size(),
                saldos.stream().filter(this::semEstoque).count(),
                saldos.stream().filter(this::estoqueBaixo).count(),
                dias,
                vencendo.size(),
                vencidos.size(),
                vencidos.stream().mapToLong(Lote::getQuantidadeAtual).sum());
    }

    private boolean semEstoque(AlertaEstoqueResponse alerta) {
        return alerta.saldoDisponivel() == 0;
    }

    private boolean estoqueBaixo(AlertaEstoqueResponse alerta) {
        return alerta.saldoDisponivel() > 0
                && alerta.saldoDisponivel() <= alerta.estoqueMinimo();
    }

    private List<AlertaEstoqueResponse> saldosDosAtivos() {
        return medicamentoRepository.saldoDisponivelDosAtivos(LocalDate.now()).stream()
                .map(linha -> new AlertaEstoqueResponse(
                        ((Number) linha[0]).longValue(),
                        (String) linha[1],
                        ((Number) linha[3]).longValue(),
                        ((Number) linha[2]).intValue()))
                .toList();
    }

    private void validarDias(int dias) {
        if (dias < DIAS_MINIMO || dias > DIAS_MAXIMO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O prazo deve estar entre " + DIAS_MINIMO + " e " + DIAS_MAXIMO + " dias.");
        }
    }

    private AlertaLoteResponse toResponse(Lote lote, LocalDate hoje) {
        return new AlertaLoteResponse(
                lote.getId(),
                lote.getMedicamento().getId(),
                lote.getMedicamento().getNome(),
                lote.getNumeroLote(),
                lote.getValidade(),
                lote.getQuantidadeAtual(),
                ChronoUnit.DAYS.between(hoje, lote.getValidade()));
    }
}