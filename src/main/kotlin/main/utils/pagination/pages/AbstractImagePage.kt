package main.utils.pagination.pages

import java.io.InputStream

abstract class AbstractImagePage : MessagePage {
    abstract suspend fun generateImage(): InputStream?
}