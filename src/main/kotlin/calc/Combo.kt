package calc

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import tornadofx.property

class Combo(numOrbs: Int = 3,
            orbType: Attribute = Attribute.NONE,
            numOE: Int = 0,
            comboType: ComboType = ComboType.NORMAL) {

    // UI Properties for use in TableView
    var numOrbs by property(numOrbs)
    var orbType by property(orbType)
    var numOE by property(numOE)
    var comboType by property(comboType)

    override fun toString(): String {
        return "$orbType - $comboType: $numOrbs, $numOE"
    }

    fun toJSON(): JsonObject {
        return json {
            obj(
                    "num" to numOrbs,
                    "att" to orbType.toString(),
                    "oe" to numOE,
                    "type" to comboType.toString())
        }
    }
}

enum class ComboType {
    NORMAL, ROW, TPA, BOX, SPARKLE, CROSS;

    companion object {
        fun fromString(str: String): ComboType {
            val upperStr = str.toUpperCase()
            values().forEach {
                if (upperStr == it.toString()) return it
            }
            return NORMAL
        }
    }
}