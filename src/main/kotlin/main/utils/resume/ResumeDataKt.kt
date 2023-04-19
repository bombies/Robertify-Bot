package main.utils.resume

import com.fasterxml.jackson.module.kotlin.readValue
import main.utils.internal.jackson.DefaultObjectMapper

data class ResumeDataKt(val channel_id: String, val tracks: List<ResumableTrackKt>) {

    companion object {
        private val mapper = DefaultObjectMapper()

        fun fromJSON(json: String): ResumeData =
            mapper.readValue(json)
    }

    override fun toString(): String =
        mapper.writeValueAsString(this)
}
