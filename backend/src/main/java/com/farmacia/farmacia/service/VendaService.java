package com.farmacia.farmacia.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.farmacia.farmacia.dto.HistoricoVendasResponse;
import com.farmacia.farmacia.dto.ItemVendaRequest;
import com.farmacia.farmacia.dto.ItemVendaResponse;
import com.farmacia.farmacia.dto.ProdutoVendaResponse;
import com.farmacia.farmacia.dto.TotalFormaResponse;
import com.farmacia.farmacia.dto.VendaRequest;
import com.farmacia.farmacia.dto.VendaResponse;
import com.farmacia.farmacia.dto.VendaResumoResponse;
import com.farmacia.farmacia.model.FormaPagamento;
import com.farmacia.farmacia.model.ItemVenda;
import com.farmacia.farmacia.model.Lote;
import com.farmacia.farmacia.model.Medicamento;
import com.farmacia.farmacia.model.MovimentacaoEstoque;
import com.farmacia.farmacia.model.StatusVenda;
import com.farmacia.farmacia.model.TipoMedicamento;
import com.farmacia.farmacia.model.TipoMovimentacao;
import com.farmacia.farmacia.model.Usuario;
import com.farmacia.farmacia.model.Venda;
import com.farmacia.farmacia.repository.LoteRepository;
import com.farmacia.farmacia.repository.MedicamentoRepository;
import com.farmacia.farmacia.repository.MovimentacaoEstoqueRepository;
import com.farmacia.farmacia.repository.UsuarioRepository;
import com.farmacia.farmacia.repository.VendaRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class VendaService {

    private static final int LIMITE_PRODUTOS = 60;
    private static final int LIMITE_HISTORICO = 500;
    private static final long MAXIMO_DIAS_PERIODO = 366;

    private final VendaRepository vendaRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final LoteRepository loteRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicamentoService medicamentoService;

    public VendaService(VendaRepository vendaRepository,
                        MedicamentoRepository medicamentoRepository,
                        LoteRepository loteRepository,
                        MovimentacaoEstoqueRepository movimentacaoRepository,
                        UsuarioRepository usuarioRepository,
                        MedicamentoService medicamentoService) {
        this.vendaRepository = vendaRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.loteRepository = loteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.medicamentoService = medicamentoService;
    }

    /* ---------- Busca de produtos para a tela de venda ---------- */

    @Transactional(readOnly = true)
    public List<ProdutoVendaResponse> listarProdutos(String busca, Long categoriaId,
                                                     TipoMedicamento tipo) {
        Map<Long, Long> saldos = new HashMap<>();
        for (Object[] linha : medicamentoRepository.saldoDisponivelDosAtivos(LocalDate.now())) {
            saldos.put(((Number) linha[0]).longValue(), ((Number) linha[3]).longValue());
        }

        return medicamentoService.listar(busca, categoriaId, tipo, false).stream()
                .map(m -> new ProdutoVendaResponse(
                        m.id(),
                        m.nome(),
                        m.categoriaNome(),
                        m.fabricanteNome(),
                        m.tipo(),
                        m.preco(),
                        saldos.getOrDefault(m.id(), 0L)))
                .sorted(Comparator.comparing((ProdutoVendaResponse p) -> p.disponivel() == 0))
                .limit(LIMITE_PRODUTOS)
                .toList();
    }

    /* ---------- Registrar venda ---------- */

    @Transactional
    public VendaResponse registrar(VendaRequest request, String loginVendedor) {
        Usuario vendedor = buscarUsuario(loginVendedor);

        // Juntar itens repetidos e processar sempre na mesma ordem (por id do medicamento)
        // evita que duas vendas simultâneas travem uma à outra.
        Map<Long, Integer> pedidos = new TreeMap<>();
        for (ItemVendaRequest item : request.itens()) {
            pedidos.merge(item.medicamentoId(), item.quantidade(), Integer::sum);
        }

        Venda venda = new Venda(vendedor, request.formaPagamento());

        for (Map.Entry<Long, Integer> pedido : pedidos.entrySet()) {
            Medicamento medicamento = medicamentoRepository.findById(pedido.getKey())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_ENTITY, "Medicamento não encontrado."));

            if (!medicamento.isAtivo()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Medicamento desativado: " + medicamento.getNome() + ".");
            }

            venda.adicionarItem(medicamento, pedido.getValue(), medicamento.getPreco());
        }

        vendaRepository.save(venda);
        baixarEstoque(venda, vendedor);

        return toResponse(venda);
    }

    private void baixarEstoque(Venda venda, Usuario vendedor) {
        LocalDate hoje = LocalDate.now();

        for (ItemVenda item : venda.getItens()) {
            Medicamento medicamento = item.getMedicamento();

            List<Lote> lotes = loteRepository.buscarDisponiveisParaSaida(medicamento.getId(), hoje);
            int disponivel = lotes.stream().mapToInt(Lote::getQuantidadeAtual).sum();

            if (disponivel < item.getQuantidade()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Estoque insuficiente de " + medicamento.getNome()
                                + ". Disponível: " + disponivel + ".");
            }

            int restante = item.getQuantidade();

            for (Lote lote : lotes) {
                if (restante == 0) {
                    break;
                }

                int retirada = Math.min(restante, lote.getQuantidadeAtual());
                lote.remover(retirada);

                movimentacaoRepository.save(new MovimentacaoEstoque(
                        lote,
                        TipoMovimentacao.SAIDA,
                        retirada,
                        "Venda #" + venda.getId(),
                        vendedor.getNome(),
                        venda.getId()));

                restante -= retirada;
            }
        }
    }

    /* ---------- Consultar ---------- */

    @Transactional(readOnly = true)
    public VendaResponse buscar(Long id, String login, boolean administrador) {
        Venda venda = vendaRepository.findById(id)
                .filter(v -> administrador || v.getVendedor().getLogin().equals(login))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venda não encontrada."));

        return toResponse(venda);
    }

    @Transactional(readOnly = true)
    public HistoricoVendasResponse listar(LocalDate de, LocalDate ate, Long vendedorId,
                                          StatusVenda status, String login,
                                          boolean administrador) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = de != null ? de : hoje;
        LocalDate fim = ate != null ? ate : hoje;

        if (fim.isBefore(inicio)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A data final não pode ser anterior à inicial.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) > MAXIMO_DIAS_PERIODO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O período máximo é de " + MAXIMO_DIAS_PERIODO + " dias.");
        }

        // O vendedor só enxerga as próprias vendas, não importa o que a tela envie.
        Long vendedorFiltro = administrador ? vendedorId : buscarUsuario(login).getId();

        ZoneId zona = ZoneId.systemDefault();
        Instant comeco = inicio.atStartOfDay(zona).toInstant();
        Instant termino = fim.plusDays(1).atStartOfDay(zona).toInstant();

        Specification<Venda> filtro = (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.greaterThanOrEqualTo(root.<Instant>get("criadaEm"), comeco));
            condicoes.add(cb.lessThan(root.<Instant>get("criadaEm"), termino));

            if (vendedorFiltro != null) {
                condicoes.add(cb.equal(root.get("vendedor").get("id"), vendedorFiltro));
            }
            if (status != null) {
                condicoes.add(cb.equal(root.get("status"), status));
            }

            return cb.and(condicoes.toArray(new Predicate[0]));
        };

        List<Venda> vendas = vendaRepository.findAll(filtro,
                Sort.by(Sort.Direction.DESC, "criadaEm", "id"));

        BigDecimal totalVendido = BigDecimal.ZERO;
        long concluidas = 0;
        long canceladas = 0;
        Map<FormaPagamento, BigDecimal> totaisPorForma = new EnumMap<>(FormaPagamento.class);
        Map<FormaPagamento, Long> quantidadePorForma = new EnumMap<>(FormaPagamento.class);

        for (Venda venda : vendas) {
            if (venda.getStatus() == StatusVenda.CANCELADA) {
                canceladas++;
                continue;
            }
            concluidas++;
            totalVendido = totalVendido.add(venda.getTotal());
            totaisPorForma.merge(venda.getFormaPagamento(), venda.getTotal(), BigDecimal::add);
            quantidadePorForma.merge(venda.getFormaPagamento(), 1L, Long::sum);
        }

        List<TotalFormaResponse> porForma = Arrays.stream(FormaPagamento.values())
                .map(forma -> new TotalFormaResponse(
                        forma,
                        totaisPorForma.getOrDefault(forma, BigDecimal.ZERO),
                        quantidadePorForma.getOrDefault(forma, 0L)))
                .toList();

        List<VendaResumoResponse> linhas = vendas.stream()
                .limit(LIMITE_HISTORICO)
                .map(v -> new VendaResumoResponse(
                        v.getId(),
                        v.getCriadaEm(),
                        v.getVendedor().getNome(),
                        v.getFormaPagamento(),
                        v.getTotal(),
                        v.getStatus(),
                        v.getMotivoCancelamento()))
                .toList();

        return new HistoricoVendasResponse(
                linhas,
                concluidas,
                canceladas,
                totalVendido,
                porForma,
                vendas.size() > LIMITE_HISTORICO);
    }

    /* ---------- Cancelar ---------- */

    @Transactional
    public VendaResponse cancelar(Long id, String motivo, String loginAdmin,
                                  boolean administrador) {
        if (!administrador) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Acesso restrito ao administrador.");
        }

        Usuario admin = buscarUsuario(loginAdmin);

        // A linha da venda fica travada: dois cancelamentos ao mesmo tempo
        // não conseguem devolver o estoque duas vezes.
        Venda venda = vendaRepository.buscarPorIdComTravamento(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venda não encontrada."));

        if (venda.getStatus() == StatusVenda.CANCELADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Essa venda já foi cancelada.");
        }

        String motivoLimpo = motivo.trim();

        for (MovimentacaoEstoque saida
                : movimentacaoRepository.findByVendaIdAndTipo(venda.getId(), TipoMovimentacao.SAIDA)) {

            Lote lote = loteRepository.buscarPorIdComTravamento(saida.getLote().getId())
                    .orElseThrow(() -> new IllegalStateException("Lote da venda não encontrado."));

            lote.adicionar(saida.getQuantidade());

            movimentacaoRepository.save(new MovimentacaoEstoque(
                    lote,
                    TipoMovimentacao.ESTORNO,
                    saida.getQuantidade(),
                    "Cancelamento da venda #" + venda.getId() + ": " + motivoLimpo,
                    admin.getNome(),
                    venda.getId()));
        }

        venda.cancelar(admin, motivoLimpo);
        return toResponse(venda);
    }

    /* ---------- Apoio ---------- */

    private Usuario buscarUsuario(String login) {
        return usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre novamente."));
    }

    private VendaResponse toResponse(Venda venda) {
        List<ItemVendaResponse> itens = venda.getItens().stream()
                .map(i -> new ItemVendaResponse(
                        i.getMedicamento().getId(),
                        i.getMedicamento().getNome(),
                        i.getQuantidade(),
                        i.getPrecoUnitario(),
                        i.getSubtotal()))
                .toList();

        return new VendaResponse(
                venda.getId(),
                venda.getCriadaEm(),
                venda.getVendedor().getNome(),
                venda.getFormaPagamento(),
                venda.getTotal(),
                venda.getStatus(),
                venda.getCanceladaEm(),
                venda.getCanceladaPor() == null ? null : venda.getCanceladaPor().getNome(),
                venda.getMotivoCancelamento(),
                itens);
    }
}