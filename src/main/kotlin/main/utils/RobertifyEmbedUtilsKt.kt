package main.utils

import dev.minn.jda.ktx.messages.send
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.LocaleMessageKt
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import net.dv8tion.jda.api.requests.restaction.MessageEditAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction

class RobertifyEmbedUtilsKt private constructor(private val guild: Guild? = null) {

    companion object {
        private val guildEmbedUtils = emptyMap<Long, RobertifyEmbedUtilsKt>().toMutableMap()
        private val guildEmbedSuppliers = emptyMap<Long, () -> EmbedBuilder>().toMutableMap()

        fun getGuildUtils(guild: Guild?): RobertifyEmbedUtilsKt {
            if (guild == null)
                return RobertifyEmbedUtilsKt()
            return guildEmbedUtils.computeIfAbsent(guild.idLong) { RobertifyEmbedUtilsKt(guild)}
        }

        fun getDefaultEmbed(): EmbedBuilder {
            return when (val embedSupplier = guildEmbedSuppliers[0L]) {
                null -> {
                    GeneralUtilsKt.setDefaultEmbed()
                    guildEmbedSuppliers[0L]!!.invoke()
                }

                else -> embedSupplier.invoke()
            }
        }

        fun getDefaultEmbed(guild: Guild?): EmbedBuilder {
            if (guild == null)
                return getDefaultEmbed()

            return when (val embedSupplier = guildEmbedSuppliers[guild.idLong]) {
                null -> {
                    GeneralUtilsKt.setDefaultEmbed(guild)
                    guildEmbedSuppliers[guild.idLong]!!.invoke()
                }

                else -> embedSupplier.invoke()
            }
        }

        fun setEmbedBuilder(guild: Guild, supplier: () -> EmbedBuilder) {
            guildEmbedSuppliers[guild.idLong] = supplier
        }

        fun setEmbedBuilder(supplier: () -> EmbedBuilder) {
            guildEmbedSuppliers[0L] = supplier
        }

        fun getEmbedBuilder(guild: Guild?): EmbedBuilder {
            if (guild == null)
                return getDefaultEmbed()

            return when (val supplier = guildEmbedSuppliers[guild.idLong]) {
                null -> {
                    GeneralUtilsKt.setDefaultEmbed(guild)
                    guildEmbedSuppliers[guild.idLong]!!.invoke()
                }

                else -> supplier.invoke()
            }
        }

        fun embedMessage(guild: Guild?, message: String): EmbedBuilder {
            val builder = if (guild == null) getDefaultEmbed() else getDefaultEmbed(guild)
            return builder.setDescription(message)
        }

        fun embedMessage(guild: Guild?, message: LocaleMessageKt): EmbedBuilder {
            if (guild == null) return embedMessage(message)
            val localeManager = LocaleManagerKt[guild]
            return getDefaultEmbed(guild).setDescription(localeManager.getMessage(message))
        }

        fun embedMessage(message: LocaleMessageKt): EmbedBuilder {
            val localeManager = LocaleManagerKt.globalManager()
            return getDefaultEmbed().setDescription(localeManager.getMessage(message))
        }

        fun embedMessage(message: String): EmbedBuilder {
            return getDefaultEmbed().setDescription(message)
        }

        fun embedMessage(
            guild: Guild?,
            message: LocaleMessageKt,
            vararg placeholders: Pair<String, String>
        ): EmbedBuilder {
            val localeManager = LocaleManagerKt[guild]
            return getDefaultEmbed(guild)
                .setDescription(localeManager.getMessage(message, *placeholders))
        }

        fun embedMessageWithTitle(guild: Guild?, title: String, message: String): EmbedBuilder {
            return getDefaultEmbed(guild).setTitle(title).setDescription(message)
        }

        fun embedMessageWithTitle(guild: Guild?, title: LocaleMessageKt, message: LocaleMessageKt): EmbedBuilder {
            val localeManager = LocaleManagerKt[guild]
            return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title))
                .setDescription(localeManager.getMessage(message))
        }

        fun embedMessageWithTitle(guild: Guild?, title: String, message: LocaleMessageKt): EmbedBuilder {
            val localeManager = LocaleManagerKt[guild]
            return getDefaultEmbed(guild).setTitle(title)
                .setDescription(localeManager.getMessage(message))
        }

        fun embedMessageWithTitle(guild: Guild?, title: LocaleMessageKt, message: String): EmbedBuilder {
            val localeManager = LocaleManagerKt[guild]
            return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title))
                .setDescription(message)
        }

        @SafeVarargs
        fun embedMessageWithTitle(
            guild: Guild?,
            title: LocaleMessageKt,
            message: LocaleMessageKt,
            vararg placeholders: Pair<String, String>
        ): EmbedBuilder {
            val localeManager = LocaleManagerKt[guild]
            return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title))
                .setDescription(localeManager.getMessage(message, *placeholders))
        }

        @SafeVarargs
        fun embedMessageWithTitle(
            guild: Guild?,
            title: String,
            message: LocaleMessageKt,
            vararg placeholders: Pair<String, String>
        ): EmbedBuilder {
            val localeManager = LocaleManagerKt[guild]
            return getDefaultEmbed(guild).setTitle(title)
                .setDescription(localeManager.getMessage(message, *placeholders))
        }

        @SafeVarargs
        fun embedMessageWithTitle(
            guild: Guild?,
            title: LocaleMessageKt,
            message: String,
            vararg placeholders: Pair<String, String>
        ): EmbedBuilder {
            val localeManager = LocaleManagerKt[guild]
            return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title, *placeholders))
                .setDescription(message)
        }

        fun getEphemeralState(channel: GuildMessageChannel, default: Boolean = false): Boolean {
            val dedicatedChannelConfig = RequestChannelConfigKt(channel.guild)
            return if (!dedicatedChannelConfig.isChannelSet()) default else dedicatedChannelConfig.channelId == channel.idLong
        }

        inline fun InteractionHook.sendEmbed(guild: Guild? = null, supplier: RobertifyEmbedUtilsKt.() -> MessageEmbed): WebhookMessageCreateAction<Message> {
            val embedUtils = getGuildUtils(guild)
            return sendMessageEmbeds(supplier(embedUtils))
        }

        fun InteractionHook.sendEmbed(guild: Guild? = null, message: LocaleMessageKt, vararg placeholders: Pair<String, String>): WebhookMessageCreateAction<Message> {
            val embedUtils = getGuildUtils(guild)
            return sendMessageEmbeds(embedUtils.embed(message, *placeholders))
        }

        fun InteractionHook.sendEmbed(guild: Guild? = null, message: String): WebhookMessageCreateAction<Message> {
            val embedUtils = getGuildUtils(guild)
            return sendMessageEmbeds(embedUtils.embed(message))
        }

        inline fun InteractionHook.editEmbed(guild: Guild? = null, supplier: RobertifyEmbedUtilsKt.() -> MessageEmbed): WebhookMessageEditAction<Message> {
            val embedUtils = getGuildUtils(guild)
            return editOriginalEmbeds(supplier(embedUtils))
        }

        fun InteractionHook.editEmbed(guild: Guild? = null, message: LocaleMessageKt, vararg placeholders: Pair<String, String>): WebhookMessageEditAction<Message> {
            val embedUtils = getGuildUtils(guild)
            return editOriginalEmbeds(embedUtils.embed(message, *placeholders))
        }

        inline fun MessageChannel.sendEmbed(guild: Guild? = null, supplier: RobertifyEmbedUtilsKt.() -> MessageEmbed): MessageCreateAction {
            val embedUtils = getGuildUtils(guild)
            return sendMessageEmbeds(supplier(embedUtils))
        }

        fun MessageChannel.sendEmbed(guild: Guild? = null, message: LocaleMessageKt, vararg placeholders: Pair<String, String>): MessageCreateAction {
            val embedUtils = getGuildUtils(guild)
            return sendMessageEmbeds(embedUtils.embed(message, *placeholders))
        }

        inline fun GuildMessageChannel.sendEmbed(supplier: RobertifyEmbedUtilsKt.() -> MessageEmbed): MessageCreateAction {
            return this.sendEmbed(guild, supplier)
        }

        fun GuildMessageChannel.sendEmbed(message: LocaleMessageKt, vararg placeholders: Pair<String, String>): MessageCreateAction {
            return this.sendEmbed(guild, message, *placeholders)
        }

        inline fun IReplyCallback.replyEmbed(supplier: RobertifyEmbedUtilsKt.() -> MessageEmbed): ReplyCallbackAction {
            val embedUtils = getGuildUtils(guild)
            return replyEmbeds(supplier(embedUtils))
        }

        inline fun IReplyCallback.replyEmbeds(supplier: RobertifyEmbedUtilsKt.() -> Collection<MessageEmbed>): ReplyCallbackAction {
            val embedUtils = getGuildUtils(guild)
            return replyEmbeds(supplier(embedUtils))
        }

        fun IReplyCallback.replyEmbed(message: LocaleMessageKt, vararg placeholders: Pair<String, String>): ReplyCallbackAction {
            val embedUtils = getGuildUtils(guild)
            return replyEmbeds(embedUtils.embed(message, *placeholders))
        }

        fun IReplyCallback.replyEmbed(message: String): ReplyCallbackAction {
            val embedUtils = getGuildUtils(guild)
            return replyEmbeds(embedUtils.embed(message))
        }

        inline fun Message.editEmbed(supplier: RobertifyEmbedUtilsKt.() -> MessageEmbed): MessageEditAction {
            val embedUtils = getGuildUtils(guild)
            return editMessageEmbeds(supplier(embedUtils))
        }

        fun EmbedBuilder.addField(guild: Guild? = null, name: LocaleMessageKt, value: LocaleMessageKt, inline: Boolean = false): EmbedBuilder {
            val localeManager = LocaleManagerKt[guild]
            return addField(localeManager.getMessage(name), localeManager.getMessage(value), inline)
        }

        fun EmbedBuilder.addField(guild: Guild? = null, name: LocaleMessageKt, value: String, inline: Boolean = false): EmbedBuilder {
            val localeManager = LocaleManagerKt[guild]
            return addField(localeManager.getMessage(name), value, inline)
        }
    }

    fun embed(description: LocaleMessageKt, title: LocaleMessageKt, vararg placeholders: Pair<String, String>): MessageEmbed =
        embedMessageWithTitle(guild, title, description, *placeholders).build()

    fun embed(title: LocaleMessageKt, description: String, vararg placeholders: Pair<String, String>): MessageEmbed =
        embedMessageWithTitle(guild, title, description, *placeholders).build()

    fun embed(description: LocaleMessageKt, vararg placeholders: Pair<String, String>): MessageEmbed =
        embedMessage(guild, description, *placeholders).build()

    fun embedBuilder(description: LocaleMessageKt, vararg placeholders: Pair<String, String>): EmbedBuilder =
        embedMessage(guild, description, *placeholders)

    fun embed(description: String): MessageEmbed =
        embedMessage(guild, description).build()
}