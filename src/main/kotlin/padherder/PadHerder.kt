package padherder

import calc.Attribute
import calc.Awakening
import calc.Monster
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import org.h2.jdbc.JdbcSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.singleAssign
import java.io.*
import java.net.URL

private val path = "./PadHerder/"
private val db = "monsters.db"

object Monsters : Table() {
    val id = integer("id")
    val searchName = varchar("search", 128)
    val name = varchar("name", 128)
    val name_jp = varchar("name_jp", 128)
    val att1 = varchar("att1", 5)
    val att2 = varchar("att2", 5)
    val atk = integer("atk")
    val awokens = binary("awokens", 1024)
    val image = varchar("image", 128)
}

class DBMonster(val id: Int,
                val name: String,
                val mainAtt: Attribute,
                val subAtt: Attribute,
                val attack: Int,
                val awakenings: Array<Awakening>,
                val image: String) {
    fun toMonster() = Monster(name, mainAtt, subAtt, attack, 99, awakenings)
}

class PadHerder {

    companion object {
        private fun connectToDB() = Database.connect("jdbc:h2:$path$db", driver = "org.h2.Driver")

        fun update() {
            val monsters = URL("https://www.padherder.com/api/monsters/")
            val conn = monsters.openConnection()
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0")
            conn.connect()
            val json = Parser().parse(conn.getInputStream()) as JsonArray<*>
            conn.getInputStream().close()

            val f = File(path); if (!f.exists()) f.mkdirs()
            connectToDB()

            transaction {
                drop(Monsters)
                create(Monsters)

                for (o in json) {
                    val obj = o as JsonObject

                    val idv = obj["id"].toString().toInt()
                    val searchnamev = obj["name"].toString().toLowerCase()
                    val namev = obj["name"].toString()
                    val namejpv = obj["name_jp"].toString()
                    val att1v =
                            when (obj["element"]) {
                                0 -> Attribute.FIRE
                                1 -> Attribute.WATER
                                2 -> Attribute.WOOD
                                3 -> Attribute.LIGHT
                                4 -> Attribute.DARK
                                else -> Attribute.NONE
                            }.toString()
                    val att2v =
                            when (obj["element2"]) {
                                0 -> Attribute.FIRE
                                1 -> Attribute.WATER
                                2 -> Attribute.WOOD
                                3 -> Attribute.LIGHT
                                4 -> Attribute.DARK
                                else -> Attribute.NONE
                            }.toString()
                    val atkv = obj["atk_max"].toString().toInt()
                    val imagev = "http://puzzledragonx.com/en/img/book/$idv.png"
                    val awokensArray = obj["awoken_skills"] as JsonArray<*>

                    val baos = ByteArrayOutputStream()
                    val oos = ObjectOutputStream(baos)
                    oos.writeObject(awokensArray.filter {
                        when (it) {// Valid awoken IDs
                            14, 15, 16, 17, 18,
                            22, 23, 24, 25, 26,
                            27, 30, 43, 48, 2 -> true
                            else -> false
                        }
                    }.map {
                        when (it) {
                            14 -> Awakening.FIREOE
                            15 -> Awakening.WATEROE
                            16 -> Awakening.WOODOE
                            17 -> Awakening.LIGHTOE
                            18 -> Awakening.DARKOE

                            22 -> Awakening.FIREROW
                            23 -> Awakening.WATERROW
                            24 -> Awakening.WOODROW
                            25 -> Awakening.LIGHTROW
                            26 -> Awakening.DARKROW

                            27 -> Awakening.TPA
                            30 -> Awakening.COOP
                            43 -> Awakening.SEVENC
                            48 -> Awakening.VOIDPEN
                            2 -> Awakening.ATK
                            else -> null
                        }
                    }.map { it.toString() })
                    oos.flush()
                    baos.close()

                    Monsters.insert {
                        it[id] = idv
                        it[searchName] = searchnamev
                        it[name] = namev
                        it[name_jp] = namejpv
                        it[att1] = att1v
                        it[att2] = att2v
                        it[atk] = atkv
                        it[awokens] = baos.toByteArray()
                        it[image] = imagev
                    }
                }
            }
        }

        fun get(query: String): Array<DBMonster> {
            return try {
                try {
                    getByID(query.trim().toInt())
                } catch (e: NumberFormatException) {
                    getByKeywords(query)
                }
            } catch (se: JdbcSQLException) {
                throw FileNotFoundException("No Monster Database file.")
            }
        }

        private fun getByKeywords(query: String): Array<DBMonster> {
            var newQuery = query.toLowerCase().trim()
            val atts = newQuery.split(" ").filter { it.matches(Regex("^[rbgld][/]?[rbgldx]?$")) }
            val att = if (atts.isNotEmpty()) atts[0] else ""
            val att1 =
                    if (att.isNotEmpty()) {
                        when (att[0]) {
                            'r' -> Attribute.FIRE
                            'b' -> Attribute.WATER
                            'g' -> Attribute.WOOD
                            'l' -> Attribute.LIGHT
                            'd' -> Attribute.DARK
                            else -> null
                        }
                    } else null
            val att2 =
                    if (att.length == 2) {
                        when (att[1]) {
                            'r' -> Attribute.FIRE
                            'b' -> Attribute.WATER
                            'g' -> Attribute.WOOD
                            'l' -> Attribute.LIGHT
                            'd' -> Attribute.DARK
                            'x' -> Attribute.NONE
                            else -> null
                        }
                    } else null
            var check1 = false
            var check2 = false
            if (att1 != null) check1 = true
            if (att2 != null) check2 = true
            if (check1 || check2) newQuery = newQuery.replace(att, "").trim()

            var queryResults: List<ResultRow> by singleAssign()
            connectToDB()
            transaction {
                queryResults =
                        (if (check1 && check2) Monsters.select { Monsters.searchName.like("%$newQuery%") and Monsters.att1.eq(att1.toString()) and Monsters.att2.eq(att2.toString()) }
                        else if (check1) Monsters.select { Monsters.searchName.like("%$newQuery%") and Monsters.att1.eq(att1.toString()) }
                        else Monsters.select { Monsters.searchName like "%$newQuery%" }).toList()
            }
            return queryToDBMonsArray(queryResults)
        }

        private fun getByID(id: Int): Array<DBMonster> {
            var query: List<ResultRow> by singleAssign()
            connectToDB()
            transaction { query = Monsters.select { Monsters.id eq id }.toList() }
            return queryToDBMonsArray(query)
        }

        private fun queryToDBMonsArray(query: List<ResultRow>): Array<DBMonster> {
            return query.map { it ->
                val byteArray = it[Monsters.awokens]
                val bais = ByteArrayInputStream(byteArray)
                val ois = ObjectInputStream(bais)
                val obj = (ois.readObject() as List<*>).map { Awakening.fromString(it as String) }
                DBMonster(it[Monsters.id],
                        it[Monsters.name],
                        Attribute.fromString(it[Monsters.att1]),
                        Attribute.fromString(it[Monsters.att2]),
                        it[Monsters.atk],
                        obj.toTypedArray().requireNoNulls(),
                        it[Monsters.image])
            }.toTypedArray()
        }
    }
}