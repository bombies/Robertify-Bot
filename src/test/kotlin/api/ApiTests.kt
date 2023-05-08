package api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApiTests {

    @Test
    fun testRoot() = defaultTestApplication {
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Welcome to the API!", bodyAsText())
        }

    }
}