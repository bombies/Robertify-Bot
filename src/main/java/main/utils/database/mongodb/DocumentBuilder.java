package main.utils.database.mongodb;

import lombok.SneakyThrows;
import main.utils.component.InvalidBuilderException;
import org.bson.Document;
import org.json.JSONObject;

public class DocumentBuilder {
    private final JSONObject obj;

    private DocumentBuilder() {
        obj = new JSONObject();
    }

    public static DocumentBuilder create() {
        return new DocumentBuilder();
    }

    public <T> DocumentBuilder addField(String key, T value) {
        obj.put(key, value);
        return this;
    }

    @SneakyThrows
    public Document build() {
        if (obj.isEmpty())
            throw new InvalidBuilderException("You cannot create a document with no fields!");

        return Document.parse(obj.toString());
    }
}
