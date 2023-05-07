package main.utils

import main.constants.RobertifyPermissionKt
import main.constants.RobertifyEmojiKt
import main.constants.RobertifyThemeKt
import main.constants.TimeFormatKt
import main.main.RobertifyKt
import main.utils.RobertifyEmbedUtilsKt.Companion.sendEmbed
import main.utils.database.mongodb.cache.BotDBCacheKt
import main.utils.json.GenericJSONFieldKt
import main.utils.json.permissions.PermissionsConfigKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.LocaleMessageKt
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.PremiumMessages
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.RestAction
import org.apache.commons.lang3.time.DurationFormatUtils
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.regex.Pattern

object GeneralUtilsKt {

    val log: Logger = LoggerFactory.getLogger(this::class.java);

    fun String?.isNum() = when (this) {
        null -> false
        else -> this.matches("-?\\d+(\\.\\d+)?".toRegex())
    }

    fun String?.isInt() = when (this) {
        null -> false
        else -> this.matches("-?\\d+".toRegex())
    }

    fun <T> Collection<T>.asString(transform: ((T) -> CharSequence)? = null): String =
        this.joinToString(separator = ", ", transform = transform)

    fun String.isDiscordId(): Boolean =
        this.matches("^[0-9]{18}$".toRegex())

    fun String?.isUrl(): Boolean {
        if (this == null)
            return false

        return try {
            URI(this)
            this.contains("://")
        } catch (e: URISyntaxException) {
            false
        }
    }

    fun String.toUrl(): URL? = if (this.isUrl())
        URL(this)
    else null

    fun URL.getDestination(): String {
        val openConnection: () -> HttpURLConnection = {
            this.openConnection() as HttpURLConnection
        }

        var con = openConnection()
        con.instanceFollowRedirects = false

        var mutableLocation = this.toString()
        while (con.responseCode / 100 == 3) {
            mutableLocation = con.getHeaderField("location")
            con = openConnection()

            if (con.responseCode / 100 == 2) {
                mutableLocation = con.url.toString()
                con = openConnection()
            }
        }

        return mutableLocation
    }

    fun String.digits(): String =
        this.replace("\\D".toRegex(), "")

    fun String.stripDigits(): String =
        this.replace("\\d".toRegex(), "")

    fun Member?.hasPermissions(vararg perms: RobertifyPermissionKt): Boolean {
        if (this == null) return false

        if (hasPermission(Permission.ADMINISTRATOR) || isOwner)
            return true

        val config = PermissionsConfigKt(guild)
        var pass = 0

        roles.forEach { role ->
            if (config.getRolesForPermission(RobertifyPermissionKt.ROBERTIFY_ADMIN).contains(role.idLong))
                return true
            perms.forEach { perm ->
                pass += if (config.getRolesForPermission(perm).contains(role.idLong)
                    || config.getUsersForPermission(perm.name).contains(idLong)
                )
                    1
                else 0
            }
        }

        return pass >= perms.size
    }

    fun hasPerms(guild: Guild, sender: Member?, vararg perms: RobertifyPermissionKt): Boolean {
        if (sender == null)
            return false

        if (sender.hasPermission(Permission.ADMINISTRATOR)
            || sender.isOwner
        )
            return true

        val roles = sender.roles
        val config = PermissionsConfigKt(guild)
        var pass = 0

        roles.forEach { role ->
            if (config.getRolesForPermission(RobertifyPermissionKt.ROBERTIFY_ADMIN).contains(role.idLong))
                return true
            perms.forEach { perm ->
                pass += if (config.getRolesForPermission(perm).contains(role.idLong)
                    || config.getUsersForPermission(perm.name).contains(sender.idLong)
                )
                    1
                else 0
            }
        }

        return pass >= perms.size
    }

    fun isDeveloper(uid: Long): Boolean {
        return BotDBCacheKt.instance.isDeveloper(uid)
    }

    fun isDeveloper(uid: String): Boolean {
        return isDeveloper(uid.toLong())
    }

    enum class ProgressBar {
        DURATION,
        FILL
    }

