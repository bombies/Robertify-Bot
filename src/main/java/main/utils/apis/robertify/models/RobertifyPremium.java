package main.utils.apis.robertify.models;

import lombok.Getter;
import main.main.Robertify;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.List;
import java.util.Objects;

public class RobertifyPremium {
    private final long userId;
    @Getter
    private final String email;
    @Getter
    private final int type, tier;
    private final List<String> servers;
    @Getter
    private final long startedAt, endsAt;

    public RobertifyPremium(long userId, String email, int type, int tier, List<String> servers, long startedAt, long endsAt) {
        this.userId = userId;
        this.email = email;
        this.type = type;
        this.tier = tier;
        this.servers = servers;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }

    public RobertifyPremium(String userId, String email, int type, int tier, List<String> servers, String startedAt, String endsAt) {
        this.userId = Long.parseLong(userId);
        this.email = email;
        this.type = type;
        this.tier = tier;
        this.servers = servers;
        this.startedAt = Long.parseLong(startedAt);
        this.endsAt = Long.parseLong(endsAt);
    }

    public User getUser() {
        return Robertify.getShardManager().getUserById(this.userId);
    }

    public List<Guild> getGuilds() {
        return servers.stream()
                .map(id -> Robertify.getShardManager().getGuildById(id))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<String> getGuildIDs() {
        return this.servers;
    }
}
