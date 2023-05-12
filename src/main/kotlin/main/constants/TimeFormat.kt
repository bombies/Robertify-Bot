package main.constants


enum class TimeFormat(private val str: String) {
    SECONDS("seconds"),
    MINUTES("minutes"),
    HOURS("hours"),
    DAYS("days"),
    LOCALIZED_DATE("of_localized_date"),
    LOCALIZED_TIME("of_localized_time"),
    LOCALIZED_DATETIME("of_localized_date_time"),

    /**
     * Formats the string in such format: 06/29/2021
     */
    MM_DD_YYYY("MM/dd/yyyy"),

    /**
     * Formats the string in such format: 29-6-2021 8:20:43
     */
    DD_M_YYYY_HH_MM_SS("dd-M-yyyy hh:mm:ss"),

    /**
     * Formats the string in such format: 29 June 2021
     */
    DD_MMMM_YYYY("dd MMMM yyyy"),

    /**
     * Formats the string in such format: 29 June 2021 Eastern Standard Time
     */
    DD_MMMM_YYYY_ZZZZ("dd MMMM yyyy zzzz"),

    /**
     * Formats the string in such format: Tue, 29 June 2021 8:20:43 EST
     */
    E_DD_MMM_YYYY_HH_MM_SS_Z("E, dd MMM yyyy HH:mm:ss z");

    override fun toString(): String {
        return str
    }
}
