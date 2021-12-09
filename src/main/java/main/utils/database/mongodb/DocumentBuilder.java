package main.utils.database.mongodb;

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

    public Document build() {
        return Document.parse(obj.toString());
    }
}
