package com.farmacia.farmacia.service;

import com.farmacia.farmacia.dto.MedicamentoRequest;
import com.farmacia.farmacia.dto.MedicamentoResponse;
import com.farmacia.farmacia.model.Categoria;
import com.farmacia.farmacia.model.Fabricante;
import com.farmacia.farmacia.model.Medicamento;
import com.farmacia.farmacia.model.TipoMedicamento;
import com.farmacia.farmacia.repository.CategoriaRepository;
import com.farmacia.farmacia.repository.FabricanteRepository;
import com.farmacia.farmacia.repository.MedicamentoRepository;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        String codigoBarras = normalizarCodigo(request.codigoBarras());

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
                request.tipo(),
                request.preco(),
                request.estoqueMinimo());

        return toResponse(medicamentoRepository.save(medicamento));
    }

    @Transactional
    public MedicamentoResponse atualizar(Long id, MedicamentoRequest request) {
        Medicamento medicamento = buscarEntidade(id);
        String codigoBarras = normalizarCodigo(request.codigoBarras());

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
                request.tipo(),
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
    public List<MedicamentoResponse> listar(String busca, Long categoriaId,
                                            TipoMedicamento tipo, boolean incluirInativos) {
        List<String> palavras = palavrasDaBusca(busca);

        return medicamentoRepository.listarTodosComRelacionamentos().stream()
                .filter(m -> incluirInativos || m.isAtivo())
                .filter(m -> categoriaId == null || m.getCategoria().getId().equals(categoriaId))
                .filter(m -> tipo == null || m.getTipo() == tipo)
                .filter(m -> corresponde(m, palavras))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicamentoResponse buscarPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    private boolean corresponde(Medicamento m, List<String> palavras) {
        if (palavras.isEmpty()) {
            return true;
        }

        String texto = semAcento(String.join(" ",
                m.getNome(),
                m.getCategoria().getNome(),
                m.getFabricante().getNome(),
                rotulo(m.getTipo()),
                m.getCodigoBarras() == null ? "" : m.getCodigoBarras()));

        return palavras.stream().allMatch(texto::contains);
    }

    private List<String> palavrasDaBusca(String busca) {
        if (busca == null || busca.isBlank()) {
            return List.of();
        }
        return Arrays.asList(semAcento(busca).split("\\s+"));
    }

    private String semAcento(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String rotulo(TipoMedicamento tipo) {
        return switch (tipo) {
            case REFERENCIA -> "Referência";
            case GENERICO -> "Genérico";
            case SIMILAR -> "Similar";
        };
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

    private String normalizarCodigo(String codigoBarras) {
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
                m.getTipo(),
                m.getPreco(),
                m.getEstoqueMinimo(),
                m.isAtivo());
    }
}