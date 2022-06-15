package main.utils.apis.robertify;

import lombok.SneakyThrows;
import main.constants.ENV;
import main.main.Config;
import main.utils.apis.robertify.models.RobertifyGuild;
import main.utils.apis.robertify.models.RobertifyPremium;
import me.duncte123.botcommons.web.WebUtils;
import net.dv8tion.jda.api.entities.User;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONArray;
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
                        new JSONObject()
                                .put("user_name", "bombies")
                                .put("master_password", masterPassword)
                                .toString()
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
        webUtils.getClient().newCall(webUtils.prepareGet(new URIBuilder(uri.toString()).appendPath("commands").toString())
                .addHeader("auth-token", accessToken)
                .post(RequestBody.create(
                        MediaType.get("application/json"),
                        commandInfo.toString()
                ))
                .build()).execute().close();
    }

    @SneakyThrows
    public RobertifyPremium getPremiumInfo(long userId) {
        Response premiumInfo = webUtils.getClient().newCall(
                webUtils.prepareGet(new URIBuilder(uri.toString()).appendPathSegments("premium", String.valueOf(userId)).toString())
                        .addHeader("auth-token", accessToken)
                        .build()
        ).execute();

        if (premiumInfo.code() == 404) {
            premiumInfo.close();
            return null;
        }

        String error = null;
        if (premiumInfo.code() != 200)
            error = new JSONObject(premiumInfo.body().string()).getJSONObject("error").getString("message");
        premiumInfo.close();

        if (error != null)
            throw new IllegalArgumentException(error);

        final var premiumObj = new JSONObject(premiumInfo.body().string());
        premiumInfo.close();

        if (premiumObj.has("error"))
            throw new IllegalArgumentException(premiumObj.getJSONObject("error").getString("message"));

        return new RobertifyPremium(
                premiumObj.getString("user_id"),
                premiumObj.getString("user_email"),
                premiumObj.getInt("premium_type"),
                premiumObj.getInt("premium_tier"),
                premiumObj.getJSONArray("premium_servers").toList().stream().map(String::valueOf).toList(),
                premiumObj.getString("premium_started"),
                premiumObj.getString("premium_expires")
        );
    }

    public RobertifyPremium getPremiumInfo(User user) {
        return getPremiumInfo(user.getIdLong());
    }

    @SneakyThrows
    public void addPremiumUser(long userId, int premiumType, int premiumTier, long premiumExpires) {
        Response response = webUtils.getClient().newCall(webUtils.prepareGet(new URIBuilder(uri.toString()).appendPath("premium").toString())
                .addHeader("auth-token", accessToken)
                .post(RequestBody.create(
                        MediaType.get("application/json"),
                        new JSONObject()
                                .put("user_id", String.valueOf(userId))
                                .put("user_email", "")
                                .put("premium_type", premiumType)
                                .put("premium_tier", premiumTier)
                                .put("premium_started", String.valueOf(System.currentTimeMillis()))
                                .put("premium_expires", String.valueOf(premiumExpires))
                                .toString()
                ))
                .build()).execute();

        String error = null;
        if (response.code() != 200)
            error = new JSONObject(response.body().string()).getJSONObject("error").getString("message");
        response.close();

        if (error != null)
            throw new IllegalArgumentException(error);
    }

    @SneakyThrows
    public void deletePremiumUser(long userId) {
        Response response = webUtils.getClient().newCall(
                webUtils.prepareGet(new URIBuilder(uri.toString()).appendPath("premium").appendPath(String.valueOf(userId)).toString())
                        .addHeader("auth-token", accessToken)
                        .delete()
                        .build()
        ).execute();

        String error = null;
        if (response.code() != 200)
            error = new JSONObject(response.body().string()).getJSONObject("error").getString("message");
        response.close();

        if (error != null)
            throw new IllegalArgumentException(error);
    }

    @SneakyThrows
    public void updateUserTier(long userId, int tier) {
        final var premiumInfo = getPremiumInfo(userId);
        Response response = webUtils.getClient().newCall(webUtils.prepareGet(new URIBuilder(uri.toString()).appendPath("premium").toString())
                .addHeader("auth-token", accessToken)
                .patch(RequestBody.create(
                        MediaType.get("application/json"),
                        new JSONObject()
                                .put("user_id", String.valueOf(userId))
                                .put("user_email", premiumInfo.getEmail())
                                .put("premium_type", premiumInfo.getType())
                                .put("premium_tier", tier)
                                .put("premium_started", String.valueOf(premiumInfo.getStartedAt()))
                                .put("premium_expires", String.valueOf(premiumInfo.getEndsAt()))
                                .toString()
                ))
                .build()).execute();

        String error = null;
        if (response.code() != 200)
            error = new JSONObject(response.body().string()).getJSONObject("error").getString("message");
        response.close();

        if (error != null)
            throw new IllegalArgumentException(error);
    }
}
