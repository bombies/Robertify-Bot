package main.utils.json.themes

enum class ThemesConfigField(private val str: String) {
    THEME("theme");

    override fun toString(): String = str
}