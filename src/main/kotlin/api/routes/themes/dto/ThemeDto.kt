package api.routes.themes.dto

import kotlinx.serialization.Serializable

@Serializable
data class ThemeDto(
    val server_id: String,
    val theme: String
)