    fun progressBar(
        guild: Guild,
        channel: GuildMessageChannel,
        percent: Double,
        barType: ProgressBar
    ): String {
        val self = guild.selfMember
        val barBuilder = StringBuilder()
        when (barType) {
            ProgressBar.DURATION -> {
                for (i in 0..11)
                    if (i == (percent * 12).toInt())
                        barBuilder.append("\uD83D\uDD18") // 🔘
                    else barBuilder.append("▬")
                return barBuilder.toString()
            }

            ProgressBar.FILL -> {
                val selfHasExternEmojiPerms = self.hasPermission(channel, Permission.MESSAGE_EXT_EMOJI)
                val handleEmptyBars: (i: Int) -> Unit = { i ->
                    when (i) {
                        0 -> barBuilder.append(
                            if (selfHasExternEmojiPerms)
                                RobertifyEmojiKt.BAR_START_EMPTY
                            else "⬜"
                        )

                        11 -> barBuilder.append(
                            if (selfHasExternEmojiPerms)
                                RobertifyEmojiKt.BAR_START_EMPTY
                            else "⬜"
                        )

                        else -> barBuilder.append(
                            if (selfHasExternEmojiPerms)
                                RobertifyEmojiKt.BAR_MIDDLE_EMPTY
                            else "⬜"
                        )
                    }
                }

                if (percent * 12 == 0.0) {
                    for (i in 0..11)
                        handleEmptyBars(i)
                } else {
                    for (i in 0..11) {
                        when {
                            i <= (percent * 12).toInt() -> {
                                when (i) {
                                    0 -> barBuilder.append(
                                        if (selfHasExternEmojiPerms)
                                            RobertifyEmojiKt.BAR_START_FULL
                                        else "⬛"
                                    )

                                    11 -> barBuilder.append(
                                        if (selfHasExternEmojiPerms)
                                            RobertifyEmojiKt.BAR_START_FULL
                                        else "⬛"
                                    )

                                    else -> barBuilder.append(
                                        if (selfHasExternEmojiPerms)
                                            RobertifyEmojiKt.BAR_MIDDLE_FULL
                                        else "⬛"
                                    )
                                }
                            }

                            else -> handleEmptyBars(i)
                        }
                    }
                }
                return barBuilder.toString()
            }
        }
    }

    fun getTimeFromSeconds(time: Long, unit: TimeUnit): Long {
        return when (unit) {
            TimeUnit.SECONDS -> ((time % 86400) % 3600) % 60
            TimeUnit.MINUTES -> ((time % 86400) % 3600) / 60
            TimeUnit.HOURS -> (time % 86400) / 3600
            else -> throw IllegalArgumentException("The enum provided isn't a supported enum!")
        }
    }

    fun getTimeFromMillis(duration: Duration, unit: TimeUnit): Long {
        return when (unit) {
            TimeUnit.SECONDS -> duration.toSeconds() % 60
            TimeUnit.MINUTES -> duration.toMinutes() % 60
            TimeUnit.HOURS -> duration.toHours() % 24
            TimeUnit.DAYS -> duration.toDays()
            else -> throw IllegalArgumentException("The enum provided isn't a supported enum!")
        }
    }

    fun getDurationString(duration: Long): String {
        val second = getTimeFromMillis(Duration.ofMillis(duration), TimeUnit.SECONDS)
        val minute = getTimeFromMillis(Duration.ofMillis(duration), TimeUnit.MINUTES)
        val hour = getTimeFromMillis(Duration.ofMillis(duration), TimeUnit.HOURS)
        val day = getTimeFromMillis(Duration.ofMillis(duration), TimeUnit.DAYS)
        return ((if (day > 0) day.toString() + (if (day > 1) " days, " else " day, ") else "")
                + (if (hour > 0) hour.toString() + (if (hour > 1) " hours, " else " hour, ") else "")
                + (if (minute > 0) minute.toString() + (if (minute > 1) " minutes, " else " minute, ") else "")
                + second + if (second > 1) " seconds" else if (second == 0L) " seconds" else " second")
    }

    fun formatDate(date: Long, style: TimeFormatKt): String {
        return when (style) {
            TimeFormatKt.DD_MMMM_YYYY, TimeFormatKt.MM_DD_YYYY, TimeFormatKt.DD_MMMM_YYYY_ZZZZ, TimeFormatKt.DD_M_YYYY_HH_MM_SS, TimeFormatKt.E_DD_MMM_YYYY_HH_MM_SS_Z -> SimpleDateFormat(
                style.toString()
            ).format(date)

            else -> throw java.lang.IllegalArgumentException("The enum provided isn't a supported enum!")
        }
    }

    fun formatTime(duration: Long): String {
        return DurationFormatUtils.formatDuration(duration, "HH:mm:ss")
    }

