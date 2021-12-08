package main.utils.genius;

import java.io.IOException;

public class GeniusAPI {
    public GeniusSongSearch search(String query) throws IOException {
        return new GeniusSongSearch(this, query);
    }
}
