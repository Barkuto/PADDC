package calc

/**
 * If sub att is same as main att, sub att does (1/10) of main att dmg
 * If sub att is different than main att, sub att does (1/3) of dmg
 *
 * Orb Bonus: 25% additional damage for each additional orb, 3=100%,4=125%,5=150%,...10=275% <- Applied at end
 * Combo Bonus: 25% additional damage for each additional combo, 1=100%,2=125%,...,5=200% <- Applied at end
 * TPA Bonus: 1.5 * num of TPAs <- Applied per combo
 * Row Bonus: 1 + (0.01 * #Row Awakenings * #Rows matched) <- Applied at end
 * OE Bonus: [1 + (0.06 * #OE matched)] * [1 + (0.05 * #OE Awakening)] <- Applied per combo
 *
 * Box Bonus: 2.5, only applies to the box orb damage <- Applied to individual combo damage
 * 7c bonus: 2 <- Applied at end
 * Burst <- Applied at end
 * Killers(Lat/Awa) <- Applied at end
 *
 * Attribute Diff <- Applied at end
 *
 * In general, most damages are normally rounded.
 * Individual combo damages are rounded up though.
 */

fun calcTeamDamageAgainstEnemy(damages: List<Pair<Long, Long>>, team: Team, enemyAtt: Attribute, enemyDef: Long): List<Pair<Long, Long>> {
    val monsters = team.monsters()
    val dmg = mutableListOf<Pair<Long, Long>>()
    for (i in 0 until monsters.size) {
        // Strong against does 2.0x damage
        // Weak against does 0.5x damage
        val mainAttFactor =
                when {
                    monsters[i].mainAtt.isStrongAgainst(enemyAtt) -> 2.0
                    monsters[i].mainAtt.isWeakAgainst(enemyAtt) -> 0.5
                    else -> 1.0
                }
        val subAttFactor =
                when {
                    monsters[i].subAtt.isStrongAgainst(enemyAtt) -> 2.0
                    monsters[i].subAtt.isWeakAgainst(enemyAtt) -> 0.5
                    else -> 1.0
                }
        dmg.add(Pair(
                if (damages[i].first > 0)
                    Math.max((Math.round(mainAttFactor * damages[i].first) - enemyDef), 1)
                else 0
                ,
                if (damages[i].second > 0)
                    Math.max((Math.round(subAttFactor * damages[i].second) - enemyDef), 1)
                else 0))
    }
    return dmg
}

// NOTE: GENERIC KILLER APPLICATION
// i.e the # killers given is the amount that will be applied.
// The enemy typing is not taken into account, just number of awoken/latent killers
fun calcTeamDamageWithKillers(damages: List<Pair<Long, Long>>, killers: List<Pair<Int, Int>>): List<Pair<Long, Long>> {
    // Pair -> (# Killer Awakenings, # Killer Latents)
    val dmgs = mutableListOf<Pair<Long, Long>>()
    for (i in 0 until damages.size) {
        // Awoken Killers do 3.0^n times more damage where n=#of awoken killers to apply to the enemy
        // Latent Killers do 1.5^n times more damage where n=#of latent killers to apply to the enemy
        val killerMulti = Math.max(Math.pow(3.0, killers[i].first.toDouble()) * Math.pow(1.5, killers[i].second.toDouble()), 1.0)
        dmgs.add(Pair(
                Math.round(killerMulti * damages[i].first),
                Math.round(killerMulti * damages[i].second)
        ))
    }
    return dmgs
}

fun calcTeamDamage(team: Team, combos: Array<Combo>, coop: Boolean = false, burst: Double = 1.0): List<Pair<Long, Long>> {
    val teamAwakenings = team.totalAwakenings()
    val multiplier = team.multiplier()
    val damages = mutableListOf<Pair<Long, Long>>()
    team.monsters().forEach {
        val dmg = calcMonsterDamage(it, combos, teamAwakenings, coop, burst)
        damages.add(Pair(
                Math.round(multiplier * dmg.first),
                Math.round(multiplier * dmg.second)))
    }
    return damages
}

