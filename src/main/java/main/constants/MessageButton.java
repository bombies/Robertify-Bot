package main.constants;

public enum MessageButton {
    PAGE_ID("page"),
    FRONT(PAGE_ID + "front"),
    PREVIOUS(PAGE_ID + "previous"),
    NEXT(PAGE_ID + "next"),
    END(PAGE_ID + "end");

    private final String str;

    MessageButton(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
