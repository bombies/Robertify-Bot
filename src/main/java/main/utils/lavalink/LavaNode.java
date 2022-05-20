package main.utils.lavalink;

import lombok.Getter;

import java.net.URI;

public class LavaNode {
    private final String host;
    private final String port;
    @Getter
    private final String password;

    public LavaNode(String host, String port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public URI getURI() {
        return URI.create("ws://" + host + ((!port.isEmpty() && !port.isBlank()) ? ":" + port : ""));
    }

    @Override
    public String toString() {
        return "LavaNode{" +
                "host='" + host + '\'' +
                ", port='" + port + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
