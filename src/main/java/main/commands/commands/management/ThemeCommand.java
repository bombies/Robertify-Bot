package main.commands.commands.management;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Permission;
import main.constants.RobertifyTheme;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.json.toggles.TogglesConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class ThemeCommand extends InteractiveCommand implements ICommand {
    private final String menuName = "menu:themes";

    public InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(Command.of(
                        getName(),
                        "Set the colour of the bot!",
                        e -> GeneralUtils.hasPerms(e.getGuild(), e.getUser(), Permission.ROBERTIFY_THEME)
                ))
                .addSelectionDialogue(SelectionDialogue.of(
                        menuName,
                        "Select a theme",
                        Pair.of(1,1),
                        List.of(
                                Triple.of("Green", "themes:green", RobertifyTheme.GREEN.getEmoji()),
                                Triple.of("Gold", "themes:gold", RobertifyTheme.GOLD.getEmoji()),
                                Triple.of("Red", "themes:red", RobertifyTheme.RED.getEmoji()),
                                Triple.of("Pink", "themes:pink", RobertifyTheme.PINK.getEmoji()),
                                Triple.of("Purple", "themes:purple", RobertifyTheme.PURPLE.getEmoji()),
                                Triple.of("Blue", "themes:blue", RobertifyTheme.BLUE.getEmoji()),
                                Triple.of("Light Blue", "themes:lightblue", RobertifyTheme.LIGHT_BLUE.getEmoji()),
                                Triple.of("Orange", "themes:orange", RobertifyTheme.ORANGE.getEmoji()),
                                Triple.of("Yellow", "themes:yellow", RobertifyTheme.YELLOW.getEmoji())
                        ),
                        menuPredicate
                ))
                .build();
    }

    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getAuthor(), Permission.ROBERTIFY_THEME)) return;

        final var guild = ctx.getGuild();

        ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(
                guild,
                "Themes",
                "Select an option below to set the current theme!"
        ).build())
                .setActionRow(getInteractionCommand().getSelectionMenu(menuName))
                .queue();
    }

    @Override @SneakyThrows
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        final var guild = event.getGuild();

        event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(
                guild,
                "Themes",
                "Select an option below to set the current theme!"
        ).build())
                .addActionRow(getInteractionCommand().getSelectionMenu(menuName))
                .setEphemeral(false)
                .queue();
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (!event.getComponentId().equals(menuName)) return;

        final var guild = event.getGuild();

        if (!getSelectionDialogue(menuName).checkPermission(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You can't interact with this menu!").build())
                    .setEphemeral(true).queue();
            return;
        }

        final var optionSelected = event.getSelectedOptions();
        final var config = new ThemesConfig();
        String msg = null;

        switch (optionSelected.get(0).getValue()) {
            case "themes:green" -> {
                config.setTheme(guild.getIdLong(), RobertifyTheme.GREEN);
                msg = "The theme has been set to **GREEN**";
            }
            case "themes:gold" -> {
                config.setTheme(guild.getIdLong(), RobertifyTheme.GOLD);
                msg = "The theme has been set to **GOLD**";
            }
            case "themes:pink" -> {
                config.setTheme(guild.getIdLong(), RobertifyTheme.PINK);
                msg = "The theme has been set to **PINK**";
            }
            case "themes:purple" -> {
                config.setTheme(guild.getIdLong(), RobertifyTheme.PURPLE);
                msg = "The theme has been set to **PURPLE**";
            }
            case "themes:blue" -> {
                config.setTheme(guild.getIdLong(), RobertifyTheme.BLUE);
                msg = "The theme has been set to **BLUE**";
            }
            case "themes:lightblue" -> {
                config.setTheme(guild.getIdLong(), RobertifyTheme.LIGHT_BLUE);
                msg = "The theme has been set to **LIGHT BLUE**";
            }
            case "themes:orange" -> {
                config.setTheme(guild.getIdLong(), RobertifyTheme.ORANGE);
                msg = "The theme has been set to **ORANGE**";
            }
            case "themes:red" -> {
                config.setTheme(guild.getIdLong(), RobertifyTheme.RED);
                msg = "The theme has been set to **RED**";
            }
            case "themes:yellow" -> {
                config.setTheme(guild.getIdLong(), RobertifyTheme.YELLOW);
                msg = "The theme has been set to **YELLOW**";
            }
        }

        final var newTheme = config.getTheme(guild.getIdLong());
        GeneralUtils.setDefaultEmbed(guild);

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, msg)
                        .setImage(newTheme.getTransparent())
                .build()).queue();
    }

    @Override
    public String getName() {
        return "themes";
    }

    @Override
    public List<String> getAliases() {
        return List.of("theme");
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n\n" +
                "Tired of seeing our boring old green theme? Well, using this command you can have " +
                "**8** other colours to choose from! It's as easy as selecting the colour you want from the selection menu provided.";
    }

    @Override
    public void initCommand() {
        setInteractionCommand(getCommand());
        upsertCommand();
    }

    @Override
    public void initCommand(Guild g) {
        setInteractionCommand(getCommand());
        upsertCommand(g);
    }

    public void initCommandWithoutUpsertion() {
        super.initCommandWithoutUpsertion(getCommand());
    }
}
