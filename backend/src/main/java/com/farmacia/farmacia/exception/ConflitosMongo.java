package com.farmacia.farmacia.exception;

import com.mongodb.MongoException;
import org.springframework.dao.OptimisticLockingFailureException;

public final class ConflitosMongo {
    private ConflitosMongo() {}

    public static boolean concorrente(Throwable erro) {
        for (Throwable causa=erro; causa!=null; causa=causa.getCause()) {
            if (causa instanceof OptimisticLockingFailureException) return true;
            if (causa instanceof MongoException mongo && (mongo.getCode()==112
                    || mongo.hasErrorLabel("TransientTransactionError"))) return true;
        }
        return false;
    }
}
