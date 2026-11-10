package com.farmacia.farmacia.config;

import com.farmacia.farmacia.model.*;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import java.lang.reflect.Field;
import org.bson.Document;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Component
public class MongoIdCallback implements BeforeConvertCallback<Object> {
    private final MongoCollection<Document> sequences;
    public MongoIdCallback(MongoDatabaseFactory factory) {
        // Use the raw collection without ClientSession; rollback may leave sequence gaps.
        this.sequences = factory.getMongoDatabase().getCollection("_sequences").withWriteConcern(WriteConcern.MAJORITY);
    }
    @Override public Object onBeforeConvert(Object entity, String collection) {
        if (!entity.getClass().getPackageName().equals("com.farmacia.farmacia.model")) return entity;
        Field version = ReflectionUtils.findField(entity.getClass(), "version");
        if (version != null) ReflectionUtils.makeAccessible(version);
        Object currentVersion = version == null ? null : ReflectionUtils.getField(version, entity);
        boolean isNew = currentVersion == null || Long.valueOf(0L).equals(currentVersion);
        assign(entity, collection, isNew);
        if (entity instanceof Venda venda) venda.getItens().forEach(item -> assign(item, "venda_item", isNew));
        if (entity instanceof Receita receita) receita.getItens().forEach(item -> assign(item, "item_receita", isNew));
        return entity;
    }
    private void assign(Object entity, String collection, boolean isNew) {
        Field field = ReflectionUtils.findField(entity.getClass(), "id");
        if (field == null) return;
        ReflectionUtils.makeAccessible(field);
        Object id = ReflectionUtils.getField(field, entity);
        if (id == null) {
            Document sequence = sequences.findOneAndUpdate(new Document("_id", collection),
                new Document("$inc", new Document("value", 1L)), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
            if (sequence == null) throw new IllegalStateException("Contador MongoDB ausente: " + collection);
            ReflectionUtils.setField(field, entity, ((Number) sequence.get("value")).longValue());
        } else if (isNew) {
            sequences.updateOne(new Document("_id", collection), new Document("$max", new Document("value", id)));
        }
    }
}
