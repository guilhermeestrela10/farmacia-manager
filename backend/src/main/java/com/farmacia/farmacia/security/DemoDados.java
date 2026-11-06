package com.farmacia.farmacia.security;

import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.model.*;
import com.farmacia.farmacia.repository.*;
import com.farmacia.farmacia.service.*;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
@Order(0)
public class DemoDados implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final CategoriaService categorias;
    private final FabricanteService fabricantes;
    private final MedicamentoService medicamentos;
    private final MedicamentoRepository catalogo;
    private final LoteRepository lotes;
    private final MovimentacaoEstoqueRepository movimentos;
    private final EstoqueService estoque;
    private final VendaService vendas;
    private final ClienteService clientes;
    private final AtendimentoService atendimentos;
    private final ReceitaService receitas;
    private final DispensacaoService dispensacoes;
    private final ConfiguracaoService configuracao;
    private final JdbcTemplate jdbc;

    public DemoDados(UsuarioRepository usuarios, PasswordEncoder encoder, CategoriaService categorias,
            FabricanteService fabricantes, MedicamentoService medicamentos, MedicamentoRepository catalogo,
            LoteRepository lotes, MovimentacaoEstoqueRepository movimentos, EstoqueService estoque,
            VendaService vendas, ClienteService clientes, AtendimentoService atendimentos,
            ReceitaService receitas, DispensacaoService dispensacoes, ConfiguracaoService configuracao,
            JdbcTemplate jdbc) {
        this.usuarios=usuarios; this.encoder=encoder; this.categorias=categorias;
        this.fabricantes=fabricantes; this.medicamentos=medicamentos; this.catalogo=catalogo;
        this.lotes=lotes; this.movimentos=movimentos; this.estoque=estoque; this.vendas=vendas;
        this.clientes=clientes; this.atendimentos=atendimentos; this.receitas=receitas;
        this.dispensacoes=dispensacoes; this.configuracao=configuracao; this.jdbc=jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.findByLogin("demo.admin").isPresent()) return;
        if (usuarios.count() != 0 || catalogo.count() != 0) {
            throw new IllegalStateException("O perfil demo exige um banco exclusivo e vazio. Não use bancos reais.");
        }
        usuarios.saveAndFlush(new Usuario("Administrador de demonstração", "demo.admin", senhaAleatoria(), Perfil.ADMIN));
        usuarios.saveAndFlush(new Usuario("Vendedor de demonstração", "demo.vendedor", senhaAleatoria(), Perfil.VENDEDOR));
        configuracao.atualizar(new ConfiguracaoRequest("Farmácia Manager • Demonstração", "", "", "Endereço fictício — ambiente de demonstração"));
        Long cuidados=categorias.criar(new CategoriaRequest("Cuidados e higiene")).id();
        Long medicamentosCategoria=categorias.criar(new CategoriaRequest("Medicamentos de exemplo")).id();
        Long fabricante=fabricantes.criar(new FabricanteRequest("Laboratório Fictício Demo")).id();
        String[] nomes={"Paracetamol 500 mg — exemplo", "Dipirona 500 mg — exemplo", "Solução fisiológica — exemplo",
                "Vitamina C — exemplo", "Curativo adesivo — exemplo", "Gaze estéril — exemplo",
                "Álcool 70% — exemplo", "Sabonete neutro — exemplo", "Protetor solar — exemplo",
                "Creme hidratante — exemplo", "Escova dental — exemplo", "Termômetro — exemplo"};
        List<Long> ids=new ArrayList<>();
        LocalDate hoje=LocalDate.now();
        for (int i=0; i<nomes.length; i++) {
            Long id=medicamentos.criar(new MedicamentoRequest(nomes[i], "DEMO-"+(1000+i),
                    i<4 ? medicamentosCategoria : cuidados, fabricante, TipoMedicamento.GENERICO,
                    BigDecimal.valueOf(650L+i*275L,2), i==11 ? 10 : 5)).id();
            ids.add(id);
            estoque.registrarEntrada(new EntradaEstoqueRequest(id,"DEMO-REGULAR-"+i,
                    hoje.plusDays(180+i*10L), i==11 ? 3 : 60, "Estoque inicial fictício", "Demonstração"));
        }
        estoque.registrarEntrada(new EntradaEstoqueRequest(ids.get(0),"DEMO-PROXIMO",
                hoje.plusDays(25),12,"Exemplo fictício de vencimento próximo","Demonstração"));
        var vencido=new Lote(catalogo.findById(ids.get(4)).orElseThrow(),"DEMO-VENCIDO",hoje.minusDays(10));
        vencido.adicionar(4);
        lotes.saveAndFlush(vencido);
        movimentos.saveAndFlush(new MovimentacaoEstoque(vencido,TipoMovimentacao.ENTRADA,4,
                "Fixture fictícia para demonstrar descarte de vencidos", "Demonstração"));
        for (int i=0; i<18; i++) {
            var venda=vendas.registrar(new VendaRequest(List.of(new ItemVendaRequest(ids.get(i%8),1+i%3,null)),
                    FormaPagamento.values()[i%4]),"demo.vendedor");
            var data=Timestamp.from(hoje.minusDays(i%14).atTime(10+i%8,15).atZone(ZoneId.of("America/Sao_Paulo")).toInstant());
            jdbc.update("UPDATE venda SET criada_em=? WHERE id=?",data,venda.id());
            jdbc.update("UPDATE movimentacao_estoque SET data_hora=? WHERE venda_id=?",data,venda.id());
        }
        Long ana=clientes.criar(new ClienteRequest("Cliente Fictícia Ana",null,null)).id();
        Long bruno=clientes.criar(new ClienteRequest("Cliente Fictício Bruno",null,null)).id();
        clientes.criar(new ClienteRequest("Cliente Fictícia Carla",null,null));
        atendimentos.criar(new AtendimentoRequest(ana,"Profissional fictício","Exemplo de registro manual. Sem orientação clínica."));
        atendimentos.criar(new AtendimentoRequest(bruno,"Profissional fictício","Exemplo fictício de atendimento de balcão."));
        Long receita=receitas.criar(new ReceitaRequest(ana,"Prescritor fictício","DEMO-001",hoje.minusDays(2),
                hoje.plusDays(30),"Exemplo de fluxo. Não representa receita válida ou indicação de tratamento.",
                List.of(new ItemReceitaRequest(ids.get(0),3)))).id();
        receitas.criar(new ReceitaRequest(bruno,"Prescritor fictício","DEMO-002",hoje.minusDays(5),hoje.plusDays(15),
                "Somente demonstração; não utilizar clinicamente.",List.of(new ItemReceitaRequest(ids.get(1),2))));
        dispensacoes.registrar(new DispensacaoRequest(receita,FormaPagamento.PIX,
                List.of(new ItemVendaRequest(ids.get(0),1,null))),"demo.vendedor");
    }

    private String senhaAleatoria() {
        byte[] bytes=new byte[32];
        new SecureRandom().nextBytes(bytes);
        return encoder.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }
}
