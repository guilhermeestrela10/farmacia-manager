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
public class ReceitaService {
    private final ReceitaRepository repository;
    private final ClienteService clientes;
    private final MedicamentoRepository medicamentos;
    private final DispensacaoRepository dispensacoes;
    public ReceitaService(ReceitaRepository repository,ClienteService clientes,MedicamentoRepository medicamentos,DispensacaoRepository dispensacoes) {
        this.repository=repository; this.clientes=clientes; this.medicamentos=medicamentos; this.dispensacoes=dispensacoes;
    }
    @Transactional public ReceitaResponse criar(ReceitaRequest r) {
        if (r.emitidaEm().isAfter(LocalDate.now()) || r.validadeAte().isBefore(r.emitidaEm()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Datas da receita inválidas.");
        var receita=new Receita(clientes.buscarAtivo(r.clienteId()),r.prescritor(),r.numero(),r.emitidaEm(),r.validadeAte(),r.observacoes());
        Set<Long> ids=new HashSet<>();
        for (var item:r.itens()) {
            if (!ids.add(item.medicamentoId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Medicamento repetido na receita.");
            var medicamento=medicamentos.findById(item.medicamentoId()).filter(Medicamento::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Medicamento não encontrado ou desativado."));
            receita.adicionarItem(medicamento,item.quantidade());
        }
        return toResponse(repository.save(receita));
    }
    @Transactional(readOnly=true) public List<ReceitaResponse> listar(Long clienteId) { return repository.listar(clienteId).stream().map(this::toResponse).toList(); }
    @Transactional(readOnly=true) public ReceitaResponse buscar(Long id) { return toResponse(repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Receita não encontrada."))); }
    private ReceitaResponse toResponse(Receita r) {
        var itens=r.getItens().stream().map(i -> {
            var m=i.getMedicamento();
            long dispensado=dispensacoes.quantidadeDispensada(r.getId(),m.getId());
            return new ItemReceitaResponse(m.getId(),m.getNome(),m.getPreco(),i.getQuantidade(),dispensado,i.getQuantidade()-dispensado);
        }).toList();
        return new ReceitaResponse(r.getId(),r.getCliente().getId(),r.getCliente().getNome(),r.getPrescritor(),r.getNumero(),r.getEmitidaEm(),r.getValidadeAte(),r.getObservacoes(),itens);
    }
}
