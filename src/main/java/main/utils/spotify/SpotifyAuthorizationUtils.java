package main.utils.spotify;

import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import lombok.SneakyThrows;
import main.main.Robertify;

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
}
