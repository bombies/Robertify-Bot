package main.commands.slashcommands.commands.management;

import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.apis.robertify.models.RobertifyPremium;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class PremiumCommand extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("premium")
                        .setDescription("Check the premium status of this server!")
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

        final var api = Robertify.getRobertifyAPI();
        final var guild = event.getGuild();

        event.deferReply().queue();

        if (!api.guildIsPremium(guild.getId())) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild,
                            RobertifyLocaleMessage.PremiumMessages.NOT_PREMIUM
                    ).build())
                    .addActionRow(Button.link("https://robertify.me/premium", LocaleManager.getLocaleManager(guild).getMessage(RobertifyLocaleMessage.GeneralMessages.PREMIUM_UPGRADE_BUTTON)))
                    .queue();
            return;
        }

        final var premiumSetterInfo = api.getGuildPremiumSetter(guild.getId());

        event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild,
                        RobertifyLocaleMessage.PremiumMessages.IS_PREMIUM,
                        Pair.of("{user}", premiumSetterInfo.getUser().getId()),
                        Pair.of("{tier}", RobertifyPremium.parseTier(premiumSetterInfo.getTier())),
                        Pair.of("{premium_started}", "<t:"+ Math.round(premiumSetterInfo.getStartedAt() / 1000.0) +":F>"),
                        Pair.of("{premium_ends}", "<t:"+ Math.round(premiumSetterInfo.getEndsAt() / 1000.0) +":F>")
                ).build())
                .queue();
    }
}
