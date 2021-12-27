package main.commands.commands.management.permissions;

import java.util.ArrayList;
import java.util.List;

public enum Permission {
    ROBERTIFY_ADMIN(0),
    ROBERTIFY_DJ(1);

    private final int code;

    Permission(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static List<Integer> getCodes() {
        List<Integer> codes = new ArrayList<>();
        for (Permission p : Permission.values())
            codes.add(p.getCode());
        return codes;
    }

    public static List<String> getPermissions() {
        List<String> ret = new ArrayList<>();
        for (Permission p : Permission.values())
            ret.add(p.name());
        return ret;
    }

    public static Permission parse(int code) {
        switch (code) {
            case 0 -> {
                return ROBERTIFY_ADMIN;
            }
            case 1 -> {
                return ROBERTIFY_DJ;
            }
            default -> throw new IllegalArgumentException("Invalid code!");
        }
    }
}