    fun formatTime(duration: Long, format: String): String {
        return DurationFormatUtils.formatDuration(duration, format)
    }

    fun isValidDuration(timeUnparsed: String?): Boolean {
        val durationStrRegex = "^\\d*[sSmMhHdD]$"
        return Pattern.matches(durationStrRegex, timeUnparsed)
    }

    fun getFutureTime(timeUnparsed: String): Long {
        val timeDigits = timeUnparsed.substring(0, timeUnparsed.length - 1)
        val duration = timeUnparsed[timeUnparsed.length - 1]
        require(timeDigits.toInt() >= 0) { "The time cannot be negative!" }
        val scheduledDuration: Long = if (timeDigits.isInt()) when (duration) {
            's' -> System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(
                timeDigits.toInt().toLong()
            )

            'm' -> System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(
                timeDigits.toInt().toLong()
            )

            'h' -> System.currentTimeMillis() + TimeUnit.HOURS.toMillis(
                timeDigits.toInt().toLong()
            )

            'd' -> System.currentTimeMillis() + TimeUnit.DAYS.toMillis(
                timeDigits.toInt().toLong()
            )

            else -> throw java.lang.IllegalArgumentException("The duration specifier \"$duration\" is invalid!")
        } else throw java.lang.IllegalArgumentException("There was no valid integer provided!")
        return scheduledDuration
    }

    fun getStaticTime(timeUnparsed: String): Long {
        return getFutureTime(timeUnparsed) - System.currentTimeMillis()
    }

    fun formatDuration(timeUnparsed: String): String {
        val ret: String
        val timeDigits = timeUnparsed.substring(0, timeUnparsed.length - 1)
        val duration = timeUnparsed[timeUnparsed.length - 1]
        ret = if (timeDigits.isInt()) when (duration) {
            's' -> "$timeDigits seconds"
            'm' -> "$timeDigits minutes"
            'h' -> "$timeDigits hours"
            'd' -> "$timeDigits days"
            else -> throw java.lang.IllegalArgumentException("The duration specifier \"$duration\" is invalid!")
        } else throw java.lang.IllegalArgumentException("There was no valid integer provided!")
        return ret
    }

    fun getFileContent(path: String): String {
        var ret: String? = null
        try {
            ret = String(Files.readAllBytes(Paths.get(path)))
        } catch (e: IOException) {
            log.error("[FATAL ERROR] There was an error reading from the file!", e)
        }
        if (ret == null) throw NullPointerException()
        return ret.replace("\t\n".toRegex(), "")
    }

    fun getFileContent(path: Path): String {
        var ret: String? = null
        try {
            ret = String(Files.readAllBytes(path))
        } catch (e: IOException) {
            log.error("[FATAL ERROR] There was an error reading from the file!", e)
        }
        if (ret == null) throw NullPointerException()
        return ret.replace("\t\n".toRegex(), "")
    }

    @Throws(IOException::class)
    fun setFileContent(path: String, content: String) {
        val file = File(path)
        if (!file.exists()) file.createNewFile()
        val writer = FileWriter(path, false)
        writer.write(content)
        writer.close()
    }

    @Throws(IOException::class)
    fun setFileContent(path: Path, content: String) {
        val file = File(path.toString())
        if (!file.exists()) file.createNewFile()
        val writer = FileWriter(path.toString(), false)
        writer.write(content)
        writer.close()
    }

    @Throws(IOException::class)
    fun setFileContent(passedFile: File, content: String) {
        val file = File(passedFile.path)
        if (!file.exists()) file.createNewFile()
        val writer = FileWriter(passedFile.path, false)
        writer.write(content)
        writer.close()
    }

    fun File.setContent(content: String) {
        if (!exists()) createNewFile()
        val writer = FileWriter(path, false)
        writer.write(content)
        writer.close()
    }

    fun File.getContent(): String {
        var ret: String? = null
        try {
            ret = String(Files.readAllBytes(Path.of(path)))
        } catch (e: IOException) {
            log.error("[FATAL ERROR] There was an error reading from the file!", e)
        }
        if (ret == null) throw NullPointerException()
        return ret.replace("\t\n".toRegex(), "")
    }

    fun File.appendContent(content: String) =
        setContent(getContent() + content)

    fun appendFileContent(file: File, content: String) {
        setFileContent(file, getFileContent(file.path) + content)
    }

    fun fileExists(path: String): Boolean {
        return Files.exists(Path.of(path))
    }

