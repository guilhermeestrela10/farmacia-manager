package com.farmacia.farmacia;

import static org.junit.jupiter.api.Assertions.*;
import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.model.*;
import com.farmacia.farmacia.repository.*;
import com.farmacia.farmacia.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "farmacia.test.mongo-uri", matches = "mongodb://127\\.0\\.0\\.1:55433/\\?replicaSet=farmacia-teste")
@Transactional
class SaldoEstornoIntegracaoTest {
    @Autowired CategoriaRepository categorias;
    @Autowired FabricanteRepository fabricantes;
    @Autowired MedicamentoRepository medicamentos;
    @Autowired UsuarioRepository usuarios;
    @Autowired LoteRepository lotes;
    @Autowired MovimentacaoEstoqueRepository movimentos;
    @Autowired EstoqueService estoque;
    @Autowired VendaService vendas;

    private Medicamento medicamento() {
        String sufixo = UUID.randomUUID().toString();
        var categoria = categorias.saveAndFlush(new Categoria("Categoria " + sufixo));
        var fabricante = fabricantes.saveAndFlush(new Fabricante("Fabricante " + sufixo));
        return medicamentos.saveAndFlush(new Medicamento("Fictício", null, categoria, fabricante,
                TipoMedicamento.GENERICO, new BigDecimal("10.00"), 0));
    }

    private Lote lote(Medicamento medicamento, int dias, int quantidade) {
        var lote = new Lote(medicamento, "LOTE-" + dias, LocalDate.now().plusDays(dias));
        lote.adicionar(quantidade);
        return lotes.saveAndFlush(lote);
    }

    @Test
    void saldoAgregadoIncluiHojeESeparaVencidosMesmoSemLotes() {
        var medicamento = medicamento();
        var vazio = estoque.consultarSaldo(medicamento.getId());
        assertEquals(0, vazio.saldoTotal());
        assertEquals(0, vazio.saldoDisponivel());
        assertEquals(0, vazio.saldoVencido());
        lote(medicamento, -1, 2);
        lote(medicamento, 0, 4);
        lote(medicamento, 1, 3);

        var saldo = estoque.consultarSaldo(medicamento.getId());
        assertEquals(9, saldo.saldoTotal());
        assertEquals(7, saldo.saldoDisponivel());
        assertEquals(2, saldo.saldoVencido());
    }

    @Test
    void estornoSegueOrdemDosMedicamentosMesmoComValidadesInvertidas() {
        var primeiro = medicamento();
        var segundo = medicamento();
        var lotePrimeiro = lote(primeiro, 30, 5);
        var loteSegundo = lote(segundo, 10, 5);
        var usuario = usuarios.saveAndFlush(new Usuario("Fictício", "teste-" + UUID.randomUUID(),
                "hash-ficticio", Perfil.ADMIN));
        var venda = vendas.registrar(new VendaRequest(List.of(
                new ItemVendaRequest(segundo.getId(), 1, segundo.getPreco()),
                new ItemVendaRequest(primeiro.getId(), 1, primeiro.getPreco())), FormaPagamento.PIX),
                usuario.getLogin());

        var saidas = movimentos.findByVendaIdAndTipo(venda.id(), TipoMovimentacao.SAIDA);
        assertEquals(List.of(lotePrimeiro.getId(), loteSegundo.getId()),
                saidas.stream().map(m -> m.getLote().getId()).toList());
        vendas.cancelar(venda.id(), "Teste de ordem", usuario.getLogin(), true);
        assertEquals(5, lotes.findById(lotePrimeiro.getId()).orElseThrow().getQuantidadeAtual());
        assertEquals(5, lotes.findById(loteSegundo.getId()).orElseThrow().getQuantidadeAtual());
    }
}
