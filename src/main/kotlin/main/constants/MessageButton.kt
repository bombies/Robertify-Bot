package main.constants

enum class MessageButton(private val str: String) {
    PAGE_ID("page"),
    FRONT("${PAGE_ID}front"),
    PREVIOUS("${PAGE_ID}previous"),
    NEXT("${PAGE_ID}next"),
    END("${PAGE_ID}end");

    override fun toString(): String =
        str
}