package main.utils.pagination.pages

import java.io.InputStream

abstract class AbstractImagePage : MessagePage {
    abstract fun generateImage(): InputStream?
}