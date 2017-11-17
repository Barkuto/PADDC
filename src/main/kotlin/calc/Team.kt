package calc

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json

class Team(var multi1: Double = 0.0,
           var multi2: Double = 0.0,
           var leader: Monster = Monster(),
           var sub1: Monster = Monster(),
           var sub2: Monster = Monster(),
           var sub3: Monster = Monster(),
           var sub4: Monster = Monster(),
           var friend: Monster = Monster()) {

    fun monsters() = arrayOf(leader, sub1, sub2, sub3, sub4, friend)

    fun totalAwakenings(): Array<Int> {
        val awakeningArray = Array(Awakening.values().size, { 0 })
        monsters().forEach {
            it.awakenings.forEach { a ->
                awakeningArray[a.ordinal]++
            }
        }
        return awakeningArray
    }

    fun multiplier() = multi1 * multi2

    override fun toString(): String {
        var str = "|%-10s|%-10s|%-10s|%-10s|%-10s|%-10s|\n".format(leader.name, sub1.name, sub2.name, sub3.name, sub4.name, friend.name)
        str += "|%-10d|%-10d|%-10d|%-10d|%-10d|%-10d|\n".format(leader.buffedAtk(), sub1.buffedAtk(), sub2.buffedAtk(), sub3.buffedAtk(), sub4.buffedAtk(), friend.buffedAtk())
        str += "|%-10d|%-10d|%-10d|%-10d|%-10d|%-10d|".format(leader.buffedSubAtk(), sub1.buffedSubAtk(), sub2.buffedSubAtk(), sub3.buffedSubAtk(), sub4.buffedSubAtk(), friend.buffedSubAtk())
        return str
    }

    fun toJSON(): JsonObject {
        return json {
            obj("team" to array(listOf(
                    obj("multi1" to multi1,
                            "multi2" to multi2),
                    leader.toJSON(),
                    sub1.toJSON(),
                    sub2.toJSON(),
                    sub3.toJSON(),
                    sub4.toJSON(),
                    friend.toJSON())))

        }
    }
}