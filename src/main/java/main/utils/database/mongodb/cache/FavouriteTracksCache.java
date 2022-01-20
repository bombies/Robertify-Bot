package main.utils.database.mongodb.cache;

import lombok.Getter;
import main.constants.TrackSource;
import main.utils.database.mongodb.DocumentBuilder;
import main.utils.database.mongodb.databases.FavouriteTracksDB;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FavouriteTracksCache extends AbstractMongoCache {
    private final Logger logger = LoggerFactory.getLogger(FavouriteTracksCache.class);

    @Getter
    private static FavouriteTracksCache instance;

    FavouriteTracksCache() {
        super(FavouriteTracksDB.ins());
        updateCache();
        logger.debug("Done instantiating Favourite Tracks cache");
    }

    public static void initCache() {
        instance = new FavouriteTracksCache();
    }

    public synchronized void addUser(long uid) {
        if (userHasInfo(uid))
            throw new IllegalArgumentException("This user already has information!");

        addToCache(getDefaultDocument(uid));
    }

    public synchronized void addTrack(long uid, String trackID, String title, String author,  TrackSource source) {
        if (!userHasInfo(uid))
            addUser(uid);

        if (trackIsAdded(uid, trackID))
            throw new IllegalArgumentException("This track has already been added!");

        JSONObject userInfo = getUserInfo(uid);
        JSONArray jsonArray = userInfo.getJSONArray(FavouriteTracksDB.Field.TRACKS_ARRAY.toString());
        jsonArray.put(
                new JSONObject()
                        .put(FavouriteTracksDB.Field.TRACK_ID.toString(), trackID)
                        .put(FavouriteTracksDB.Field.TRACK_TITLE.toString(), title)
                        .put(FavouriteTracksDB.Field.TRACK_AUTHOR.toString(), author)
                        .put(FavouriteTracksDB.Field.TRACK_SOURCE.toString(), source.name().toLowerCase())
        );

        updateCache(userInfo, FavouriteTracksDB.Field.USER_ID, uid);
    }

    public synchronized void removeTrack(long uid, int index) {
        if (!userHasInfo(uid))
            throw new NullPointerException("This user doesn't have any information");

        JSONObject userInfo = getUserInfo(uid);
        JSONArray jsonArray = userInfo.getJSONArray(FavouriteTracksDB.Field.TRACKS_ARRAY.toString());
        jsonArray.remove(index);

        updateCache(userInfo, FavouriteTracksDB.Field.USER_ID, uid);
    }

    public synchronized List<Track> getTracks(long uid) {
        if (!userHasInfo(uid))
            addUser(uid);

        final List<Track> ret = new ArrayList<>();
        final var arr = getUserInfo(uid).getJSONArray(FavouriteTracksDB.Field.TRACKS_ARRAY.toString());

        for (final var obj : arr) {
            final JSONObject actualObj = (JSONObject) obj;

            ret.add(
                    new Track(
                            actualObj.getString(FavouriteTracksDB.Field.TRACK_ID.toString()),
                            actualObj.getString(FavouriteTracksDB.Field.TRACK_TITLE.toString()),
                            actualObj.getString(FavouriteTracksDB.Field.TRACK_AUTHOR.toString()),
                            TrackSource.parse(actualObj.getString(FavouriteTracksDB.Field.TRACK_SOURCE.toString()))
                    )
            );
        }

        return ret;
    }

    public synchronized boolean trackIsAdded(long uid, String trackID) {
        if (!userHasInfo(uid)) {
            addUser(uid);
            return false;
        }

        if (getTracks(uid).isEmpty()) return false;

        for (final var track : getTracks(uid))
            if (track.id().equals(trackID))
                return true;
        return false;
    }



    public synchronized JSONObject getUserInfo(long userId) {
        if (!userHasInfo(userId)) return null;
        return getCache().getJSONObject(getIndexOfObjectInArray(getCache(), FavouriteTracksDB.Field.USER_ID, userId));
    }

    public synchronized boolean userHasInfo(long uid) {
        try {
            getCache().getJSONObject(getIndexOfObjectInArray(getCache(), FavouriteTracksDB.Field.USER_ID, uid));
            return true;
        } catch (JSONException | NullPointerException e) {
            return false;
        }
    }

    private Document getDefaultDocument(long userID) {
        return DocumentBuilder.create()
                .addField(FavouriteTracksDB.Field.USER_ID, userID)
                .addField(FavouriteTracksDB.Field.TRACKS_ARRAY, new JSONArray())
                .build();
    }

    public static record Track(String id, String title, String author, TrackSource source) {}
}
