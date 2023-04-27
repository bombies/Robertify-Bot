package main.commands.slashcommands.audio.filters.internal

import lavalink.client.io.filters.Filters
import main.audiohandlers.RobertifyAudioManagerKt
import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.isNotNull
import main.utils.RobertifyEmbedUtilsKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt.Companion.audioChannelChecks
import main.utils.json.logs.LogTypeKt
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

internal inline fun handleGenericFilterToggle(
    event: SlashCommandInteractionEvent,
    filterName: String,
    filterPredicate: Filters.() -> Boolean,
    filterOn: Filters.() -> Unit,
    filterOff: Filters.() -> Unit
): MessageEmbed =
    handleGenericFilterToggle(
        memberVoiceState = event.member!!.voiceState!!,
        selfVoiceState = event.guild!!.selfMember.voiceState!!,
        filterName,
        filterPredicate,
        filterOn,
        filterOff
    )

internal inline fun handleGenericFilterToggle(
    memberVoiceState: GuildVoiceState,
    selfVoiceState: GuildVoiceState,
    filterName: String,
    filterPredicate: Filters.() -> Boolean,
    filterOn: Filters.() -> Unit,
    filterOff: Filters.() -> Unit
): MessageEmbed {
    val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, songMustBePlaying = true)
    if (acChecks.isNotNull()) return acChecks!!

    val guild = selfVoiceState.guild
    val player = RobertifyAudioManagerKt.getMusicManager(guild).player
    val filters = player.filters
    val logUtils = LogUtilsKt(guild)
    val localeManager = LocaleManagerKt.getLocaleManager(guild)

    return if (filterPredicate(filters)) {
        filterOff(filters)
        logUtils.sendLog(
            LogTypeKt.FILTER_TOGGLE,
            "${memberVoiceState.member.asMention} ${
                localeManager.getMessage(
                    RobertifyLocaleMessageKt.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE,
                    Pair("{filter}", filterName),
                    GeneralUtilsKt.Pair("{status}", RobertifyLocaleMessageKt.GeneralMessages.OFF_STATUS, localeManager)
                )
            }"
        )

        RobertifyEmbedUtilsKt.embedMessage(
            guild,
            RobertifyLocaleMessageKt.FilterMessages.FILTER_TOGGLE_MESSAGE,
            Pair("{filter}", filterName),
            GeneralUtilsKt.Pair("{status}", RobertifyLocaleMessageKt.GeneralMessages.OFF_STATUS, localeManager)
        ).build()
    } else {
        filterOn(filters)
        logUtils.sendLog(
            LogTypeKt.FILTER_TOGGLE,
            "${memberVoiceState.member.asMention} ${
                localeManager.getMessage(
                    RobertifyLocaleMessageKt.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE,
                    Pair("{filter}", filterName),
                    GeneralUtilsKt.Pair("{status}", RobertifyLocaleMessageKt.GeneralMessages.ON_STATUS, localeManager)
                )
            }"
        )

        RobertifyEmbedUtilsKt.embedMessage(
            guild,
            RobertifyLocaleMessageKt.FilterMessages.FILTER_TOGGLE_MESSAGE,
            Pair("{filter}", filterName),
            GeneralUtilsKt.Pair("{status}", RobertifyLocaleMessageKt.GeneralMessages.ON_STATUS, localeManager)
        ).build()
    }
}