package com.farmacia.farmacia.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.farmacia.farmacia.dto.MedicamentoRequest;
import com.farmacia.farmacia.dto.MedicamentoResponse;
import com.farmacia.farmacia.model.Categoria;
import com.farmacia.farmacia.model.Fabricante;
import com.farmacia.farmacia.model.Medicamento;
import com.farmacia.farmacia.repository.CategoriaRepository;
import com.farmacia.farmacia.repository.FabricanteRepository;
import com.farmacia.farmacia.repository.MedicamentoRepository;

@Service
public class MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;
    private final CategoriaRepository categoriaRepository;
    private final FabricanteRepository fabricanteRepository;

    public MedicamentoService(MedicamentoRepository medicamentoRepository,
                              CategoriaRepository categoriaRepository,
                              FabricanteRepository fabricanteRepository) {
        this.medicamentoRepository = medicamentoRepository;
        this.categoriaRepository = categoriaRepository;
        this.fabricanteRepository = fabricanteRepository;
    }

    @Transactional
    public MedicamentoResponse criar(MedicamentoRequest request) {
        String codigoBarras = normalizar(request.codigoBarras());

        if (codigoBarras != null && medicamentoRepository.existsByCodigoBarras(codigoBarras)) {
            throw codigoBarrasDuplicado();
        }

        Categoria categoria = buscarCategoria(request.categoriaId());
        Fabricante fabricante = buscarFabricante(request.fabricanteId());

        Medicamento medicamento = new Medicamento(
                request.nome().trim(),
                codigoBarras,
                categoria,
                fabricante,
                request.preco(),
                request.estoqueMinimo());

        return toResponse(medicamentoRepository.save(medicamento));
    }

    @Transactional
    public MedicamentoResponse atualizar(Long id, MedicamentoRequest request) {
        Medicamento medicamento = buscarEntidade(id);
        String codigoBarras = normalizar(request.codigoBarras());

        if (codigoBarras != null
                && medicamentoRepository.existsByCodigoBarrasAndIdNot(codigoBarras, id)) {
            throw codigoBarrasDuplicado();
        }

        Categoria categoria = buscarCategoria(request.categoriaId());
        Fabricante fabricante = buscarFabricante(request.fabricanteId());

        medicamento.atualizar(
                request.nome().trim(),
                codigoBarras,
                categoria,
                fabricante,
                request.preco(),
                request.estoqueMinimo());

        return toResponse(medicamento);
    }

    @Transactional
    public MedicamentoResponse desativar(Long id) {
        Medicamento medicamento = buscarEntidade(id);
        medicamento.desativar();
        return toResponse(medicamento);
    }

    @Transactional
    public MedicamentoResponse ativar(Long id) {
        Medicamento medicamento = buscarEntidade(id);
        medicamento.ativar();
        return toResponse(medicamento);
    }

    @Transactional(readOnly = true)
    public List<MedicamentoResponse> listar(String nome, boolean incluirInativos) {
        boolean semFiltroDeNome = nome == null || nome.isBlank();
        List<Medicamento> medicamentos;

        if (semFiltroDeNome) {
            medicamentos = incluirInativos
                    ? medicamentoRepository.findAllByOrderByNome()
                    : medicamentoRepository.findByAtivoTrueOrderByNome();
        } else {
            String termo = nome.trim();
            medicamentos = incluirInativos
                    ? medicamentoRepository.findByNomeContainingIgnoreCaseOrderByNome(termo)
                    : medicamentoRepository
                            .findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNome(termo);
        }

        return medicamentos.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MedicamentoResponse buscarPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    private Medicamento buscarEntidade(Long id) {
        return medicamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Medicamento não encontrado."));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "Categoria não encontrada."));
    }

    private Fabricante buscarFabricante(Long id) {
        return fabricanteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "Fabricante não encontrado."));
    }

    private ResponseStatusException codigoBarrasDuplicado() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, "Já existe um medicamento com esse código de barras.");
    }

    private String normalizar(String codigoBarras) {
        if (codigoBarras == null || codigoBarras.isBlank()) {
            return null;
        }
        return codigoBarras.trim();
    }

    private MedicamentoResponse toResponse(Medicamento m) {
        return new MedicamentoResponse(
                m.getId(),
                m.getNome(),
                m.getCodigoBarras(),
                m.getCategoria().getId(),
                m.getCategoria().getNome(),
                m.getFabricante().getId(),
                m.getFabricante().getNome(),
                m.getPreco(),
                m.getEstoqueMinimo(),
                m.isAtivo());
    }
}