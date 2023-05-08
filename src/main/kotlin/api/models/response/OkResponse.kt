package api.models.response

import io.ktor.http.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement

@Serializable
@SerialName("ok")
data class OkResponse(
    override val message: String = "Success",
    override val status: Int,
    override val data: JsonElement?
) : GenericResponse {

    constructor(message: String, status: HttpStatusCode = HttpStatusCode.OK, data: JsonElement? = null) : this(message, status.value, data)

}
