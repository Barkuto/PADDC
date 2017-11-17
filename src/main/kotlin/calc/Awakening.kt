package calc

enum class Awakening {
    FIREROW, WATERROW, WOODROW, LIGHTROW, DARKROW,
    FIREOE, WATEROE, WOODOE, LIGHTOE, DARKOE,
    TPA, COOP, SEVENC, FUA, SFUA, VOIDPEN, ATK;

    companion object {
        fun fromString(str: String): Awakening? {
            val upperStr = str.toUpperCase()
            values().forEach {
                if (upperStr == it.toString()) return it
            }
            return null
        }
    }
}