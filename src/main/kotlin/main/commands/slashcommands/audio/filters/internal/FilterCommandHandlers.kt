package main.commands.slashcommands.audio.filters.internal

import dev.arbjerg.lavalink.client.protocol.FilterBuilder
import dev.arbjerg.lavalink.protocol.v4.Filters
import main.audiohandlers.RobertifyAudioManager
import main.utils.GeneralUtils
import main.utils.GeneralUtils.isNotNull
import main.utils.RobertifyEmbedUtils
import main.utils.component.interactions.slashcommand.AbstractSlashCommand.Companion.audioChannelChecks
import main.utils.json.logs.LogType
import main.utils.json.logs.LogUtilsKt
import main.utils.locale.LocaleManager
import main.utils.locale.messages.FilterMessages
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

internal inline fun handleGenericFilterToggle(
    event: SlashCommandInteractionEvent,
    filterName: String,
    filterPredicate: Filters.() -> Boolean,
    noinline filterOn: FilterBuilder.() -> FilterBuilder,
    noinline filterOff: FilterBuilder.() -> FilterBuilder
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
    noinline filterOn: FilterBuilder.() -> FilterBuilder,
    noinline filterOff: FilterBuilder.() -> FilterBuilder
): MessageEmbed {
    val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, songMustBePlaying = true)
    if (acChecks.isNotNull()) return acChecks!!

    val guild = selfVoiceState.guild
    val player = RobertifyAudioManager[guild].player!!
    val filters = player.filters
    val logUtils = LogUtilsKt(guild)
    val localeManager = LocaleManager[guild]

    return if (filterPredicate(filters)) {
        player.setFilters(filterOff(FilterBuilder()).build()).subscribe()
        logUtils.sendLog(
            LogType.FILTER_TOGGLE,
            "${memberVoiceState.member.asMention} ${
                localeManager.getMessage(
                    FilterMessages.FILTER_TOGGLE_LOG_MESSAGE,
                    Pair("{filter}", filterName),
                    GeneralUtils.Pair("{status}", GeneralMessages.OFF_STATUS, localeManager)
                )
            }"
        )

        RobertifyEmbedUtils.embedMessage(
            guild,
            FilterMessages.FILTER_TOGGLE_MESSAGE,
            Pair("{filter}", filterName),
            GeneralUtils.Pair("{status}", GeneralMessages.OFF_STATUS, localeManager)
        ).build()
    } else {
        player.setFilters(filterOn(FilterBuilder()).build()).subscribe()
        logUtils.sendLog(
            LogType.FILTER_TOGGLE,
            "${memberVoiceState.member.asMention} ${
                localeManager.getMessage(
                    FilterMessages.FILTER_TOGGLE_LOG_MESSAGE,
                    Pair("{filter}", filterName),
                    GeneralUtils.Pair("{status}", GeneralMessages.ON_STATUS, localeManager)
                )
            }"
        )

        RobertifyEmbedUtils.embedMessage(
            guild,
            FilterMessages.FILTER_TOGGLE_MESSAGE,
            Pair("{filter}", filterName),
            GeneralUtils.Pair("{status}", GeneralMessages.ON_STATUS, localeManager)
        ).build()
    }
}