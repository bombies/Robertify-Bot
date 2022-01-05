package main.utils.spotify;

import lombok.SneakyThrows;
import main.main.Robertify;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.net.URI;

public class SpotifyAuthorizationUtils {
    private static final String code = "";

    private static final AuthorizationCodeUriRequest ACUR = Robertify.getSpotifyApi().authorizationCodeUri()
            .build();
    private static final AuthorizationCodeRequest ACR = Robertify.getSpotifyApi().authorizationCode(code)
            .build();
    private static final AuthorizationCodeRefreshRequest ACRR = Robertify.getSpotifyApi().authorizationCodeRefresh()
            .build();
    private static final ClientCredentialsRequest clientCredentialsRequest = Robertify.getSpotifyApi().clientCredentials()
            .build();

    private static URI getAuthenticationCodeURI() {
        return ACUR.execute();
    }

    @SneakyThrows
    private static AuthorizationCodeCredentials getAuthorizationCodeCredentials() {
        return ACR.execute();
    }

    @SneakyThrows
    private static ClientCredentials getClientCredentials() {
        return clientCredentialsRequest.execute();
    }

    public static void setTokens() {
        Robertify.getSpotifyApi().setAccessToken(getClientCredentials().getAccessToken());
    }

    private static class RefreshSpotifyToken implements Runnable {

        @Override
        public void run() {
            SpotifyAuthorizationUtils.setTokens();
        }
    }

    public static Runnable doTokenRefresh() {
        return new RefreshSpotifyToken();
    }
}
