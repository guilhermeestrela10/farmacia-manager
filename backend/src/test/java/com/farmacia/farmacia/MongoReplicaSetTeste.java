package com.farmacia.farmacia;

import com.mongodb.client.MongoClients;
import java.util.List;
import org.bson.Document;

/** Inicializa somente o mongod descartável criado por testar.ps1. */
public final class MongoReplicaSetTeste {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].equals("mongodb://127.0.0.1:55433/?replicaSet=farmacia-teste")) {
            throw new IllegalArgumentException("Inicialização permitida somente no replica set local de teste.");
        }
        try (var client = MongoClients.create("mongodb://127.0.0.1:55433/?directConnection=true&serverSelectionTimeoutMS=15000")) {
            var admin = client.getDatabase("admin");
            admin.runCommand(new Document("replSetInitiate", new Document("_id", "farmacia-teste")
                    .append("members", List.of(new Document("_id", 0).append("host", "127.0.0.1:55433")))));
            for (int tentativa = 0; tentativa < 60; tentativa++) {
                if (Boolean.TRUE.equals(admin.runCommand(new Document("hello", 1)).getBoolean("isWritablePrimary"))) return;
                Thread.sleep(500);
            }
            throw new IllegalStateException("O replica set descartável não elegeu primário.");
        }
    }
}