private fun calcMonsterDamage(monster: Monster, combos: Array<Combo>, teamAwakenings: Array<Int>, coop: Boolean = false, burst: Double = 1.0): Pair<Long, Long> {
    val mainAtk = if (coop) monster.buffedCoopAtk() else monster.buffedAtk()
    val subAtk = if (coop) monster.buffedSubCoopAtk() else monster.buffedSubAtk()
    val mainAttDmg = calcAttDamage(mainAtk, monster.mainAtt, monster.awakenings, combos, teamAwakenings, burst)
    var subAttDmg = calcAttDamage(subAtk, monster.subAtt, monster.awakenings, combos, teamAwakenings, burst)
    subAttDmg = ((if (monster.mainAtt == monster.subAtt) (1.0 / 10.0) else (1.0 / 3.0)) * subAttDmg).toLong()
    return Pair(mainAttDmg, subAttDmg)
}

private fun calcAttDamage(attack: Long, attribute: Attribute, monsterAwakenings: Array<Awakening>, combos: Array<Combo>, teamAwakenings: Array<Int>, burst: Double = 1.0): Long {
    val relevantCombos = combos.filter { it.orbType != Attribute.NONE && it.orbType == attribute }

    val numRowAwakenings = when (attribute) {
        Attribute.FIRE -> teamAwakenings[Awakening.FIREROW.ordinal]
        Attribute.WATER -> teamAwakenings[Awakening.WATERROW.ordinal]
        Attribute.WOOD -> teamAwakenings[Awakening.WOODROW.ordinal]
        Attribute.LIGHT -> teamAwakenings[Awakening.LIGHTROW.ordinal]
        Attribute.DARK -> teamAwakenings[Awakening.DARKROW.ordinal]
        Attribute.NONE -> 0
    }

    val numOEAwakenings = when (attribute) {
        Attribute.FIRE -> teamAwakenings[Awakening.FIREOE.ordinal]
        Attribute.WATER -> teamAwakenings[Awakening.WATEROE.ordinal]
        Attribute.WOOD -> teamAwakenings[Awakening.WOODOE.ordinal]
        Attribute.LIGHT -> teamAwakenings[Awakening.LIGHTOE.ordinal]
        Attribute.DARK -> teamAwakenings[Awakening.DARKOE.ordinal]
        Attribute.NONE -> 0
    }

    // Overall Bonuses
    val comboBonus = 1 + (0.25 * (combos.size - 1))
    val rowBonus = 1 + ((0.1 * numRowAwakenings) * combos.count { it.comboType == ComboType.ROW })
    val sevenBonus = Math.max(Math.pow(2.0, monsterAwakenings.count { it == Awakening.SEVENC }.toDouble()), 1.0)

    // Specific Bonuses
    val tpaBonus = Math.max(Math.pow(1.5, monsterAwakenings.count { it == Awakening.TPA }.toDouble()), 1.0)
    val voidpenBonus = Math.max(Math.pow(2.5, monsterAwakenings.count { it == Awakening.VOIDPEN }.toDouble()), 1.0)
    fun oeBonus(oeMatched: Int) = (1 + (0.06 * oeMatched)) * (1 + (0.05 * numOEAwakenings))
    fun orbBonus(numOrbs: Int) = (1 + (0.25 * (numOrbs - 3)))

    var baseDmgTotal = 0.0
    relevantCombos.forEach {
        // Round up each combo with its bonuses specific to combos
        // TPA bonus is applied on the outside of the other bonuses, then rounded.
        baseDmgTotal += Math.round(
                Math.ceil(attack
                        * orbBonus(it.numOrbs)
                        * (if (it.comboType == ComboType.BOX) voidpenBonus else 1.0)
                        * (if (it.numOE > 0) oeBonus(it.numOE) else 1.0))

                        * (if (it.numOrbs == 4) tpaBonus else 1.0))
    }
    // Ceiling the overall damage total along with the overall bonuses
    return Math.round(burst * (Math.ceil(baseDmgTotal * rowBonus * comboBonus) * (if (combos.size >= 7) sevenBonus else 1.0)))
}

fun totalDamage(damages: List<Pair<Long, Long>>) = damages.fold(0L, { a, x -> a + x.first + x.second })