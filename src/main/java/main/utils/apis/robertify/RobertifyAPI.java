package main.utils.apis.robertify;

import lombok.SneakyThrows;
import main.constants.ENV;
import main.main.Config;
import main.utils.apis.robertify.models.RobertifyGuild;
import main.utils.apis.robertify.models.RobertifyPremium;
import me.duncte123.botcommons.web.ContentType;
import me.duncte123.botcommons.web.WebUtils;
import net.dv8tion.jda.api.entities.User;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.util.List;

public class RobertifyAPI {
    private final Logger logger = LoggerFactory.getLogger(RobertifyAPI.class);

    private final String masterPassword;
    private final URI uri;
    private final WebUtils webUtils;
    private final String accessToken;

    private final String AUTHORIZATION_HEADER = "Authorization";

    @SneakyThrows
    public RobertifyAPI() {
        this.webUtils = WebUtils.ins;
        this.masterPassword = Config.get(ENV.ROBERTIFY_API_PASSWORD);
        this.uri = new URIBuilder(Config.get(ENV.ROBERTIFY_API_HOSTNAME)).build();
        this.accessToken = getAccessToken();
    }

    @SneakyThrows
    private String getAccessToken() {
        Response response = webUtils.getClient()
                .newCall(webUtils.prepareGet(new URIBuilder(uri.toString())
                                .appendPath("auth")
                                .appendPath("login")
                                .toString()
                        )
                        .post(RequestBody.create(
                                ContentType.JSON.toMediaType(),
                                new JSONObject()
                                        .put("username", "bombies")
                                        .put("password", masterPassword)
                                        .toString()
                        ))
                .build())
                .execute();
        final var responseObj = new JSONObject(response.body().string());
        if (responseObj.has("access_token"))
            return responseObj.getString("access_token");
        else
            throw new IllegalStateException(responseObj.getString("message"));
    }

