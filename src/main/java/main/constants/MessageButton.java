package main.constants;

public enum MessageButton {
    FRONT("front"),
    PREVIOUS("previous"),
    NEXT("next"),
    END("end");

    private final String str;

    MessageButton(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