    fun fileExists(file: File): Boolean {
        return file.exists()
    }

    fun directoryExists(path: String): Boolean {
        return fileExists(path)
    }

    fun createDirectory(path: String) {
        Files.createDirectory(Path.of(path))
    }

    fun createFile(path: String) {
        Files.createFile(Path.of(path))
    }

    fun setDefaultEmbed() {
        val theme = RobertifyThemeKt.GREEN
        RobertifyEmbedUtilsKt.setEmbedBuilder {
            EmbedBuilder().setColor(theme.color)
        }
    }

    fun setDefaultEmbed(guild: Guild) {
        val theme = ThemesConfigKt(guild).theme
        RobertifyEmbedUtilsKt.setEmbedBuilder(guild) {
            EmbedBuilder().setColor(theme.color)
        }
    }

    fun setCustomEmbed(guild: Guild, author: String? = null, footer: String? = null) {
        val theme = ThemesConfigKt(guild).theme
        RobertifyEmbedUtilsKt.setEmbedBuilder(guild) {
            EmbedBuilder()
                .setColor(theme.color)
                .setAuthor(author, null, if (author != null) theme.transparent else null)
                .setFooter(footer)
        }
    }

    fun parseColor(hex: String): Color = Color.decode(hex)

    fun parseNumEmoji(emoji: String): Int {
        return when (emoji) {
            "0️⃣" -> 0
            "1️⃣" -> 1
            "2️⃣" -> 2
            "3️⃣" -> 3
            "4️⃣" -> 4
            "5️⃣" -> 5
            "6️⃣" -> 6
            "7️⃣" -> 7
            "8️⃣" -> 8
            "9️⃣" -> 9
            "🔟" -> 10
            else -> throw IllegalArgumentException("Invalid argument: \"$emoji\"")
        }
    }

    fun parseNumEmoji(num: Int): String {
        return when (num) {
            0 -> "0️⃣"
            1 -> "1️⃣"
            2 -> "2️⃣"
            3 -> "3️⃣"
            4 -> "4️⃣"
            5 -> "5️⃣"
            6 -> "6️⃣"
            7 -> "7️⃣"
            8 -> "8️⃣"
            9 -> "9️⃣"
            10 -> "🔟"
            else -> throw IllegalArgumentException("Invalid argument: \"$num\"")
        }
    }

    fun toSafeString(str: String): String = Pattern.quote(str)

    enum class Mentioner {
        USER,
        ROLE,
        CHANNEL
    }

    fun listOfIDsToMentions(guild: Guild?, mentions: List<Long>, mentioner: Mentioner): String {
        val mentionTag: String = when (mentioner) {
            Mentioner.USER -> "@"
            Mentioner.ROLE -> "@&"
            Mentioner.CHANNEL -> "#"
        }

        val parsedMentions = mentions.map { id -> "<$mentionTag$id>" }
        return when {
            parsedMentions.isEmpty() -> LocaleManagerKt[guild]
                .getMessage(GeneralMessages.NOTHING_HERE)

            else -> parsedMentions.asString()
        }
    }

    fun toMention(guild: Guild? = null, id: Long, mentioner: Mentioner): String =
        listOfIDsToMentions(guild, listOf(id), mentioner)

    fun toMention(guild: Guild? = null, id: String, mentioner: Mentioner): String =
        listOfIDsToMentions(guild, listOf(id.toLong()), mentioner)

    fun Long.toMention(mentioner: Mentioner, guild: Guild? = null): String =
        listOfIDsToMentions(guild, listOf(this), mentioner)

    fun String.toMention(mentioner: Mentioner, guild: Guild? = null): String =
        listOfIDsToMentions(guild, listOf(this.toLong()), mentioner)

    fun getID(obj: JSONObject, field: String): Long = try {
        obj.getLong(field)
    } catch (e: JSONException) {
        obj.getString(field).toLong()
    }

    fun getID(obj: JSONObject, field: GenericJSONFieldKt): Long = getID(obj, field.toString())

    fun checkPremium(guild: Guild, event: GenericComponentInteractionCreateEvent): Boolean {
        // TODO: Guild config premium check

        event.replyEmbeds(
            RobertifyEmbedUtilsKt.embedMessageWithTitle(
                guild,
                PremiumMessages.LOCKED_COMMAND_EMBED_TITLE,
                PremiumMessages.LOCKED_COMMAND_EMBED_DESC
            ).build()
        )
            .setEphemeral(true)
            .queue()
        return false
    }

