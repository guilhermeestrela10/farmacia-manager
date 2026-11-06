package com.farmacia.farmacia.service;
import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.model.*;
import com.farmacia.farmacia.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.time.LocalDate;
@Service
public class DispensacaoService {
    private final DispensacaoRepository repository;
    private final ReceitaRepository receitas;
    private final ClienteService clientes;
    private final VendaService vendas;
    private final VendaRepository vendaRepository;
    public DispensacaoService(DispensacaoRepository repository,ReceitaRepository receitas,ClienteService clientes,VendaService vendas,VendaRepository vendaRepository) {
        this.repository=repository; this.receitas=receitas; this.clientes=clientes; this.vendas=vendas; this.vendaRepository=vendaRepository;
    }
    @Transactional public DispensacaoResponse registrar(DispensacaoRequest r,String login) {
        // Serializa dispensações da mesma receita até a venda e o estoque confirmarem juntos.
        var receita=receitas.buscarPorIdComTravamento(r.receitaId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Receita não encontrada."));
        var hoje=LocalDate.now();
        if (receita.getEmitidaEm().isAfter(hoje) || receita.getValidadeAte().isBefore(hoje))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Receita fora do período de validade.");
        clientes.buscarAtivo(receita.getCliente().getId());
        Map<Long,Long> pedidos=new TreeMap<>();
        for (var item:r.itens()) pedidos.merge(item.medicamentoId(),item.quantidade().longValue(),Long::sum);
        for (var pedido:pedidos.entrySet()) {
            var prescrito=receita.getItens().stream().filter(i -> i.getMedicamento().getId().equals(pedido.getKey())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Medicamento não consta na receita."));
            long dispensado=repository.quantidadeDispensada(receita.getId(),pedido.getKey());
            if (pedido.getValue()>prescrito.getQuantidade()-dispensado)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Quantidade excede o saldo da receita.");
        }
        var venda=vendas.registrar(new VendaRequest(r.itens(),r.formaPagamento()),login);
        return toResponse(repository.save(new Dispensacao(receita,vendaRepository.getReferenceById(venda.id()))));
    }
    @Transactional(readOnly=true) public List<DispensacaoResponse> listar() { return repository.findAllByOrderByCriadaEmDescIdDesc().stream().map(this::toResponse).toList(); }
    private DispensacaoResponse toResponse(Dispensacao d) {
        var v=d.getVenda();
        return new DispensacaoResponse(d.getId(),d.getReceita().getId(),d.getReceita().getCliente().getNome(),v.getId(),v.getTotal(),v.getStatus(),d.getCriadaEm());
    }
}
