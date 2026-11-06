package com.farmacia.farmacia.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public record ReceitaRequest(@NotNull Long clienteId, @NotBlank @Size(max=100) String prescritor,
    @NotBlank @Size(max=80) String numero, @NotNull @PastOrPresent LocalDate emitidaEm,
    @NotNull LocalDate validadeAte, @Size(max=2000) String observacoes,
    @NotEmpty @Size(max=20) List<@NotNull @Valid ItemReceitaRequest> itens) {}
