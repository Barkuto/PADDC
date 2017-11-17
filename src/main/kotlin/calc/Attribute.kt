package calc

enum class Attribute {
    FIRE, WATER, WOOD, LIGHT, DARK, NONE;

    fun isWeakAgainst(att: Attribute) =
            (this == FIRE && att == WATER) ||
                    (this == WATER && att == WOOD) ||
                    (this == WOOD && att == FIRE) ||
                    (this == LIGHT && att == DARK) ||
                    (this == DARK && att == LIGHT)


    fun isStrongAgainst(att: Attribute) =
            (this == FIRE && att == WOOD) ||
                    (this == WATER && att == FIRE) ||
                    (this == WOOD && att == WATER) ||
                    (this == LIGHT && att == DARK) ||
                    (this == DARK && att == LIGHT)

    companion object {
        fun fromString(str: String): Attribute {
            val upperStr = str.toUpperCase()
            values().forEach {
                if (upperStr == it.toString()) return it
            }
            return NONE
        }
    }
}