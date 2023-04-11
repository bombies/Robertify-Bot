package main.constants


enum class PermissionKt(val code: Int) {
    ROBERTIFY_ADMIN(0),
    ROBERTIFY_DJ(1),
    ROBERTIFY_POLLS(2),
    ROBERTIFY_BAN(3),
    ROBERTIFY_THEME(4),
    ROBERTIFY_8BALL(5);

    companion object {
        val codes: List<Int>
            get() {
                val codes: MutableList<Int> = ArrayList()
                for (p in values()) codes.add(p.code)
                return codes
            }
        val permissions: List<String>
            get() {
                val ret: MutableList<String> = ArrayList()
                for (p in values()) ret.add(p.name)
                return ret
            }

        fun parse(code: Int): PermissionKt {
            return when (code) {
                0 -> ROBERTIFY_ADMIN
                1 -> ROBERTIFY_DJ
                2 -> ROBERTIFY_POLLS
                3 -> ROBERTIFY_BAN
                4 -> ROBERTIFY_THEME
                5 -> ROBERTIFY_8BALL
                else -> throw IllegalArgumentException("Invalid code!")
            }
        }
    }
}
