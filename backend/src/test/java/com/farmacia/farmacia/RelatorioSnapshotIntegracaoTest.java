package com.farmacia.farmacia;

import static org.junit.jupiter.api.Assertions.*;
import com.farmacia.farmacia.model.*;
import com.farmacia.farmacia.repository.*;
import com.farmacia.farmacia.service.RelatorioService;
import com.farmacia.farmacia.service.VendaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "farmacia.test.mongo-uri", matches = "mongodb://127\\.0\\.0\\.1:55433/\\?replicaSet=farmacia-teste")
class RelatorioSnapshotIntegracaoTest {
    @Autowired CategoriaRepository categorias;
    @Autowired FabricanteRepository fabricantes;
    @Autowired MedicamentoRepository medicamentos;
    @Autowired UsuarioRepository usuarios;
    @Autowired VendaRepository vendas;
    @Autowired VendaService vendaService;
    @Autowired RelatorioService relatorios;
    @Autowired PlatformTransactionManager transacoes;

    @Test
    void cancelamentoDuranteLeituraNaoMisturaTotaisDeSnapshotsDiferentes() {
        String sufixo = UUID.randomUUID().toString();
        var categoria = categorias.saveAndFlush(new Categoria("Snapshot " + sufixo));
        var fabricante = fabricantes.saveAndFlush(new Fabricante("Snapshot " + sufixo));
        var medicamento = medicamentos.saveAndFlush(new Medicamento("Snapshot", null, categoria, fabricante,
                TipoMedicamento.GENERICO, new BigDecimal("10.00"), 0));
        var usuario = usuarios.saveAndFlush(new Usuario("Snapshot", "snap-" + sufixo, "hash-ficticio", Perfil.VENDEDOR));
        LocalDate dia = LocalDate.of(2000, 1, 15);
        var venda = new Venda(usuario, FormaPagamento.PIX);
        venda.adicionarItem(medicamento, 1, medicamento.getPreco());
        ReflectionTestUtils.setField(venda, "criadaEm", dia.atStartOfDay(ZoneId.systemDefault()).toInstant());
        vendas.saveAndFlush(venda);

        var cancelou = new AtomicBoolean();
        var outraTransacao = new TransactionTemplate(transacoes);
        outraTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        VendaRepository repositorio = AopTestUtils.getTargetObject(vendas);
        var fabrica = new ProxyFactory(repositorio);
        fabrica.setProxyTargetClass(true);
        fabrica.addAdvice((MethodInterceptor) chamada -> {
            Object resultado = chamada.proceed();
            if (chamada.getMethod().getName().equals("findAll") && cancelou.compareAndSet(false, true)) {
                outraTransacao.executeWithoutResult(status -> vendaService.cancelar(
                        venda.getId(), "Cancelamento durante relatório", usuario.getLogin(), true));
            }
            return resultado;
        });
        Object alvo = AopTestUtils.getTargetObject(relatorios);
        ReflectionTestUtils.setField(alvo, "vendaRepository", fabrica.getProxy());
        try {
            var relatorio = relatorios.vendas(dia, dia);
            assertTrue(cancelou.get());
            assertEquals(1, relatorio.quantidadeVendas());
            assertEquals(0, relatorio.quantidadeCanceladas());
            assertEquals(0, relatorio.totalVendido().compareTo(new BigDecimal("10.00")));
            assertEquals(0, relatorio.porCategoria().getFirst().total().compareTo(relatorio.totalVendido()));
        } finally {
            ReflectionTestUtils.setField(alvo, "vendaRepository", vendas);
        }
        assertEquals(StatusVenda.CANCELADA, vendas.findById(venda.getId()).orElseThrow().getStatus());
    }
}
