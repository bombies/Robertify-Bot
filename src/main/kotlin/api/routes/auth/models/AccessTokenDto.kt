package api.routes.auth.models

import kotlinx.serialization.Serializable

@Serializable
data class AccessTokenDto(val access_token: String)
