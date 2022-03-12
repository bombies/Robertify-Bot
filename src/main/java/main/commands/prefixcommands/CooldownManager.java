package main.commands.prefixcommands;

import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;

public class CooldownManager {
    private final HashMap<User, Long> cooldowns = new HashMap<>();
    public static final long DEFAULT_COOLDOWN = 2L;

    public void setCooldown(User u, long time) {
        if (time == 0)
            cooldowns.remove(u);
        else cooldowns.put(u, time);
    }

    public long getCooldown(User u) {
        return (cooldowns.get(u) == null ? 0 : cooldowns.get(u));
    }

    private CooldownManager() {}

    public static final CooldownManager INSTANCE = new CooldownManager();
}
