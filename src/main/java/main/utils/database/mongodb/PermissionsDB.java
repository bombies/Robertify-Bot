package main.utils.database.mongodb;

import main.commands.commands.management.permissions.Permission;
import main.constants.Database;
import main.utils.database.sqlite3.BotDB;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import org.json.JSONArray;
import org.json.JSONObject;

public class PermissionsDB extends AbstractMongoDatabase {

    public PermissionsDB() {
        super(Database.MONGO.ROBERTIFY_DATABASE, Database.MONGO.ROBERTIFY_PERMISSIONS);
    }

    public void init() {
        for (Guild g : new BotDB().getGuilds()) {
            if (findDocument(Fields.GUILD_ID.toString(), g.getId()) == null)
                continue;

            DocumentBuilder documentBuilder = DocumentBuilder.create();
            documentBuilder.addField(Fields.GUILD_ID.toString(), g.getId());

            JSONObject userArr = new JSONObject();

            for (Permission p : Permission.values())
                documentBuilder.addField(String.valueOf(p.getCode()), new JSONArray().toString());

            documentBuilder.addField(Fields.USERS.toString(), userArr.toString());

            addDocument(documentBuilder.build());
        }
    }

    public void addMentionableToPermission(String guildId, IMentionable mentionable, Permission permission) {
        var doc = findSpecificDocument(Fields.GUILD_ID.toString(), guildId);

        if (doc == null)
            throw new NullPointerException("There is no such guild with ID \""+guildId+"\"");

        JSONArray arr = (JSONArray) doc.get(String.valueOf(permission.getCode()));

        arr.put(mentionable.getIdLong());

        updateDocument(doc, String.valueOf(permission.getCode()), arr.toString());
    }

    enum Fields {
        GUILD_ID("guild_id"),
        USERS("users");

        private final String str;

        Fields(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

}