    @SneakyThrows
    public RobertifyGuild getGuild(long gid) {
        if (accessToken == null)
            throw new AccessDeniedException("There was no access token set for the API wrapper! I am not able to make any requests.");

        try (Response guild = webUtils.getClient().newCall(
                webUtils.prepareGet(new URIBuilder(uri.toString()).appendPathSegments("guild", String.valueOf(gid)).toString())
                        .addHeader(AUTHORIZATION_HEADER, getBearerToken())
                        .build()
        ).execute()) {
            final var guildObj = new JSONObject(guild.body().string());

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
    }

    @SneakyThrows
    public Response postCommandInfo(JSONObject commandInfo) {
        if (accessToken == null)
            throw new AccessDeniedException("There was no access token set for the API wrapper! I am not able to make any requests.");
        return webUtils.getClient().newCall(webUtils.prepareGet(new URIBuilder(uri.toString()).appendPath("commands").toString())
                .addHeader(AUTHORIZATION_HEADER, getBearerToken())
                .post(RequestBody.create(
                        ContentType.JSON.toMediaType(),
                        commandInfo.toString()
                ))
                .build()).execute();
    }

    @SneakyThrows
    public RobertifyPremium getPremiumInfo(long userId) {
        if (accessToken == null)
            throw new AccessDeniedException("There was no access token set for the API wrapper! I am not able to make any requests.");

        Response premiumInfo = webUtils.getClient().newCall(
                webUtils.prepareGet(new URIBuilder(uri.toString()).appendPathSegments("premium", String.valueOf(userId)).toString())
                        .addHeader(AUTHORIZATION_HEADER, getBearerToken())
                        .build()
        ).execute();

        if (premiumInfo.code() == 404) {
            premiumInfo.close();
            return null;
        }

        String error = null;
        if (premiumInfo.code() != 200)
            error = new JSONObject(premiumInfo.body().string()).getJSONObject("error").getString("message");

        if (error != null) {
            premiumInfo.close();
            throw new IllegalArgumentException(error);
        }

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
        if (accessToken == null)
            throw new AccessDeniedException("There was no access token set for the API wrapper! I am not able to make any requests.");

        Response response = webUtils.getClient().newCall(webUtils.prepareGet(new URIBuilder(uri.toString()).appendPath("premium").toString())
                .addHeader(AUTHORIZATION_HEADER, getBearerToken())
                .post(RequestBody.create(
                        ContentType.JSON.toMediaType(),
                        new JSONObject()
                                .put("user_id", String.valueOf(userId))
                                .put("user_email", "thisdocumentwasforced@email.com")
                                .put("premium_type", premiumType)
                                .put("premium_tier", premiumTier)
                                .put("premium_started", String.valueOf(System.currentTimeMillis()))
                                .put("premium_expires", String.valueOf(premiumExpires))
                                .toString()
                ))
                .build()).execute();

        String error = null;
        if (response.code() != 200) {
            JSONObject jsonObject = new JSONObject(response.body().string());
            if (jsonObject.get("error") instanceof String)
                error = jsonObject.getString("error");
            else
                error = jsonObject.getJSONObject("error").getString("message");
        }
        response.close();

        if (error != null)
            throw new IllegalArgumentException(error);
    }

    @SneakyThrows
    public void deletePremiumUser(long userId) {
        if (accessToken == null)
            throw new AccessDeniedException("There was no access token set for the API wrapper! I am not able to make any requests.");

        Response response = webUtils.getClient().newCall(
                webUtils.prepareGet(new URIBuilder(uri.toString()).appendPath("premium").appendPath(String.valueOf(userId)).toString())
                        .addHeader(AUTHORIZATION_HEADER, getBearerToken())
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
        if (accessToken == null)
            throw new AccessDeniedException("There was no access token set for the API wrapper! I am not able to make any requests.");

        final var premiumInfo = getPremiumInfo(userId);
        if (premiumInfo == null)
            throw new IllegalArgumentException("There is no information for user with that ID!");
        Response response = webUtils.getClient().newCall(webUtils.prepareGet(new URIBuilder(uri.toString()).appendPath("premium").toString())
                .addHeader(AUTHORIZATION_HEADER, getBearerToken())
                .patch(RequestBody.create(
                        MediaType.get("application/json"),
                        new JSONObject()
                                .put("user_id", String.valueOf(userId))
                                .put("user_email", premiumInfo.getEmail())
                                .put("premium_type", premiumInfo.getType())
                                .put("premium_tier", tier)
                                .put("premium_servers", new JSONArray())
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

    public boolean guildIsPremium(long gid) {
        return guildIsPremium(String.valueOf(gid));
    }

    @SneakyThrows
    public boolean guildIsPremium(String gid) {
        return getPremiumGuilds().contains(gid);
    }

    @SneakyThrows
    private List<String> getPremiumGuilds() {
        final var response = webUtils.getClient().newCall(webUtils.prepareGet(new URIBuilder(uri.toString()).appendPath("premium").appendPath("guilds").toString())
                        .addHeader(AUTHORIZATION_HEADER, getBearerToken())
                        .build())
                .execute();
        final var responseObj = new JSONArray(response.body().string());
        response.close();
        return responseObj.toList()
                .stream()
                .map(String::valueOf)
                .toList();
    }

    public RobertifyPremium getGuildPremiumSetter(long gid) {
        return getGuildPremiumSetter(String.valueOf(gid));
    }

    @SneakyThrows
    public RobertifyPremium getGuildPremiumSetter(String gid) {
        final var response = webUtils.getClient().newCall(webUtils.prepareGet(new URIBuilder(uri.toString()).appendPath("premium").appendPath("guilds").appendPath("user").appendPath(gid).toString())
                        .addHeader(AUTHORIZATION_HEADER, getBearerToken())
                        .build())
                .execute();

        if (response.code() == HttpStatus.SC_NOT_FOUND)
            throw new NullPointerException("Guild with ID " + gid + " isn't premium.");
        final var responseObj = new JSONObject(response.body().string());
        response.close();
        if (responseObj.has("error"))
            throw new IllegalArgumentException(responseObj.getString("error"));
        return new RobertifyPremium(
                responseObj.getString("user_id"),
                responseObj.getString("user_email"),
                responseObj.getInt("premium_type"),
                responseObj.getInt("premium_tier"),
                responseObj.getJSONArray("premium_servers").toList().stream().map(String::valueOf).toList(),
                responseObj.getString("premium_started"),
                responseObj.getString("premium_expires")
        );
    }

    private String getBearerToken() {
        return "Bearer " + this.accessToken;
    }
}
