package main.utils.genius;

import lombok.Getter;
import main.constants.BotConstants;
import main.constants.ENV;
import main.main.Config;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

public class GeniusSongSearch {

    @Getter
    private final GeniusAPI gla;
    @Getter
    private int status;
    @Getter
    private int nextPage;
    private final LinkedList<GeniusSongSearch.Hit> hits = new LinkedList<>();

    public GeniusSongSearch(GeniusAPI gla, String query) {
        this.gla = gla;
        query = URLEncoder.encode(query, StandardCharsets.UTF_8);
        try {
            URI uri = new URI("https://genius.com/api/search/song?page=1&q=" + query);
            request(uri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public GeniusSongSearch(GeniusAPI gla, String query, int page) throws IOException {
        this.gla = gla;
        query = URLEncoder.encode(query, StandardCharsets.UTF_8);
        try {
            URI uri = new URI("https://genius.com/api/search/song?page=" + page + "&q=" + query);
            request(uri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void request(URI uri) {
        try {
            Request request = new Request.Builder()
                    .url(uri.toURL())
                    .get()
                    .addHeader("User-Agent", BotConstants.USER_AGENT.toString())
                    .addHeader("Content-Type","application/json")
                    .addHeader("Accept","application/json")
                    .addHeader("x-rapidapi-host", "genius.p.rapidapi.com")
                    .addHeader("x-rapidapi-key", Config.get(ENV.GENIUS_API_KEY))
                    .build();

            String result;

            try {
                Response response = new OkHttpClient().newCall(request).execute();
                result = response.body().string();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            parse(new JSONObject(result));
        } catch (MalformedURLException e) {
            throw new InternalError();
        }
    }

    private void parse(JSONObject jRoot) {
        this.status = jRoot.getJSONObject("meta").getInt("status");
        JSONObject response = jRoot.getJSONObject("response");
        if (!response.isNull("next_page")) {
            this.nextPage = response.getInt("next_page");
        }
        JSONObject section = response.getJSONArray("sections").getJSONObject(0);
        JSONArray hits = section.getJSONArray("hits");
        for (int i = 0; i < hits.length(); i++) {
            JSONObject hitRoot = hits.getJSONObject(i).getJSONObject("result");
            this.hits.add(new Hit(hitRoot));
        }
    }


    public LinkedList<GeniusSongSearch.Hit> getHits() {
        return hits;
    }

    public class Hit {

        @Getter
        private final long id;
        @Getter
        private final String title;
        @Getter
        private final String titleWithFeatured;
        @Getter
        private final String url;
        @Getter
        private final String imageUrl;
        @Getter
        private final String thumbnailUrl;
        @Getter
        private final GeniusSongSearch.Artist artist;

        public Hit(JSONObject jRoot) {
            this.id = jRoot.getLong("id");
            this.title = jRoot.getString("title");
            this.titleWithFeatured = jRoot.getString("title_with_featured");
            this.url = jRoot.getString("url");
            this.imageUrl = jRoot.getString("header_image_url");
            this.thumbnailUrl = jRoot.getString("song_art_image_thumbnail_url");
            this.artist = new Artist(jRoot.getJSONObject("primary_artist"));
        }

        public String fetchLyrics() {
            return new GeniusLyricsParser(GeniusSongSearch.this.gla).get(this.id + "");
        }

    }

    public static class Artist {

        @Getter
        private final long id;
        @Getter
        private final String imageUrl;
        @Getter
        private final String name;
        @Getter
        private final String slug;
        @Getter
        private final String url;

        public Artist(JSONObject jRoot) {
            this.id = jRoot.getLong("id");
            this.imageUrl = jRoot.getString("image_url");
            this.name = jRoot.getString("name");
            this.slug = jRoot.getString("slug");
            this.url = jRoot.getString("url");
        }
    }
}
