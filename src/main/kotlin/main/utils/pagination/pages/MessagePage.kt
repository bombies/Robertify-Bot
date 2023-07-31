package main.utils.pagination.pages

import net.dv8tion.jda.api.entities.MessageEmbed

interface MessagePage {
    val embed: MessageEmbed?
}