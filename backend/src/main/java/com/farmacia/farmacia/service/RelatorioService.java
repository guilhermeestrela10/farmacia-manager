package com.farmacia.farmacia.service;

import com.farmacia.farmacia.dto.CategoriaTotalResponse;
import com.farmacia.farmacia.dto.ProdutoVendidoResponse;
import com.farmacia.farmacia.dto.RelatorioEstoqueResponse;
import com.farmacia.farmacia.dto.RelatorioVendasResponse;
import com.farmacia.farmacia.dto.TotalFormaResponse;
import com.farmacia.farmacia.dto.VendaDiaResponse;
import com.farmacia.farmacia.dto.VendedorTotalResponse;
import com.farmacia.farmacia.model.FormaPagamento;
import com.farmacia.farmacia.model.StatusVenda;
import com.farmacia.farmacia.model.TipoMovimentacao;
import com.farmacia.farmacia.model.Venda;
import com.farmacia.farmacia.repository.ItemVendaRepository;
import com.farmacia.farmacia.repository.LoteRepository;
import com.farmacia.farmacia.repository.MovimentacaoEstoqueRepository;
import com.farmacia.farmacia.repository.VendaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RelatorioService {

    private static final long MAXIMO_DIAS = 366;
    private static final int TOP_PRODUTOS = 10;
    private static final int DIAS_VENCIMENTO = 90;
    private static final LocalDate INICIO_DOS_TEMPOS = LocalDate.of(1900, 1, 1);
    private static final LocalDate FIM_DOS_TEMPOS = LocalDate.of(9999, 12, 31);

    private record Periodo(LocalDate inicio, LocalDate fim, Instant comeco, Instant termino) {
    }

    private record Resumo(long unidades, BigDecimal valor) {
    }

    private final VendaRepository vendaRepository;
    private final ItemVendaRepository itemVendaRepository;
    private final LoteRepository loteRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    public RelatorioService(VendaRepository vendaRepository,
                            ItemVendaRepository itemVendaRepository,
                            LoteRepository loteRepository,
                            MovimentacaoEstoqueRepository movimentacaoRepository) {
        this.vendaRepository = vendaRepository;
        this.itemVendaRepository = itemVendaRepository;
        this.loteRepository = loteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    /* ---------- Vendas ---------- */

    @Transactional(readOnly = true)
    public RelatorioVendasResponse vendas(LocalDate de, LocalDate ate) {
        Periodo periodo = periodo(de, ate);
        ZoneId zona = ZoneId.systemDefault();

        List<Venda> concluidas = buscarVendas(periodo, StatusVenda.CONCLUIDA);
        List<Venda> canceladas = buscarVendas(periodo, StatusVenda.CANCELADA);

        BigDecimal totalVendido = somar(concluidas);
        long quantidade = concluidas.size();
        BigDecimal ticketMedio = quantidade == 0
                ? BigDecimal.ZERO
                : totalVendido.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);

        // Um registro por dia do período, mesmo sem vendas (o gráfico fica contínuo).
        Map<LocalDate, BigDecimal> totalPorDia = new TreeMap<>();
        Map<LocalDate, Long> quantidadePorDia = new TreeMap<>();
        for (LocalDate dia = periodo.inicio(); !dia.isAfter(periodo.fim()); dia = dia.plusDays(1)) {
            totalPorDia.put(dia, BigDecimal.ZERO);
            quantidadePorDia.put(dia, 0L);
        }

        Map<FormaPagamento, BigDecimal> totalPorForma = new EnumMap<>(FormaPagamento.class);
        Map<FormaPagamento, Long> quantidadePorForma = new EnumMap<>(FormaPagamento.class);

        Map<Long, String> nomeDoVendedor = new HashMap<>();
        Map<Long, BigDecimal> totalPorVendedor = new HashMap<>();
        Map<Long, Long> quantidadePorVendedor = new HashMap<>();

        for (Venda venda : concluidas) {
            LocalDate dia = venda.getCriadaEm().atZone(zona).toLocalDate();
            totalPorDia.merge(dia, venda.getTotal(), BigDecimal::add);
            quantidadePorDia.merge(dia, 1L, Long::sum);

            totalPorForma.merge(venda.getFormaPagamento(), venda.getTotal(), BigDecimal::add);
            quantidadePorForma.merge(venda.getFormaPagamento(), 1L, Long::sum);

            Long vendedorId = venda.getVendedor().getId();
            nomeDoVendedor.put(vendedorId, venda.getVendedor().getNome());
            totalPorVendedor.merge(vendedorId, venda.getTotal(), BigDecimal::add);
            quantidadePorVendedor.merge(vendedorId, 1L, Long::sum);
        }

        List<VendaDiaResponse> porDia = totalPorDia.entrySet().stream()
                .map(e -> new VendaDiaResponse(
                        e.getKey(), quantidadePorDia.get(e.getKey()), e.getValue()))
                .toList();

        List<TotalFormaResponse> porForma = Arrays.stream(FormaPagamento.values())
                .map(forma -> new TotalFormaResponse(
                        forma,
                        totalPorForma.getOrDefault(forma, BigDecimal.ZERO),
                        quantidadePorForma.getOrDefault(forma, 0L)))
                .toList();

        List<VendedorTotalResponse> porVendedor = totalPorVendedor.entrySet().stream()
                .map(e -> new VendedorTotalResponse(
                        nomeDoVendedor.get(e.getKey()),
                        quantidadePorVendedor.get(e.getKey()),
                        e.getValue()))
                .sorted(Comparator.comparing(VendedorTotalResponse::total).reversed())
                .toList();

        List<CategoriaTotalResponse> porCategoria = itemVendaRepository
                .totaisPorCategoria(StatusVenda.CONCLUIDA, periodo.comeco(), periodo.termino())
                .stream()
                .map(l -> new CategoriaTotalResponse(
                        (String) l[0], ((Number) l[1]).longValue(), (BigDecimal) l[2]))
                .toList();

        long unidadesVendidas = porCategoria.stream()
                .mapToLong(CategoriaTotalResponse::unidades)
                .sum();

        List<ProdutoVendidoResponse> maisVendidos = itemVendaRepository
                .maisVendidos(StatusVenda.CONCLUIDA, periodo.comeco(), periodo.termino(),
                        PageRequest.of(0, TOP_PRODUTOS))
                .stream()
                .map(l -> new ProdutoVendidoResponse(
                        ((Number) l[0]).longValue(),
                        (String) l[1],
                        ((Number) l[2]).longValue(),
                        (BigDecimal) l[3]))
                .toList();

        return new RelatorioVendasResponse(
                periodo.inicio(),
                periodo.fim(),
                totalVendido,
                quantidade,
                ticketMedio,
                unidadesVendidas,
                canceladas.size(),
                somar(canceladas),
                porDia,
                porForma,
                porVendedor,
                porCategoria,
                maisVendidos);
    }

    /* ---------- Posição do estoque ---------- */

    @Transactional(readOnly = true)
    public RelatorioEstoqueResponse estoque(LocalDate de, LocalDate ate) {
        LocalDate hoje = LocalDate.now();
        Periodo periodo = periodo(de, ate);

        Resumo disponivel = resumoPorValidade(hoje, FIM_DOS_TEMPOS);
        Resumo vencendo = resumoPorValidade(hoje, hoje.plusDays(DIAS_VENCIMENTO));
        Resumo vencido = resumoPorValidade(INICIO_DOS_TEMPOS, hoje.minusDays(1));

        List<Object[]> perdas = movimentacaoRepository.somarPorTipo(
                TipoMovimentacao.PERDA, periodo.comeco(), periodo.termino());
        Resumo perdidas = leitura(perdas);

        return new RelatorioEstoqueResponse(
                disponivel.unidades(),
                disponivel.valor(),
                DIAS_VENCIMENTO,
                vencendo.unidades(),
                vencendo.valor(),
                vencido.unidades(),
                vencido.valor(),
                perdidas.unidades(),
                perdidas.valor());
    }

    /* ---------- Apoio ---------- */

    private Periodo periodo(LocalDate de, LocalDate ate) {
        LocalDate fim = ate != null ? ate : LocalDate.now();
        LocalDate inicio = de != null ? de : fim.minusDays(29);

        if (fim.isBefore(inicio)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A data final não pode ser anterior à inicial.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) > MAXIMO_DIAS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O período máximo é de " + MAXIMO_DIAS + " dias.");
        }

        ZoneId zona = ZoneId.systemDefault();
        return new Periodo(
                inicio,
                fim,
                inicio.atStartOfDay(zona).toInstant(),
                fim.plusDays(1).atStartOfDay(zona).toInstant());
    }

    private List<Venda> buscarVendas(Periodo periodo, StatusVenda status) {
        Query filtro = Query.query(Criteria.where("criadaEm")
                .gte(periodo.comeco()).lt(periodo.termino()).and("status").is(status));
        return vendaRepository.findAll(filtro);
    }

    private BigDecimal somar(List<Venda> vendas) {
        return vendas.stream()
                .map(Venda::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Resumo resumoPorValidade(LocalDate de, LocalDate ate) {
        return leitura(loteRepository.resumoPorValidade(de, ate));
    }

    private Resumo leitura(List<Object[]> linhas) {
        if (linhas.isEmpty()) {
            return new Resumo(0, BigDecimal.ZERO);
        }

        Object[] linha = linhas.get(0);
        long unidades = linha[0] == null ? 0 : ((Number) linha[0]).longValue();
        BigDecimal valor = linha[1] == null ? BigDecimal.ZERO : (BigDecimal) linha[1];
        return new Resumo(unidades, valor);
    }
}
