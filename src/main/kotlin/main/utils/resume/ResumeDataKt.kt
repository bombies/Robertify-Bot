package main.utils.resume

import main.utils.internal.jackson.DefaultObjectMapper

data class ResumeDataKt(val channel_id: String, val tracks: List<Any>) {
    private val mapper = DefaultObjectMapper()

}
