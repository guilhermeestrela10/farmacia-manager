package com.farmacia.farmacia;

import static org.junit.jupiter.api.Assertions.*;
import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.model.*;
import com.farmacia.farmacia.repository.*;
import com.farmacia.farmacia.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named="farmacia.test.url",matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/postgres")
class DemoModulosIntegracaoTest {
    @Autowired CategoriaRepository categorias;
    @Autowired FabricanteRepository fabricantes;
    @Autowired MedicamentoRepository medicamentos;
    @Autowired UsuarioRepository usuarios;
    @Autowired ClienteService clientes;
    @Autowired AtendimentoService atendimentos;
    @Autowired ReceitaService receitas;
    @Autowired DispensacaoService dispensacoes;
    @Autowired VendaService vendas;
    @Autowired EstoqueService estoque;
    private record Cenario(Long clienteId, Long medicamentoId, Long receitaId, String login) {}
    private Cenario cenario(int prescrito,int saldo,int validade) {
        String uuid=UUID.randomUUID().toString();
        var categoria=categorias.saveAndFlush(new Categoria("Categoria "+uuid));
        var fabricante=fabricantes.saveAndFlush(new Fabricante("Fabricante "+uuid));
        var medicamento=medicamentos.saveAndFlush(new Medicamento("Fictício "+uuid,null,categoria,fabricante,TipoMedicamento.GENERICO,new BigDecimal("10.00"),0));
        var usuario=usuarios.saveAndFlush(new Usuario("Fictício","teste-"+uuid,"hash-ficticio",Perfil.VENDEDOR));
        var cliente=clientes.criar(new ClienteRequest("Cliente "+uuid,null,null));
        estoque.registrarEntrada(new EntradaEstoqueRequest(medicamento.getId(),uuid,LocalDate.now().plusDays(90),saldo,"Fixture fictícia","Teste"));
        var receita=receitas.criar(new ReceitaRequest(cliente.id(),"Exemplo manual","EXEMPLO",LocalDate.now().minusDays(5),LocalDate.now().plusDays(validade),null,List.of(new ItemReceitaRequest(medicamento.getId(),prescrito))));
        return new Cenario(cliente.id(),medicamento.getId(),receita.id(),usuario.getLogin());
    }
    private DispensacaoRequest pedido(Cenario c,int quantidade) {
        return new DispensacaoRequest(c.receitaId(),FormaPagamento.PIX,List.of(new ItemVendaRequest(c.medicamentoId(),quantidade,new BigDecimal("10.00"))));
    }
    @Test void dispensaECancelamentoRestauramSaldoPrescritoEEstoque() {
        var c=cenario(3,5,10);
        var dispensa=dispensacoes.registrar(pedido(c,2),c.login());
        assertEquals(1,receitas.buscar(c.receitaId()).itens().getFirst().restante());
        assertEquals(3,estoque.consultarSaldo(c.medicamentoId()).saldoDisponivel());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->dispensacoes.registrar(pedido(c,2),c.login())).getStatusCode().value());
        vendas.cancelar(dispensa.vendaId(),"Cancelamento de exemplo",c.login(),true);
        assertEquals(3,receitas.buscar(c.receitaId()).itens().getFirst().restante());
        assertEquals(5,estoque.consultarSaldo(c.medicamentoId()).saldoDisponivel());
        assertEquals(StatusVenda.CANCELADA,dispensacoes.listar().stream().filter(d->d.id().equals(dispensa.id())).findFirst().orElseThrow().statusVenda());
        dispensacoes.registrar(pedido(c,3),c.login());
        assertEquals(0,receitas.buscar(c.receitaId()).itens().getFirst().restante());
    }
    @Test void duasDispensasConcorrentesNaoUltrapassamReceita() throws Exception {
        var c=cenario(1,3,10);
        var inicio=new CountDownLatch(1);
        try (var executor=Executors.newFixedThreadPool(2)) {
            Callable<Integer> acao=()->{
                assertTrue(inicio.await(10,TimeUnit.SECONDS));
                try { dispensacoes.registrar(pedido(c,1),c.login()); return 201; }
                catch(ResponseStatusException ex) { return ex.getStatusCode().value(); }
            };
            var primeira=executor.submit(acao); var segunda=executor.submit(acao);
            inicio.countDown();
            var resultados=List.of(primeira.get(20,TimeUnit.SECONDS),segunda.get(20,TimeUnit.SECONDS));
            assertEquals(1,resultados.stream().filter(s->s==201).count());
            assertEquals(1,resultados.stream().filter(s->s==409).count());
        }
        assertEquals(0,receitas.buscar(c.receitaId()).itens().getFirst().restante());
        assertEquals(2,estoque.consultarSaldo(c.medicamentoId()).saldoDisponivel());
    }
    @Test void falhaDeEstoqueReverteDispensaEVenda() {
        var c=cenario(5,1,10);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->dispensacoes.registrar(pedido(c,2),c.login())).getStatusCode().value());
        assertEquals(5,receitas.buscar(c.receitaId()).itens().getFirst().restante());
        assertEquals(1,estoque.consultarSaldo(c.medicamentoId()).saldoDisponivel());
        assertTrue(dispensacoes.listar().stream().noneMatch(d->d.receitaId().equals(c.receitaId())));
    }
    @Test void clienteInativoEReceitaVencidaNaoDispensam() {
        var vencida=cenario(2,3,-1);
        assertEquals(422,assertThrows(ResponseStatusException.class,()->dispensacoes.registrar(pedido(vencida,1),vencida.login())).getStatusCode().value());
        var c=cenario(2,3,10);
        clientes.definirAtivo(c.clienteId(),false);
        assertEquals(422,assertThrows(ResponseStatusException.class,()->dispensacoes.registrar(pedido(c,1),c.login())).getStatusCode().value());
        assertThrows(ResponseStatusException.class,()->atendimentos.criar(new AtendimentoRequest(c.clienteId(),"Exemplo","Registro manual")));
    }
    @Test void rejeitaDuplicadosENaoPrescritos() {
        var c=cenario(2,3,10);
        assertEquals(400,assertThrows(ResponseStatusException.class,()->receitas.criar(new ReceitaRequest(c.clienteId(),"Exemplo","1",LocalDate.now(),LocalDate.now(),null,List.of(new ItemReceitaRequest(c.medicamentoId(),1),new ItemReceitaRequest(c.medicamentoId(),1))))).getStatusCode().value());
        var outro=cenario(1,3,10);
        var request=new DispensacaoRequest(c.receitaId(),FormaPagamento.PIX,List.of(new ItemVendaRequest(outro.medicamentoId(),1,new BigDecimal("10.00"))));
        assertEquals(422,assertThrows(ResponseStatusException.class,()->dispensacoes.registrar(request,c.login())).getStatusCode().value());
    }
    @Test void lotesFiltramSemDiferenciarCaixaESituacaoInvalidaFalha() {
        var c=cenario(1,3,10);
        var nome=medicamentos.findById(c.medicamentoId()).orElseThrow().getNome();
        assertEquals(1,estoque.listarLotes(nome.toUpperCase(Locale.ROOT),"PROXIMO").size());
        assertTrue(estoque.listarLotes(nome,"VENCIDO").isEmpty());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->estoque.listarLotes(null,"INVALIDA")).getStatusCode().value());
    }
}
