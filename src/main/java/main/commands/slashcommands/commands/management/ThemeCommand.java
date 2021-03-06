package main.commands.slashcommands.commands.management;

import lombok.SneakyThrows;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Permission;
import main.constants.RobertifyTheme;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.component.interactions.selectionmenu.SelectMenuOption;
import main.utils.component.interactions.selectionmenu.SelectionMenuBuilder;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.votes.VoteManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class ThemeCommand extends AbstractSlashCommand implements ICommand {
    final String menuName = "menu:themes";

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("themes")
                        .setDescription("Set the colour of the bot!")
                        .setPermissionCheck(e -> GeneralUtils.hasPerms(e.getGuild(), e.getMember(), Permission.ROBERTIFY_THEME))
                        .setPremium()
                        .build()
        );
    }

    private SelectionMenuBuilder getSelectionMenuBuilder(Guild guild, long userID) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        return new SelectionMenuBuilder()
                .setName(menuName)
                .setPlaceHolder(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_SELECT_MENU_PLACEHOLDER))
                .setRange(1, 1)
                .addOptions(
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_GREEN), "themes:green", RobertifyTheme.GREEN.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_MINT), "themes:mint", RobertifyTheme.MINT.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_GOLD), "themes:gold", RobertifyTheme.GOLD.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_RED), "themes:red", RobertifyTheme.RED.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_PASTEL_RED), "themes:pastel_red", RobertifyTheme.PASTEL_RED.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_PINK), "themes:pink", RobertifyTheme.PINK.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_PURPLE), "themes:purple", RobertifyTheme.PURPLE.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_PASTEL_PURPLE), "themes:pastel_purple", RobertifyTheme.PASTEL_PURPLE.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_BLUE), "themes:blue", RobertifyTheme.BLUE.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_LIGHT_BLUE), "themes:lightblue", RobertifyTheme.LIGHT_BLUE.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_BABY_BLUE), "themes:baby_blue", RobertifyTheme.BABY_BLUE.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_ORANGE), "themes:orange", RobertifyTheme.ORANGE.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_YELLOW), "themes:yellow", RobertifyTheme.YELLOW.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_PASTEL_YELLOW), "themes:pastel_yellow", RobertifyTheme.PASTEL_YELLOW.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_DARK), "themes:dark", RobertifyTheme.DARK.getEmoji()),
                        SelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_LIGHT), "themes:light", RobertifyTheme.LIGHT.getEmoji())
                )
                .limitToUser(userID);
    }

    private SelectionMenu getSelectionMenu(Guild guild, long userID) {
        return getSelectionMenuBuilder(guild, userID).build();
    }

    @Override
    public String getHelp() {
        return "Tired of seeing our boring old green theme? Well, using this command you can have " +
                "**10** other colours to choose from! It's as easy as selecting the colour you want from the selection menu provided.";
    }

    @Override @SneakyThrows
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_THEME)) {
            ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(),
                            RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS,
                            Pair.of("{permissions}", Permission.ROBERTIFY_THEME.name())
                    ).build())
                    .queue();
            return;
        }

        final var guild = ctx.getGuild();
        final var localeManager  = LocaleManager.getLocaleManager(guild);

        ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild,
                        localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_EMBED_TITLE),
                        localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_EMBED_DESC)
        ).build())
                .setActionRow(getSelectionMenu(guild, ctx.getAuthor().getIdLong()))
                .queue();
    }

    @Override @SneakyThrows
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checksWithPremium(event)) return;

        final var guild = event.getGuild();

        if (!predicateCheck(event)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild,
                            RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS,
                            Pair.of("{permissions}", Permission.ROBERTIFY_THEME.name())
                    ).build())
                    .setEphemeral(true).queue();
            return;
        }

        final var localeManager = LocaleManager.getLocaleManager(guild);

        event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild,
                        localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_EMBED_TITLE),
                        localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_EMBED_DESC)
        ).build())
                .addActionRow(getSelectionMenu(guild, event.getUser().getIdLong()))
                .setEphemeral(false)
                .queue();
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (!event.getComponentId().startsWith(menuName)) return;

        final Guild guild = event.getGuild();
        final var localeManager = LocaleManager.getLocaleManager(guild);
        if (isPremiumCommand()) {
            if (!new VoteManager().userVoted(event.getUser().getId(), VoteManager.Website.TOP_GG)) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild,
                                localeManager.getMessage(RobertifyLocaleMessage.PremiumMessages.LOCKED_COMMAND_EMBED_TITLE),
                                localeManager.getMessage(RobertifyLocaleMessage.PremiumMessages.LOCKED_COMMAND_EMBED_DESC)
                        ).build())
                        .addActionRow(
                                Button.of(ButtonStyle.LINK, "https://top.gg/bot/893558050504466482/vote", "Top.gg"),
                                Button.of(ButtonStyle.LINK, "https://discordbotlist.com/bots/robertify/upvote", "Discord Bot List")
                        )
                        .setEphemeral(true)
                        .queue();
                return;
            }
        }

        if (!event.getComponentId().split(":")[2].equals(event.getUser().getId())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NO_MENU_PERMS).build())
                    .setEphemeral(true).queue();
            return;
        }

        final var optionSelected = event.getSelectedOptions();
        final RobertifyTheme theme = RobertifyTheme.parse(optionSelected.get(0).getValue().split(":")[1].toLowerCase());
        new ThemesConfig(guild).setTheme(theme);
        String msg = localeManager.getMessage(RobertifyLocaleMessage.ThemeMessages.THEME_SET, Pair.of("{theme}", theme.name().replaceAll("_", " ")));

        GeneralUtils.setDefaultEmbed(guild);

        if (new DedicatedChannelConfig(guild).isChannelSet())
            new DedicatedChannelConfig(guild).updateMessage();

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
                "**10** other colours to choose from! It's as easy as selecting the colour you want from the selection menu provided.";
    }
}
