package main.utils

import kotlinx.coroutines.runBlocking
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.LocaleManager
import main.utils.locale.LocaleMessage
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

class RobertifyEmbedUtils private constructor(private val guild: Guild? = null) {

    companion object {
        private val guildEmbedUtils = emptyMap<Long, RobertifyEmbedUtils>().toMutableMap()
        private val guildEmbedSuppliers = emptyMap<Long, () -> EmbedBuilder>().toMutableMap()

        fun getGuildUtils(guild: Guild?): RobertifyEmbedUtils {
            if (guild == null)
                return RobertifyEmbedUtils()
            return guildEmbedUtils.computeIfAbsent(guild.idLong) { RobertifyEmbedUtils(guild)}
        }

        fun getDefaultEmbed(): EmbedBuilder {
            return when (val embedSupplier = guildEmbedSuppliers[0L]) {
                null -> {
                    GeneralUtils.setDefaultEmbed()
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
                    GeneralUtils.setDefaultEmbed(guild)
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
                    GeneralUtils.setDefaultEmbed(guild)
                    guildEmbedSuppliers[guild.idLong]!!.invoke()
                }

                else -> supplier.invoke()
            }
        }

        fun embedMessage(guild: Guild?, message: String): EmbedBuilder {
            val builder = if (guild == null) getDefaultEmbed() else getDefaultEmbed(guild)
            return builder.setDescription(message)
        }

        fun embedMessage(guild: Guild?, message: LocaleMessage): EmbedBuilder {
            if (guild == null) return embedMessage(message)
            val localeManager = LocaleManager[guild]
            return getDefaultEmbed(guild).setDescription(localeManager.getMessage(message))
        }

        fun embedMessage(message: LocaleMessage): EmbedBuilder {
            val localeManager = LocaleManager.globalManager()
            return getDefaultEmbed().setDescription(localeManager.getMessage(message))
        }

        fun embedMessage(message: String): EmbedBuilder {
            return getDefaultEmbed().setDescription(message)
        }

        fun embedMessage(
            guild: Guild?,
            message: LocaleMessage,
            vararg placeholders: Pair<String, String>
        ): EmbedBuilder {
            val localeManager = LocaleManager[guild]
            return getDefaultEmbed(guild)
                .setDescription(localeManager.getMessage(message, *placeholders))
        }

        fun embedMessageWithTitle(guild: Guild?, title: String, message: String): EmbedBuilder {
            return getDefaultEmbed(guild).setTitle(title).setDescription(message)
        }

        fun embedMessageWithTitle(guild: Guild?, title: LocaleMessage, message: LocaleMessage): EmbedBuilder {
            val localeManager = LocaleManager[guild]
            return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title))
                .setDescription(localeManager.getMessage(message))
        }

        fun embedMessageWithTitle(guild: Guild?, title: String, message: LocaleMessage): EmbedBuilder {
            val localeManager = LocaleManager[guild]
            return getDefaultEmbed(guild).setTitle(title)
                .setDescription(localeManager.getMessage(message))
        }

        fun embedMessageWithTitle(guild: Guild?, title: LocaleMessage, message: String): EmbedBuilder {
            val localeManager = LocaleManager[guild]
            return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title))
                .setDescription(message)
        }

        @SafeVarargs
        fun embedMessageWithTitle(
            guild: Guild?,
            title: LocaleMessage,
            message: LocaleMessage,
            vararg placeholders: Pair<String, String>
        ): EmbedBuilder {
            val localeManager = LocaleManager[guild]
            return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title))
                .setDescription(localeManager.getMessage(message, *placeholders))
        }

        @SafeVarargs
        fun embedMessageWithTitle(
            guild: Guild?,
            title: String,
            message: LocaleMessage,
            vararg placeholders: Pair<String, String>
        ): EmbedBuilder {
            val localeManager = LocaleManager[guild]
            return getDefaultEmbed(guild).setTitle(title)
                .setDescription(localeManager.getMessage(message, *placeholders))
        }

        @SafeVarargs
        fun embedMessageWithTitle(
            guild: Guild?,
            title: LocaleMessage,
            message: String,
            vararg placeholders: Pair<String, String>
        ): EmbedBuilder {
            val localeManager = LocaleManager[guild]
            return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title, *placeholders))
                .setDescription(message)
        }

        fun getEphemeralState(channel: GuildMessageChannel, default: Boolean = false): Boolean {
            val dedicatedChannelConfig = RequestChannelConfig(channel.guild)
            return if (!dedicatedChannelConfig.isChannelSet()) default else dedicatedChannelConfig.getChannelId() == channel.idLong
        }

        inline fun InteractionHook.sendEmbed(guild: Guild? = null, supplier: RobertifyEmbedUtils.() -> MessageEmbed): WebhookMessageCreateAction<Message> {
            val embedUtils = getGuildUtils(guild)
            return sendMessageEmbeds(supplier(embedUtils))
        }

        fun InteractionHook.sendEmbed(guild: Guild? = null, message: LocaleMessage, vararg placeholders: Pair<String, String>): WebhookMessageCreateAction<Message> {
            val embedUtils = getGuildUtils(guild)
            return sendMessageEmbeds(embedUtils.embed(message, *placeholders))
        }

        fun InteractionHook.sendEmbed(guild: Guild? = null, message: String): WebhookMessageCreateAction<Message> {
            val embedUtils = getGuildUtils(guild)
            return sendMessageEmbeds(embedUtils.embed(message))
        }

        inline fun InteractionHook.editEmbed(guild: Guild? = null, supplier: RobertifyEmbedUtils.() -> MessageEmbed): WebhookMessageEditAction<Message> {
            val embedUtils = getGuildUtils(guild)
            return editOriginalEmbeds(supplier(embedUtils))
        }

        fun InteractionHook.editEmbed(guild: Guild? = null, message: LocaleMessage, vararg placeholders: Pair<String, String>): WebhookMessageEditAction<Message> {
            val embedUtils = getGuildUtils(guild)
            return editOriginalEmbeds(embedUtils.embed(message, *placeholders))
        }

        inline fun MessageChannel.sendEmbed(guild: Guild? = null, supplier: RobertifyEmbedUtils.() -> MessageEmbed): MessageCreateAction {
            val embedUtils = getGuildUtils(guild)
            return sendMessageEmbeds(supplier(embedUtils))
        }

        fun MessageChannel.sendEmbed(guild: Guild? = null, message: LocaleMessage, vararg placeholders: Pair<String, String>): MessageCreateAction {
            val embedUtils = getGuildUtils(guild)
            return sendMessageEmbeds(embedUtils.embed(message, *placeholders))
        }

        inline fun GuildMessageChannel.sendEmbed(supplier: RobertifyEmbedUtils.() -> MessageEmbed): MessageCreateAction {
            return this.sendEmbed(guild, supplier)
        }

        fun GuildMessageChannel.sendEmbed(message: LocaleMessage, vararg placeholders: Pair<String, String>): MessageCreateAction {
            return this.sendEmbed(guild, message, *placeholders)
        }

        inline fun IReplyCallback.replyEmbed(supplier: RobertifyEmbedUtils.() -> MessageEmbed): ReplyCallbackAction {
            val embedUtils = getGuildUtils(guild)
            return replyEmbeds(supplier(embedUtils))
        }

        inline fun IReplyCallback.replyEmbeds(supplier: RobertifyEmbedUtils.() -> Collection<MessageEmbed>): ReplyCallbackAction {
            val embedUtils = getGuildUtils(guild)
            return replyEmbeds(supplier(embedUtils))
        }

        fun IReplyCallback.replyEmbed(message: LocaleMessage, vararg placeholders: Pair<String, String>): ReplyCallbackAction {
            val embedUtils = getGuildUtils(guild)
            return replyEmbeds(embedUtils.embed(message, *placeholders))
        }

        fun IReplyCallback.replyEmbed(message: String): ReplyCallbackAction {
            val embedUtils = getGuildUtils(guild)
            return replyEmbeds(embedUtils.embed(message))
        }

        inline fun Message.editEmbed(supplier: RobertifyEmbedUtils.() -> MessageEmbed): MessageEditAction {
            val embedUtils = getGuildUtils(guild)
            return editMessageEmbeds(supplier(embedUtils))
        }

        fun EmbedBuilder.addField(guild: Guild? = null, name: LocaleMessage, value: LocaleMessage, inline: Boolean = false): EmbedBuilder {
            val localeManager = LocaleManager[guild]
            return addField(localeManager.getMessage(name), localeManager.getMessage(value), inline)
        }

        fun EmbedBuilder.addField(guild: Guild? = null, name: LocaleMessage, value: String, inline: Boolean = false): EmbedBuilder {
            val localeManager = LocaleManager[guild]
            return addField(localeManager.getMessage(name), value, inline)
        }
    }

    fun embed(description: LocaleMessage, title: LocaleMessage, vararg placeholders: Pair<String, String>): MessageEmbed =
        embedMessageWithTitle(guild, title, description, *placeholders).build()

    fun embed(title: LocaleMessage, description: String, vararg placeholders: Pair<String, String>): MessageEmbed =
        embedMessageWithTitle(guild, title, description, *placeholders).build()

    fun embed(description: LocaleMessage, vararg placeholders: Pair<String, String>): MessageEmbed =
        embedMessage(guild, description, *placeholders).build()

    fun embedBuilder(description: LocaleMessage, vararg placeholders: Pair<String, String>): EmbedBuilder =
        embedMessage(guild, description, *placeholders)

    fun embed(description: String): MessageEmbed =
        embedMessage(guild, description).build()
}