    fun checkPremium(guild: Guild, event: GenericCommandInteractionEvent): Boolean {
        // TODO: Guild config premium check

        event.replyEmbeds(
            RobertifyEmbedUtilsKt.embedMessageWithTitle(
                guild,
                PremiumMessages.LOCKED_COMMAND_EMBED_TITLE,
                PremiumMessages.LOCKED_COMMAND_EMBED_DESC
            ).build()
        )
            .setEphemeral(true)
            .queue()
        return false
    }

    fun checkPremium(guild: Guild, user: User, msg: Message): Boolean {
        // TODO: Guild config premium check

        msg.reply(user.asMention)
            .setEmbeds(
                RobertifyEmbedUtilsKt.embedMessageWithTitle(
                    guild,
                    PremiumMessages.LOCKED_COMMAND_EMBED_TITLE,
                    PremiumMessages.LOCKED_COMMAND_EMBED_DESC

                ).build()
            )
            .setActionRow(
                Button.link(
                    "https://robertify.me/premium",
                    LocaleManagerKt[guild]
                        .getMessage(GeneralMessages.PREMIUM_UPGRADE_BUTTON)
                )
            )
            .queue()
        return false
    }

    fun dmUser(user: User, message: LocaleMessageKt) {
        user.openPrivateChannel().queue { channel ->
            channel.sendMessageEmbeds(RobertifyEmbedUtilsKt.embedMessage(message).build())
                .queue(null) {
                    ErrorHandler()
                        .ignore(ErrorResponse.CANNOT_SEND_TO_USER)
                }
        }
    }

    fun dmUser(user: User, message: MessageEmbed) {
        user.openPrivateChannel().queue { channel ->
            channel.sendMessageEmbeds(message)
                .queue(null) {
                    ErrorHandler()
                        .ignore(ErrorResponse.CANNOT_SEND_TO_USER)
                }
        }
    }

    fun User.dm(message: MessageEmbed) {
        openPrivateChannel().queue { channel ->
            channel.sendMessageEmbeds(message)
                .queue(null) {
                    ErrorHandler()
                        .ignore(ErrorResponse.CANNOT_SEND_TO_USER)
                }
        }
    }

    fun User.dmEmbed(message: LocaleMessageKt, vararg placeholders: Pair<String, String>) {
        openPrivateChannel().queue { channel ->
            channel.sendEmbed {
                embed(message, *placeholders)
            }.queue(null) {
                ErrorHandler()
                    .ignore(ErrorResponse.CANNOT_SEND_TO_USER)
            }
        }
    }

    fun User.dm(message: String) {
        openPrivateChannel().queue { channel ->
            channel.sendMessageEmbeds(RobertifyEmbedUtilsKt.embedMessage(message).build())
                .queue(null) {
                    ErrorHandler()
                        .ignore(ErrorResponse.CANNOT_SEND_TO_USER)
                }
        }
    }

    fun dmUser(uid: Long, embed: MessageEmbed) {
        RobertifyKt.shardManager.retrieveUserById(uid)
            .queue { it.dm(embed) }
    }


    fun String?.isRightToLeft(): Boolean =
        this?.chars()?.anyMatch { it in 0x5d1..0x6ff } ?: false

    fun Any?.isNull(): Boolean = this == null

    fun Any?.isNotNull(): Boolean = this != null

    fun <A, B : LocaleMessageKt> Pair(first: A, second: B, localeManager: LocaleManagerKt): Pair<A, String> =
        Pair(first, localeManager.getMessage(second))

    fun <T> RestAction<T>.queueAfter(duration: kotlin.time.Duration) =
        queueAfter(duration.inWholeSeconds, TimeUnit.SECONDS)

    fun <T> RestAction<T>.queueAfter(duration: kotlin.time.Duration, success: Consumer<in T>) =
        queueAfter(duration.inWholeSeconds, TimeUnit.SECONDS, success)

    fun <T> List<T>.coerceAtMost(max: Int): List<T> =
        this.subList(0, this.size.coerceAtMost(max))

    fun <T> List<T>.coerceAtLeast(min: Int): List<T> =
        this.subList(0, this.size.coerceAtLeast(min))

    fun String.coerceAtMost(max: Int): String =
        this.substring(0, this.length.coerceAtMost(max))

    fun String.coerceAtLeast(min: Int): String =
        this.substring(0, this.length.coerceAtLeast(min))
}