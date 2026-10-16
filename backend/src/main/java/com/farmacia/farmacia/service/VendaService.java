package com.farmacia.farmacia.service;

import com.farmacia.farmacia.dto.ItemVendaRequest;
import com.farmacia.farmacia.dto.ItemVendaResponse;
import com.farmacia.farmacia.dto.ProdutoVendaResponse;
import com.farmacia.farmacia.dto.VendaRequest;
import com.farmacia.farmacia.dto.VendaResponse;
import com.farmacia.farmacia.model.ItemVenda;
import com.farmacia.farmacia.model.Lote;
import com.farmacia.farmacia.model.Medicamento;
import com.farmacia.farmacia.model.MovimentacaoEstoque;
import com.farmacia.farmacia.model.TipoMedicamento;
import com.farmacia.farmacia.model.TipoMovimentacao;
import com.farmacia.farmacia.model.Usuario;
import com.farmacia.farmacia.model.Venda;
import com.farmacia.farmacia.repository.LoteRepository;
import com.farmacia.farmacia.repository.MedicamentoRepository;
import com.farmacia.farmacia.repository.MovimentacaoEstoqueRepository;
import com.farmacia.farmacia.repository.UsuarioRepository;
import com.farmacia.farmacia.repository.VendaRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VendaService {

    private static final int LIMITE_PRODUTOS = 60;

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

    @Transactional
    public VendaResponse registrar(VendaRequest request, String loginVendedor) {
        Usuario vendedor = usuarioRepository.findByLogin(loginVendedor)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre novamente."));

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

    @Transactional(readOnly = true)
    public VendaResponse buscar(Long id, String login, boolean administrador) {
        Venda venda = vendaRepository.findById(id)
                .filter(v -> administrador || v.getVendedor().getLogin().equals(login))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venda não encontrada."));

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
                itens);
    }
}