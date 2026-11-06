package com.farmacia.farmacia.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.farmacia.farmacia.controller.ConfiguracaoController;
import com.farmacia.farmacia.dto.*;
import jakarta.validation.Validation;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
class DemoValidacaoTest {
    @Test void validaCamposDeEntradaSemAceitarItensNulos() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            assertFalse(validator.validate(new ClienteRequest(" ",null,"email-invalido")).isEmpty());
            assertFalse(validator.validate(new AtendimentoRequest(null," "," ")).isEmpty());
            assertFalse(validator.validate(new ItemReceitaRequest(1L,0)).isEmpty());
            assertFalse(validator.validate(new ItemReceitaRequest(1L,10000)).isEmpty());
            assertFalse(validator.validate(new ReceitaRequest(1L,"Exemplo","1",LocalDate.now(),LocalDate.now(),null,Arrays.asList((ItemReceitaRequest)null))).isEmpty());
            assertFalse(validator.validate(new ConfiguracaoRequest(" ",null,"invalido",null)).isEmpty());
            assertTrue(validator.validate(new ClienteRequest("Exemplo",null,null)).isEmpty());
        }
    }
    @Test void configuracaoExigeAdministrador() {
        var service=mock(ConfiguracaoService.class);
        var controller=new ConfiguracaoController(service);
        var vendedor=new UsernamePasswordAuthenticationToken("demo","",List.of(new SimpleGrantedAuthority("ROLE_VENDEDOR")));
        var request=new ConfiguracaoRequest("Farmácias Ruanna",null,null,null);
        assertEquals(403,assertThrows(ResponseStatusException.class,()->controller.atualizar(request,vendedor)).getStatusCode().value());
        verifyNoInteractions(service);
        var admin=new UsernamePasswordAuthenticationToken("demo","",List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        controller.atualizar(request,admin);
        verify(service).atualizar(request);
    }
}
