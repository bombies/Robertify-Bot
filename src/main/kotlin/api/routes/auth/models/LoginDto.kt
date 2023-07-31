package api.routes.auth.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginDto(
    val username: String,
    val password: String
)
