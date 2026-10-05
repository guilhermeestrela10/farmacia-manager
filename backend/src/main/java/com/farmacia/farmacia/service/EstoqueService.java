package com.farmacia.farmacia.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.farmacia.farmacia.dto.AjusteEstoqueRequest;
import com.farmacia.farmacia.dto.EntradaEstoqueRequest;
import com.farmacia.farmacia.dto.LoteResponse;
import com.farmacia.farmacia.dto.MovimentacaoResponse;
import com.farmacia.farmacia.dto.PerdaEstoqueRequest;
import com.farmacia.farmacia.dto.SaidaEstoqueRequest;
import com.farmacia.farmacia.dto.SaldoResponse;
import com.farmacia.farmacia.model.Lote;
import com.farmacia.farmacia.model.Medicamento;
import com.farmacia.farmacia.model.MovimentacaoEstoque;
import com.farmacia.farmacia.model.TipoMovimentacao;
import com.farmacia.farmacia.repository.LoteRepository;
import com.farmacia.farmacia.repository.MedicamentoRepository;
import com.farmacia.farmacia.repository.MovimentacaoEstoqueRepository;

import jakarta.persistence.criteria.Predicate;

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
        Medicamento medicamento = buscarMedicamentoAtivo(request.medicamentoId());
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

    @Transactional
    public List<MovimentacaoResponse> registrarSaida(SaidaEstoqueRequest request) {
        Medicamento medicamento = buscarMedicamentoAtivo(request.medicamentoId());

        List<Lote> lotes = loteRepository
                .buscarDisponiveisParaSaida(medicamento.getId(), LocalDate.now());

        int disponivel = lotes.stream().mapToInt(Lote::getQuantidadeAtual).sum();
        int pedido = request.quantidade();

        if (disponivel < pedido) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Estoque insuficiente. Disponível para saída: " + disponivel + ".");
        }

        String motivo = normalizar(request.motivo());
        String responsavel = request.responsavel().trim();

        List<MovimentacaoResponse> movimentacoes = new ArrayList<>();
        int restante = pedido;

        for (Lote lote : lotes) {
            if (restante == 0) {
                break;
            }

            int retirada = Math.min(restante, lote.getQuantidadeAtual());
            lote.remover(retirada);

            MovimentacaoEstoque movimentacao = movimentacaoRepository.save(
                    new MovimentacaoEstoque(
                            lote, TipoMovimentacao.SAIDA, retirada, motivo, responsavel));

            movimentacoes.add(toResponse(movimentacao));
            restante -= retirada;
        }

        return movimentacoes;
    }

    @Transactional
    public MovimentacaoResponse registrarAjuste(AjusteEstoqueRequest request) {
        TipoMovimentacao tipo = request.tipo();

        if (tipo != TipoMovimentacao.AJUSTE_ENTRADA && tipo != TipoMovimentacao.AJUSTE_SAIDA) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tipo inválido para ajuste. Use AJUSTE_ENTRADA ou AJUSTE_SAIDA.");
        }

        Lote lote = buscarLoteComTravamento(request.loteId());
        int quantidade = request.quantidade();

        if (tipo == TipoMovimentacao.AJUSTE_ENTRADA) {
            lote.adicionar(quantidade);
        } else {
            baixar(lote, quantidade);
        }

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque(
                lote, tipo, quantidade, request.motivo().trim(), request.responsavel().trim());

        return toResponse(movimentacaoRepository.save(movimentacao));
    }

    @Transactional
    public MovimentacaoResponse registrarPerda(PerdaEstoqueRequest request) {
        Lote lote = buscarLoteComTravamento(request.loteId());
        int quantidade = request.quantidade();

        baixar(lote, quantidade);

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque(
                lote,
                TipoMovimentacao.PERDA,
                quantidade,
                request.motivo().trim(),
                request.responsavel().trim());

        return toResponse(movimentacaoRepository.save(movimentacao));
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoResponse> listarMovimentacoes(Long medicamentoId,
                                                          TipoMovimentacao tipo,
                                                          LocalDate de,
                                                          LocalDate ate,
                                                          int limite) {
        if (de != null && ate != null && ate.isBefore(de)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A data final não pode ser anterior à inicial.");
        }

        ZoneId zona = ZoneId.systemDefault();

        Specification<MovimentacaoEstoque> filtro = (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();

            if (medicamentoId != null) {
                condicoes.add(cb.equal(
                        root.get("lote").get("medicamento").get("id"), medicamentoId));
            }
            if (tipo != null) {
                condicoes.add(cb.equal(root.get("tipo"), tipo));
            }
            if (de != null) {
                condicoes.add(cb.greaterThanOrEqualTo(
                        root.<Instant>get("dataHora"), de.atStartOfDay(zona).toInstant()));
            }
            if (ate != null) {
                condicoes.add(cb.lessThan(
                        root.<Instant>get("dataHora"),
                        ate.plusDays(1).atStartOfDay(zona).toInstant()));
            }

            return cb.and(condicoes.toArray(new Predicate[0]));
        };

        int tamanho = Math.min(Math.max(limite, 1), 500);
        PageRequest pagina = PageRequest.of(
                0, tamanho, Sort.by(Sort.Direction.DESC, "dataHora", "id"));

        return movimentacaoRepository.findAll(filtro, pagina)
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();
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
        long total = loteRepository.somarQuantidadePorMedicamento(medicamento.getId());
        long disponivel = loteRepository.somarDisponivelPorMedicamento(
                medicamento.getId(), LocalDate.now());

        return new SaldoResponse(
                medicamento.getId(),
                medicamento.getNome(),
                total,
                disponivel,
                total - disponivel,
                medicamento.getEstoqueMinimo(),
                disponivel <= medicamento.getEstoqueMinimo());
    }

    private void baixar(Lote lote, int quantidade) {
        if (quantidade > lote.getQuantidadeAtual()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Quantidade maior que o saldo do lote (" + lote.getQuantidadeAtual() + ").");
        }
        lote.remover(quantidade);
    }

    private Lote buscarLoteComTravamento(Long id) {
        return loteRepository.buscarPorIdComTravamento(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "Lote não encontrado."));
    }

    private Medicamento buscarMedicamento(Long id) {
        return medicamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Medicamento não encontrado."));
    }

    private Medicamento buscarMedicamentoAtivo(Long id) {
        Medicamento medicamento = medicamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "Medicamento não encontrado."));

        if (!medicamento.isAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Medicamento desativado.");
        }

        return medicamento;
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