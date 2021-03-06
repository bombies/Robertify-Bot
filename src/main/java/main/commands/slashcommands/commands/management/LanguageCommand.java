package main.commands.slashcommands.commands.management;

import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.component.interactions.selectionmenu.SelectMenuOption;
import main.utils.component.interactions.selectionmenu.SelectionMenuBuilder;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocale;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class LanguageCommand extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("language")
                        .setDescription("Change the bot's language!")
                        .addOptions(CommandOption.of(
                                OptionType.STRING,
                                "lang",
                                "The specific language to switch to",
                                false,
                                RobertifyLocale.getAvailableLanguages().stream()
                                        .map(RobertifyLocale::name)
                                        .toList()
                        ))
                        .setAdminOnly()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        final var guild = event.getGuild();
        final var localeManager = LocaleManager.getLocaleManager(guild);
        final var lang = event.getOption("lang");
        if (lang == null) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LanguageCommandMessages.LANGUAGE_EMBED_DESC, Pair.of("{language}", localeManager.getLocale().getLocalName() + " " + localeManager.getLocale().getFlag())).build())
                    .addActionRow(SelectionMenuBuilder.of(
                            "languagemenu",
                            localeManager.getMessage(RobertifyLocaleMessage.LanguageCommandMessages.LANGUAGE_SELECT_MENU_PLACE_HOLDER),
                            Pair.of(1,1),
                            RobertifyLocale.getAvailableLanguages().stream()
                                    .map(locale -> SelectMenuOption.of(
                                            locale.getName(),
                                            "languagemenu:" + locale.name().toLowerCase(),
                                            Emoji.fromUnicode(locale.getFlag())
                                    ))
                                    .toList()
                    ).build())
                    .setEphemeral(true)
                    .queue();
        } else {
            final var newLocale = RobertifyLocale.parse(lang.getAsString());
            localeManager.setLocale(newLocale);
            new DedicatedChannelConfig(guild).updateAll();
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LanguageCommandMessages.LANGUAGE_CHANGED, Pair.of("{language}", newLocale.getLocalName() + " " + newLocale.getFlag()))
                    .build())
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        final var selectionMenu = event.getSelectionMenu();
        if (!selectionMenu.getId().startsWith("languagemenu")) return;

        final var newLocale = RobertifyLocale.parse(event.getSelectedOptions().get(0).getValue().split(":")[1]);
        final var guild = event.getGuild();

        event.deferReply().queue();

        LocaleManager.getLocaleManager(guild).setLocale(newLocale);
        new DedicatedChannelConfig(guild).updateAll();
        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LanguageCommandMessages.LANGUAGE_CHANGED, Pair.of("{language}", newLocale.getLocalName() + " " + newLocale.getFlag()))
                .build())
                .setEphemeral(true)
                .queue();
    }
}
