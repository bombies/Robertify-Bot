package main.main;

import lombok.SneakyThrows;
import main.commands.CommandManager;
import main.constants.ENV;
import main.utils.database.BotUtils;
import main.utils.database.ServerUtils;
import main.utils.json.JSONConfig;
import main.utils.json.permissions.PermissionsConfig;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

public class Listener extends ListenerAdapter {
    private final CommandManager manager;
    public static final Logger LOGGER = LoggerFactory.getLogger(Listener.class);

    public Listener() {
        manager = new CommandManager();
    }

    @SneakyThrows
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        JSONConfig.initDirectory();
        PermissionsConfig permConfig = new PermissionsConfig();

        permConfig.initConfig();

        for (Guild g : new BotUtils().getGuilds()) {
            permConfig.initGuild(g.getId());
            LOGGER.info("Guild: {}", g.getName());
        }

        ServerUtils.initPrefixMap();

        Robertify.api.getPresence().setPresence(Activity.listening("-help"), true);
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        User user = event.getAuthor();
        String prefix = ServerUtils.getPrefix(event.getGuild().getIdLong());
        String raw = event.getMessage().getContentRaw();

        // Making sure the user isn't a bot or webhook command
        if (user.isBot() || event.isWebhookMessage()) {
            return;
        }

        if (raw.startsWith(prefix) && raw.length() > prefix.length()) {
            try {
                manager.handle(event);
            } catch (ScriptException e) {
                e.printStackTrace();
            }
        }
    }

    @SneakyThrows
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Guild guild = event.getGuild();

        BotUtils botUtils = new BotUtils();
        PermissionsConfig permissionsConfig = new PermissionsConfig();

        botUtils.addGuild(guild.getIdLong()).closeConnection();
        permissionsConfig.initGuild(guild.getId());

        LOGGER.info("Joined {}", guild.getName());

        ServerUtils.initPrefixMap();
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        Guild guild = event.getGuild();

        BotUtils botUtils = new BotUtils();
        botUtils.removeGuild(guild.getIdLong()).closeConnection();

        LOGGER.info("Left {}", guild.getName());


    }
}
