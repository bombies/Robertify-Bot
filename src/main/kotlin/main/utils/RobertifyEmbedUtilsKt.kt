package main.utils

import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.LocaleManagerKt
import main.utils.locale.LocaleMessageKt
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel

class RobertifyEmbedUtilsKt {

    companion object {
        private val guildEmbedSuppliers = emptyMap<Long, () -> EmbedBuilder>().toMutableMap()

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
            val localeManager = LocaleManagerKt.getLocaleManager(guild)
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
            val localeManager = LocaleManagerKt.getLocaleManager(guild)
            return getDefaultEmbed(guild)
                .setDescription(localeManager.getMessage(message, *placeholders))
        }

        fun embedMessageWithTitle(guild: Guild?, title: String, message: String): EmbedBuilder {
            return getDefaultEmbed(guild).setTitle(title).setDescription(message)
        }

        fun embedMessageWithTitle(guild: Guild?, title: LocaleMessageKt, message: LocaleMessageKt): EmbedBuilder {
            val localeManager = LocaleManagerKt.getLocaleManager(guild)
            return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title))
                .setDescription(localeManager.getMessage(message))
        }

        fun embedMessageWithTitle(guild: Guild?, title: String, message: LocaleMessageKt): EmbedBuilder {
            val localeManager = LocaleManagerKt.getLocaleManager(guild)
            return getDefaultEmbed(guild).setTitle(title)
                .setDescription(localeManager.getMessage(message))
        }

        fun embedMessageWithTitle(guild: Guild?, title: LocaleMessageKt, message: String): EmbedBuilder {
            val localeManager = LocaleManagerKt.getLocaleManager(guild)
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
            val localeManager = LocaleManagerKt.getLocaleManager(guild)
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
            val localeManager = LocaleManagerKt.getLocaleManager(guild)
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
            val localeManager = LocaleManagerKt.getLocaleManager(guild)
            return getDefaultEmbed(guild).setTitle(localeManager.getMessage(title, *placeholders))
                .setDescription(message)
        }

        fun getEphemeralState(channel: GuildMessageChannel, default: Boolean = false): Boolean {
            val dedicatedChannelConfig = RequestChannelConfig(channel.guild)
            return if (!dedicatedChannelConfig.isChannelSet) default else dedicatedChannelConfig.channelID == channel.idLong
        }
    }


}