package com.farmacia.farmacia.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.farmacia.farmacia.dto.EntradaEstoqueRequest;
import com.farmacia.farmacia.dto.LoteResponse;
import com.farmacia.farmacia.dto.MovimentacaoResponse;
import com.farmacia.farmacia.dto.SaldoResponse;
import com.farmacia.farmacia.model.Lote;
import com.farmacia.farmacia.model.Medicamento;
import com.farmacia.farmacia.model.MovimentacaoEstoque;
import com.farmacia.farmacia.model.TipoMovimentacao;
import com.farmacia.farmacia.repository.LoteRepository;
import com.farmacia.farmacia.repository.MedicamentoRepository;
import com.farmacia.farmacia.repository.MovimentacaoEstoqueRepository;

@Service
public class EstoqueService {

    private final MedicamentoRepository medicamentoRepository;
    private final LoteRepository loteRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    public EstoqueService(MedicamentoRepository medicamentoRepository,
                          LoteRepository loteRepository,
                          MovimentacaoEstoqueRepository movimentacaoRepository) {
        this.medicamentoRepository = medicamentoRepository;
        this.loteRepository = loteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Transactional
    public MovimentacaoResponse registrarEntrada(EntradaEstoqueRequest request) {
        Medicamento medicamento = medicamentoRepository.findById(request.medicamentoId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "Medicamento não encontrado."));

        if (!medicamento.isAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Medicamento desativado.");
        }

        String numeroLote = request.numeroLote().trim();

        Lote lote = loteRepository
                .findByMedicamentoIdAndNumeroLote(medicamento.getId(), numeroLote)
                .orElseGet(() -> loteRepository.save(
                        new Lote(medicamento, numeroLote, request.validade())));

        if (!lote.getValidade().equals(request.validade())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Esse lote já existe com outra validade.");
        }

        lote.adicionar(request.quantidade());

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque(
                lote,
                TipoMovimentacao.ENTRADA,
                request.quantidade(),
                normalizar(request.motivo()),
                request.responsavel().trim());

        return toResponse(movimentacaoRepository.save(movimentacao));
    }

    @Transactional(readOnly = true)
    public List<LoteResponse> listarLotes(Long medicamentoId) {
        Medicamento medicamento = buscarMedicamento(medicamentoId);

        return loteRepository
                .findByMedicamentoIdAndQuantidadeAtualGreaterThanOrderByValidade(
                        medicamento.getId(), 0)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SaldoResponse consultarSaldo(Long medicamentoId) {
        Medicamento medicamento = buscarMedicamento(medicamentoId);
        long saldo = loteRepository.somarQuantidadePorMedicamento(medicamento.getId());

        return new SaldoResponse(
                medicamento.getId(),
                medicamento.getNome(),
                saldo,
                medicamento.getEstoqueMinimo(),
                saldo <= medicamento.getEstoqueMinimo());
    }

    private Medicamento buscarMedicamento(Long id) {
        return medicamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Medicamento não encontrado."));
    }

    private String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private LoteResponse toResponse(Lote lote) {
        return new LoteResponse(
                lote.getId(),
                lote.getMedicamento().getId(),
                lote.getMedicamento().getNome(),
                lote.getNumeroLote(),
                lote.getValidade(),
                lote.getQuantidadeAtual());
    }

    private MovimentacaoResponse toResponse(MovimentacaoEstoque m) {
        Lote lote = m.getLote();
        return new MovimentacaoResponse(
                m.getId(),
                lote.getId(),
                lote.getNumeroLote(),
                lote.getMedicamento().getId(),
                lote.getMedicamento().getNome(),
                m.getTipo(),
                m.getQuantidade(),
                m.getMotivo(),
                m.getResponsavel(),
                m.getDataHora());
    }
}