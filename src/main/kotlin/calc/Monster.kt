package calc

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json

class Monster(var name: String = "",
              var mainAtt: Attribute = Attribute.NONE,
              var subAtt: Attribute = Attribute.NONE,
              var attack: Int = 0,
              var plusses: Int = 0,
              var awakenings: Array<Awakening> = emptyArray(),
              var assist: AssistMonster = AssistMonster(),
              var atkLatents: Int = 0,
              var atkPLatents: Int = 0) {

    fun buffedAtk(): Long {
        val plussesGain = if (plusses >= 99) 495 else plusses * 5
        val atkLGain = Math.round(attack * atkLatents * 0.01)
        val atkPLGain = Math.round(attack * atkPLatents * 0.05)
        val assistGain = if (assist.mainAtt == mainAtt) assist.bonusAttack() else 0
        val enhancedATKGain = awakenings.count { it == Awakening.ATK } * 100
        return attack + enhancedATKGain + plussesGain + atkLGain + atkPLGain + assistGain
    }

    fun buffedCoopAtk() = if (awakenings.contains(Awakening.COOP)) Math.ceil(buffedAtk() * coopBonus()).toLong() else buffedAtk()

    fun subAtk() = Math.ceil(subMulti() * attack).toLong()

    fun buffedSubAtk() = Math.ceil(subMulti() * buffedAtk()).toLong()

    fun buffedSubCoopAtk() =
            if (awakenings.contains(Awakening.COOP))
                Math.ceil(subMulti() * buffedAtk() * coopBonus()).toLong()
            else buffedSubAtk()

    private fun subMulti() = (if (mainAtt == subAtt) (1.0 / 10.0) else (1.0 / 3.0))
    private fun coopBonus() = Math.pow(1.5, awakenings.count { it == Awakening.COOP }.toDouble())

    fun toAssist() = AssistMonster(name, mainAtt, attack.toLong(), plusses)

    override fun toString(): String {
        return name
    }

    fun toJSON(): JsonObject {
        return json {
            obj(
                    "name" to name,
                    "att1" to mainAtt.toString(),
                    "att2" to subAtt.toString(),
                    "attack" to attack,
                    "plusses" to plusses,
                    "awakenings" to array(awakenings.map { it.toString() }),
                    "assist" to assist.toJSON(),
                    "atkl" to atkLatents,
                    "atkpl" to atkPLatents)
        }
    }
}

class AssistMonster(var name: String = "",
                    var mainAtt: Attribute = Attribute.NONE,
                    var attack: Long = 0,
                    var plusses: Int = 0) {

    // Assist stat bonus is rounded normally
    fun bonusAttack() = Math.round((attack + (if (plusses >= 99) 495 else 0)) * 0.05)

    fun toJSON(): JsonObject {
        return json {
            obj("name" to name, "att" to mainAtt.toString(), "attack" to attack, "plusses" to plusses)
        }
    }
}