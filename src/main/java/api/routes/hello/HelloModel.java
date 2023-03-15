package api.routes.hello;

import lombok.Getter;

public class HelloModel {
    @Getter
    private final String name;

    public HelloModel(String name) {
        this.name = name;
    }
}
