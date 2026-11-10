package com.farmacia.farmacia;

import static org.junit.jupiter.api.Assertions.*;
import com.mongodb.MongoException;
import org.junit.jupiter.api.Test;

class ConflitoMongoTesteTest {
    @Test void reconheceApenasConflitosTransitoriosENaoEscondeFalhas() {
        assertEquals(409, ConflitoMongoTeste.status(new MongoException(112, "WriteConflict")));
        var transitorio = new MongoException(251, "Transaction aborted");
        transitorio.addLabel("TransientTransactionError");
        assertEquals(409, ConflitoMongoTeste.status(new IllegalStateException(transitorio)));
        var inesperado = new MongoException(13, "Unauthorized");
        assertSame(inesperado, assertThrows(MongoException.class, () -> ConflitoMongoTeste.status(inesperado)));
    }
}
