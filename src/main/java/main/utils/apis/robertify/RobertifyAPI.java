package main.utils.apis.robertify;

import lombok.SneakyThrows;
import main.constants.ENV;
import main.main.Config;
import main.utils.apis.robertify.models.RobertifyGuild;
import me.duncte123.botcommons.web.WebUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class RobertifyAPI {
    private final Logger logger = LoggerFactory.getLogger(RobertifyAPI.class);

    private final String masterPassword;
    private final URI uri;
    private final WebUtils webUtils;
    private final String accessToken;

    @SneakyThrows
    public RobertifyAPI() {
        this.webUtils = WebUtils.ins;
        this.masterPassword = Config.get(ENV.ROBERTIFY_API_PASSWORD);
        this.uri = new URIBuilder(Config.get(ENV.ROBERTIFY_API_HOSTNAME)).build();
        this.accessToken = getAccessToken();
    }

    @SneakyThrows
    private String getAccessToken() {
        Response response = webUtils.getClient().newCall(webUtils.prepareGet(new URIBuilder(uri.toString()).appendPath("login").toString())
                .post(RequestBody.create(
                        MediaType.get("application/json"),
                        "{ \"user_name\": \"bombies\", \"master_password\": \"" + masterPassword + "\" }"
                ))
                .build()).execute();
        final var responseObj = new JSONObject(response.body().string());
        if (responseObj.has("token"))
            return responseObj.getString("token");
        else
            throw new IllegalStateException(responseObj.getString("message"));
    }

    @SneakyThrows
    public RobertifyGuild getGuild(long gid) {
        Response guild = webUtils.getClient().newCall(
                webUtils.prepareGet(new URIBuilder(uri.toString()).appendPathSegments("guilds", String.valueOf(gid)).toString())
                        .addHeader("auth-token", accessToken)
                        .build()
        ).execute();

        final var guildObj = new JSONObject(guild.body().string());

        guild.close();

        if (guildObj.has("message"))
            throw new IllegalArgumentException(guildObj.getString("message"));

        return new RobertifyGuild(
                guildObj.getJSONObject("dedicated_channel"),
                guildObj.getJSONObject("restricted_channels"),
                guildObj.getString("prefix"),
                guildObj.getJSONObject("permissions"),
                guildObj.getJSONObject("toggles"),
                guildObj.getJSONArray("eight_ball"),
                guildObj.getJSONObject("announcement_channel"),
                guildObj.getString("theme"),
                guildObj.getJSONObject("server_id"),
                guildObj.getJSONArray("banned_users")
        );
    }

    @SneakyThrows
    public void postCommandInfo(JSONObject commandInfo) {
        Response response = webUtils.getClient().newCall(webUtils.prepareGet(new URIBuilder(uri.toString()).appendPath("commands").toString())
                .addHeader("auth-token", accessToken)
                .post(RequestBody.create(
                        MediaType.get("application/json"),
                        commandInfo.toString()
                ))
                .build()).execute();

        System.out.println(response.body().string());
    }
}
