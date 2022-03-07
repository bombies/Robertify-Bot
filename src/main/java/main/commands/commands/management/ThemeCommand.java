package main.commands.commands.management;

import lombok.SneakyThrows;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Permission;
import main.constants.RobertifyTheme;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.legacy.InteractiveCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.votes.VoteManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
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
                        e -> GeneralUtils.hasPerms(e.getGuild(), e.getMember(), Permission.ROBERTIFY_THEME)
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
                                Triple.of("Yellow", "themes:yellow", RobertifyTheme.YELLOW.getEmoji()),
                                Triple.of("Dark", "themes:dark", RobertifyTheme.DARK.getEmoji()),
                                Triple.of("Light", "themes:light", RobertifyTheme.LIGHT.getEmoji())
                        ),
                        menuPredicate
                ))
                .build();
    }

    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_THEME)) {
            ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), "You do not have enough permissions to execute this command" +
                            "\n\nYou must have `"+Permission.ROBERTIFY_THEME.name()+"`!").build())
                    .queue();
            return;
        }

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

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You do not have enough permissions to execute this command" +
                    "\n\nYou must have `"+Permission.ROBERTIFY_THEME.name()+"`!").build())
                    .setEphemeral(true).queue();
            return;
        }

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

        if (isPremiumCommand()) {
            if (!new VoteManager().userVoted(event.getUser().getId(), VoteManager.Website.TOP_GG)) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(event.getGuild(),
                                "🔒 Locked Command", """
                                                    Woah there! You must vote before interacting with this command.
                                                    Click on each of the buttons below to vote!

                                                    *Note: Only the first two votes sites are required, the last two are optional!*""").build())
                        .addActionRow(
                                Button.of(ButtonStyle.LINK, "https://top.gg/bot/893558050504466482/vote", "Top.gg"),
                                Button.of(ButtonStyle.LINK, "https://discordbotlist.com/bots/robertify/upvote", "Discord Bot List"),
                                Button.of(ButtonStyle.LINK, "https://discords.com/bots/bot/893558050504466482/vote", "Discords.com"),
                                Button.of(ButtonStyle.LINK, "https://discord.boats/bot/893558050504466482/vote", "Discord.boats")
                        )
                        .setEphemeral(true)
                        .queue();
                return;
            }
        }

        final var guild = event.getGuild();

        if (!getSelectionDialogue(menuName).checkPermission(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You can't interact with this menu!").build())
                    .setEphemeral(true).queue();
            return;
        }

        final var optionSelected = event.getSelectedOptions();
        final RobertifyTheme theme = RobertifyTheme.parse(optionSelected.get(0).getValue().split(":")[1].toLowerCase());
        new ThemesConfig().setTheme(guild.getIdLong(), theme);
        String msg = "The theme has been set to **" + theme.name().toUpperCase() + "**";

        GeneralUtils.setDefaultEmbed(guild);

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, msg)
                        .setImage(theme.getTransparent())
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

    @Override
    public boolean isPremiumCommand() {
        return true;
    }

    public void initCommandWithoutUpsertion() {
        super.initCommandWithoutUpsertion(getCommand());
    }
}
