package api.models.response

import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class OkResponse(
    val message: String = "Success",
    val status: Int
) {

    constructor(message: String, status: HttpStatusCode = HttpStatusCode.OK) : this(message, status.value)

}
