package api.constants;

public enum ApiHeaders {
    AUTHORIZATION("Authorization");

    private final String header;

    private ApiHeaders(String header) {
        this.header = header;
    }

    @Override
    public String toString() {
        return this.header;
    }
}
