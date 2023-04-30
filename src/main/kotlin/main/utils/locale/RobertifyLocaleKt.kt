package main.utils.locale

enum class RobertifyLocaleKt(
    val code: String,
    val englishName: String,
    val localName: String,
    val flag: String
) {
    ENGLISH("en", "English (UK)", "English (UK)", "\uD83C\uDDEC\uD83C\uDDE7"),
    SPANISH("es", "Spanish", "Español", "\uD83C\uDDEA\uD83C\uDDF8"),
    PORTUGUESE("pt", "Portuguese", "Português", "\uD83C\uDDF5\uD83C\uDDF9"),
    RUSSIAN("ru", "Russian", "Русский", "\uD83C\uDDF7\uD83C\uDDFA"),
    DUTCH("nl", "Dutch", "Nederlands", "\uD83C\uDDF3\uD83C\uDDF1"),
    FRENCH("fr", "French", "Français", "\uD83C\uDDEB\uD83C\uDDF7"),
    GERMAN("de", "German", "Deutsch", "\uD83C\uDDE9\uD83C\uDDEA");

    companion object {

        val availableLanguages: List<RobertifyLocaleKt>
            get() = values().toList()

        fun parse(locale: String): RobertifyLocaleKt {
            return when (locale.uppercase()) {
                "ENGLISH", "EN" -> ENGLISH
                "SPANISH", "ES" -> SPANISH
                "PORTUGUESE", "PT" -> PORTUGUESE
                "RUSSIAN", "RU" -> RUSSIAN
                "DUTCH", "NL" -> DUTCH
                "GERMAN", "DE" -> GERMAN
                "FRENCH", "FR" -> FRENCH
                else -> throw IllegalArgumentException("There is no such locale with the name: $locale")
            }
        }
    }
}