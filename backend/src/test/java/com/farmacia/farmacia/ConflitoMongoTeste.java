package com.farmacia.farmacia;

import com.farmacia.farmacia.exception.ConflitosMongo;
import org.springframework.web.server.ResponseStatusException;

final class ConflitoMongoTeste {
    private ConflitoMongoTeste() {}

    static int status(RuntimeException erro) {
        if (erro instanceof ResponseStatusException resposta) return resposta.getStatusCode().value();
        if (ConflitosMongo.concorrente(erro)) return 409;
        throw erro;
    }
}
