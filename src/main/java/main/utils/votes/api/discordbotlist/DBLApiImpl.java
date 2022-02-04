package main.utils.votes.api.discordbotlist;

import com.fatboyindustrial.gsonjavatime.OffsetDateTimeConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.discordbots.api.client.io.DefaultResponseTransformer;
import org.discordbots.api.client.io.ResponseTransformer;
import org.discordbots.api.client.io.UnsuccessfulHttpException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class DBLApiImpl implements DBLApi {
    private static final HttpUrl baseUrl = new HttpUrl.Builder().scheme("https").host("discordbotlist.com").addPathSegment("api").addPathSegment("v1").build();
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String token;
    private final String botID;

    public DBLApiImpl(String token, String botID) {
        this.token = token;
        this.botID = botID;
        this.gson = new GsonBuilder().registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeConverter()).create();
        this.httpClient = new OkHttpClient.Builder().addInterceptor(chain -> {
            Request request = chain.request().newBuilder().addHeader("Authorization", token).build();
            return chain.proceed(request);
        }).build();
    }

    @Override
    public CompletionStage<Void> setStats(int guilds) {
        return setStats(new JSONObject().put("guilds", guilds));
    }

    private CompletionStage<Void> setStats(JSONObject body) {
        HttpUrl url = baseUrl.newBuilder().addPathSegment("bots").addPathSegment(botID).addPathSegment("stats").build();
        return post(url, body, Void.class);
    }

    private <E> CompletionStage<E> get(HttpUrl url, Class<E> clazz) {
        return get(url, new DefaultResponseTransformer<>(clazz, gson));
    }

    private <E> CompletionStage<E> get(HttpUrl url, ResponseTransformer<E> responseTransformer) {
        Request request = new Request.Builder().get().url(url).build();
        return execute(request, responseTransformer);
    }

    private <E> CompletionStage<E> post(HttpUrl url, JSONObject jsonBody, Class<E> clazz) {
        return post(url, jsonBody, new DefaultResponseTransformer<>(clazz, gson));
    }

    private <E> CompletionStage<E> post(HttpUrl url, JSONObject jsonBody, ResponseTransformer<E> responseTransformer) {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, jsonBody.toString());
        Request request = new Request.Builder().post(body).url(url).build();
        return this.execute(request, responseTransformer);
    }

    private <E> CompletionStage<E> execute(Request request, final ResponseTransformer<E> responseTransformer) {
        Call call = httpClient.newCall(request);
        final CompletableFuture<E> future = new CompletableFuture<>();
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        E transformed = responseTransformer.transform(response);
                        future.complete(transformed);
                    } else {
                        String message = response.message();

                        if (message.isEmpty()) {
                            JSONObject body = new JSONObject(response.body().string());
                            message = body.getString("error");
                        }

                        Exception e = new UnsuccessfulHttpException(response.code(), message);
                        future.completeExceptionally(e);
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    response.body().close();
                }
            }
        });
        return future;
    }
}
