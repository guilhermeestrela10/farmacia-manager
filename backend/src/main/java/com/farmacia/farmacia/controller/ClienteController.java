package com.farmacia.farmacia.controller;
import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
@RestController @RequestMapping("/clientes")
public class ClienteController {
    private final ClienteService service;
    public ClienteController(ClienteService service) { this.service=service; }
    @GetMapping public List<ClienteResponse> listar(@RequestParam(required=false) String busca) { return service.listar(busca); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ClienteResponse criar(@Valid @RequestBody ClienteRequest r) { return service.criar(r); }
    @PatchMapping("/{id}/ativar") public ClienteResponse ativar(@PathVariable Long id) { return service.definirAtivo(id,true); }
    @PatchMapping("/{id}/desativar") public ClienteResponse desativar(@PathVariable Long id) { return service.definirAtivo(id,false); }
}
