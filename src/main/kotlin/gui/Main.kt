package gui

import calc.*
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.beust.klaxon.json
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.StageStyle
import tornadofx.*
import java.io.File
import java.text.DecimalFormat
import java.text.ParseException
import java.util.*

private val tbMargin = 2.5
private val lrMargin = 2.5
private val barTitle = "PAD Damage Calculator"

class Main : App(MainView::class)

class MainView : View(barTitle) {
    private var version: String by singleAssign()
    private val leader = MonsterView("Leader", this)
    private val sub1 = MonsterView("Sub 1", this)
    private val sub2 = MonsterView("Sub 2", this)
    private val sub3 = MonsterView("Sub 3", this)
    private val sub4 = MonsterView("Sub 4", this)
    private val friend = MonsterView("Friend Leader", this)
    private val multi = MultiView(this)

    private val coop = SimpleBooleanProperty()

    private val combos = CombosTableView(this)
    private val dmgs = DamageDoneMenuView(monsterViews(),
            multi.leaderMulti,
            multi.friendMulti,
            combos,
            coop,
            multi.burst,
            this)

    private val enemyAtt = SimpleObjectProperty<Attribute>()
    private val enemyHP = SimpleLongProperty()
    private val enemyDef = SimpleLongProperty()
    private val enemy = EnemyStatsView(enemyAtt, enemyHP, enemyDef, this)

    private val menubar = MenuBarView(this)

    private var currAllFile: File? = null
    private var currTeamFile: File? = null
    private var currComboFile: File? = null
    private var currEnemyFile: File? = null

    private fun team() = Team(
            multi.leaderMulti.get(),
            multi.friendMulti.get(),
            leader.monster(),
            sub1.monster(),
            sub2.monster(),
            sub3.monster(),
            sub4.monster(),
            friend.monster())

    private fun monsterViews() = listOf(leader, sub1, sub2, sub3, sub4, friend)
    private fun toggleMonsView(monsv: MonsterView) =
            if (monsv.root.isVisible) hideMonsView(monsv) else showMonsView(monsv)

    private fun hideMonsView(monsv: MonsterView) {
        monsv.root.hide()
        if (!primaryStage.isMaximized) primaryStage.sizeToScene(); primaryStage.centerOnScreen()
    }

    private fun showMonsView(monsv: MonsterView) {
        monsv.root.show()
        if (!primaryStage.isMaximized) primaryStage.sizeToScene(); primaryStage.centerOnScreen()
    }

    private val teamFilter = FileChooser.ExtensionFilter("PDC Teams", "*.pdct")
    private val comboFilter = FileChooser.ExtensionFilter("PDC Combos", "*.pdcc")
    private val enemyFilter = FileChooser.ExtensionFilter("PDC Enemy", "*.pdce")
    private val generalSaveFilter = FileChooser.ExtensionFilter("PDC Files", "*.pdc")
    private val generalOpenFilter = FileChooser.ExtensionFilter("PDC Files", "*.pdc*")
    private fun chooseFiles(title: String, filter: FileChooser.ExtensionFilter, mode: FileChooserMode): List<File> {
        val filters =
                arrayOf(
                        filter,
                        FileChooser.ExtensionFilter("JSON", "*.json"),
                        FileChooser.ExtensionFilter("All types", "*.*"))
        return chooseFile(title, filters, mode)
    }

    private fun teamJSONString() = team().toJSON().toJsonString(true)
    private fun combosJSONString() = combos.combosJSON().toJsonString(true)
    private fun enemyJSONString() = enemy.enemyJSON().toJsonString(true)

    private fun allJSONString(): String {
        val teamJSON = team().toJSON()
        val killersJSON = dmgs.killersJSON()
        val comboJSON = combos.combosJSON()
        val enemyJSON = enemy.enemyJSON()
        return json {
            obj("team" to teamJSON["team"],
                    "killers" to killersJSON["killers"],
                    "combos" to comboJSON["combos"],
                    "burst" to multi.burst.get(),
                    "enemy" to enemyJSON["enemy"])
        }.toJsonString(true)
    }

    fun about() {
        information(header = "PAD Damage Calculator v$version\nby Barkuto",
                content = "Discord: Barkuto#2315\n" +
                        "Github: https://github.com/Barkuto/PADDC\n" +
                        "Suggestions\n& Issues: https://github.com/Barkuto/PADDC/issues")
                .title = "About"
    }

    fun new() {
        var confirmed = false
        confirm("Create a new file?\nUnsaved changes will be lost.", actionFn = { confirmed = true })
        if (confirmed) {
            title = barTitle
            resetAll()
            resetAllFiles()
        }
    }

    fun saveCurrent() =
            when {
                currAllFile != null -> saveAll()
                currTeamFile != null -> saveTeam()
                currComboFile != null -> saveCombos()
                currEnemyFile != null -> saveEnemy()
                else -> saveAllAs()
            }

    private fun saveCurrentAs() =
            when {
                currAllFile != null -> saveAllAs()
                currTeamFile != null -> saveTeamAs()
                currComboFile != null -> saveCombosAs()
                currEnemyFile != null -> saveEnemyAs()
                else -> saveAllAs()
            }

    fun saveTeam() = if (currTeamFile != null) currTeamFile!!.writeText(teamJSONString()) else saveTeamAs()
    fun saveCombos() = if (currComboFile != null) currComboFile!!.writeText(combosJSONString()) else saveCombosAs()
    fun saveEnemy() = if (currEnemyFile != null) currEnemyFile!!.writeText(enemyJSONString()) else saveEnemyAs()
    fun saveAll() = if (currAllFile != null) currAllFile!!.writeText(allJSONString()) else saveAllAs()

    fun saveTeamAs() {
        val files = chooseFiles("Save Team As..", teamFilter, FileChooserMode.Save)
        if (files.isNotEmpty()) {
            val file = files[0]
            file.writeText(teamJSONString())
            resetAllFiles()
            currTeamFile = file
            title = "$barTitle: ${file.absolutePath}"
        }
    }

    fun saveCombosAs() {
        val files = chooseFiles("Save Combos As..", comboFilter, FileChooserMode.Save)
        if (files.isNotEmpty()) {
            val file = files[0]
            file.writeText(combosJSONString())
            resetAllFiles()
            currComboFile = file
            title = "$barTitle: ${file.absolutePath}"
        }
    }

    fun saveEnemyAs() {
        val files = chooseFiles("Save Enemy As..", enemyFilter, FileChooserMode.Save)
        if (files.isNotEmpty()) {
            val file = files[0]
            file.writeText(enemyJSONString())
            resetAllFiles()
            currEnemyFile = file
            title = "$barTitle: ${file.absolutePath}"
        }
    }

    fun saveAllAs() {
        val files = chooseFiles("Save All As..", generalSaveFilter, FileChooserMode.Save)
        if (files.isNotEmpty()) {
            val file = files[0]
            file.writeText(allJSONString())
            resetAllFiles()
            currAllFile = file
            title = "$barTitle: ${file.absolutePath}"
        }
    }

    fun readFile() {
        val files = chooseFiles("Open File", generalOpenFilter, FileChooserMode.Single)
        if (files.isNotEmpty()) {
            try {
                var teamFile = false
                var combosFile = false
                var enemyFile = false

                val file = files[0]

                val json = Parser().parse(file.inputStream()) as JsonObject

                val teamJSON = json["team"]
                val killersJSON = json["killers"]
                val combosJSON = json["combos"]
                val enemyJSON = json["enemy"]
                if (teamJSON != null) {
                    val team = teamJSON as JsonArray<*>

                    multi.leaderMulti.set((team[0] as JsonObject)["multi1"].toString().toDouble())
                    multi.friendMulti.set((team[0] as JsonObject)["multi2"].toString().toDouble())
                    multi.burst.set(json["burst"].toString().toDouble())

                    val mons = monsterViews()
                    for (i in 1 until team.size) {
                        val jsonmons = team[i] as JsonObject
                        val name = jsonmons["name"].toString()
                        val att1 = jsonmons["att1"].toString()
                        val att2 = jsonmons["att2"].toString()
                        val atk = jsonmons["attack"].toString()
                        val plusses = jsonmons["plusses"].toString()
                        val atkL = jsonmons["atkl"].toString()
                        val atkPL = jsonmons["atkpl"].toString()

                        val assist = jsonmons["assist"] as JsonObject
                        val assistName = assist["name"].toString()
                        val assistAtt = assist["att"].toString()
                        val assistAtk = assist["attack"].toString()
                        val assistPlusses = assist["plusses"].toString()

                        val awakenings = jsonmons["awakenings"] as JsonArray<*>
                        val awakeningList = mutableListOf<Awakening>()
                        awakenings.forEach {
                            awakeningList.add(Awakening.fromString(it.toString())!!)
                        }
                        mons[i - 1].setfromMonster(Monster(name,
                                Attribute.fromString(att1),
                                Attribute.fromString(att2),
                                atk.toInt(),
                                plusses.toInt(),
                                awakeningList.toTypedArray(),
                                AssistMonster(assistName,
                                        Attribute.fromString(assistAtt),
                                        assistAtk.toLong(),
                                        assistPlusses.toInt()),
                                atkL.toInt(),
                                atkPL.toInt()))
                    }
                    teamFile = true
                }

                if (killersJSON != null) {
                    val killers = killersJSON as JsonArray<*>

                    dmgs.setKillers(killers.map {
                        val obj = it as JsonObject
                        obj["awoken"].toString().toInt() to obj["latent"].toString().toInt()
                    })
                }

                if (combosJSON != null) {
                    val cmbs = combosJSON as JsonArray<*>
                    val list = cmbs
                            .map { it as JsonObject }
                            .map {
                                Combo(
                                        it["num"].toString().toInt(),
                                        Attribute.fromString(it["att"].toString()),
                                        it["oe"].toString().toInt(),
                                        ComboType.fromString(it["type"].toString()))
                            }
                    combos.setCombos(list.toTypedArray())
                    combosFile = true
                }

                if (enemyJSON != null) {
                    val en = enemyJSON as JsonObject
                    enemy.setEnemy(
                            Attribute.fromString(en["att"].toString()),
                            en["hp"].toString().toLong(),
                            en["def"].toString().toLong())
                    enemyFile = true
                }

                if (teamFile && combosFile && enemyFile) {
                    resetAllFiles()
                    currAllFile = file
                } else if (teamFile && !combosFile && !enemyFile) {
                    resetAllFiles()
                    currTeamFile = file
                } else if (!teamFile && combosFile && !enemyFile) {
                    resetAllFiles()
                    currComboFile = file
                } else if (!teamFile && !combosFile && enemyFile) {
                    resetAllFiles()
                    currEnemyFile = file
                }
                updateDamage()
                title = "$barTitle: ${file.absolutePath}"
            } catch (re: RuntimeException) {
                re.printStackTrace()
                // Bad file given
            }
        }
    }

    fun resetTeam() {
        val monsvs = monsterViews()
        monsvs.forEach { it.reset() }
        dmgs.reset()
        enemy.resetDamageDone()
        multi.reset()
    }

    fun resetCombos() = combos.reset()

    fun resetEnemy() = enemy.reset()

    fun resetAll() {
        resetTeam()
        resetCombos()
        resetEnemy()
        resetAllFiles()
    }

    private fun resetAllFiles() {
        currTeamFile = null
        currComboFile = null
        currEnemyFile = null
        currAllFile = null
    }

    fun updateDamage() {
        dmgs.updateDamages()
        enemy.updateHPLeft(dmgs.dmgTotals(), team())
    }

    override val root =
            hbox {
                minWidth = leader.root.minWidth * 6 + multi.root.minWidth
                alignment = Pos.CENTER
                val mons = monsterViews()

                borderpane {
                    left {
                        hbox {
                            val texts = listOf("L", "1", "2", "3", "4", "F")
                            vbox {
                                for (i in 0 until texts.size) {
                                    button(texts[i]) {
                                        vboxConstraints { useMaxWidth = true }
                                        action { toggleMonsView(mons[i]) }
                                    }
                                }
                                this += label("")
                                button("H") {
                                    vboxConstraints { useMaxWidth = true }
                                    action { mons.forEach { hideMonsView(it) } }
                                }
                                button("S") {
                                    vboxConstraints { useMaxWidth = true }
                                    action { mons.forEach { showMonsView(it) } }
                                }
                                this += label("")
                                this += label("")
                                button("I") {
                                    vboxConstraints { useMaxWidth = true }
                                    action { TeamStatsView(team(), coop.get()).openModal(stageStyle = StageStyle.UTILITY, modality = Modality.WINDOW_MODAL, resizable = false) }
                                }
                            }
                        }
                    }

                    center {
                        hbox {
                            this += leader.root
                            this += sub1.root
                            this += sub2.root
                            this += sub3.root
                            this += sub4.root
                            this += friend.root
                        }
                    }

                    right {
                        hbox {
                            vbox {
                                hbox {
                                    this += multi.root.hboxConstraints { alignment = Pos.CENTER }
                                    imageview(resources.image("/CoopBoost.png")) {
                                        hboxConstraints { marginRight = 5.0 }
                                    }
                                    coop.addListener { _, _, _ -> updateDamage() }
                                    checkbox { bind(coop) }
                                }
                                this += combos
                            }

                            vbox {
                                this += dmgs
                                this += styledFieldLabel("")
                                this += enemy
                            }
                        }

                    }

                    top {
                        this += menubar
                    }
                }
            }

    init {
        with(root) {
            val properties = Properties()
            properties.load(Main::class.java.getResourceAsStream("/project.properties"))
            version = properties.getProperty("version")
            setOnKeyPressed { if (it.isShortcutDown && it.isShiftDown && it.code == KeyCode.S) saveCurrentAs() }
        }
        primaryStage.minWidth = leader.root.minWidth * 10 + 20
        primaryStage.sizeToScene()
        primaryStage.centerOnScreen()
    }
}

class MonsterView(private val teamSlot: String,
                  private val parentView: MainView) : View() {
    private var name = SimpleStringProperty("")
    private var mainAtt = SimpleObjectProperty<Attribute>()
    private var subAtt = SimpleObjectProperty<Attribute>()
    private var atk = SimpleIntegerProperty()
    private var plusses = SimpleIntegerProperty()
    private var atkLatents = SimpleIntegerProperty()
    private var atkPLatents = SimpleIntegerProperty()
    private var assistName = SimpleStringProperty("")
    private var assistAtt = SimpleObjectProperty<Attribute>()
    private var assistAtk = SimpleLongProperty()
    private var assistPlusses = SimpleIntegerProperty()

    private var fireRows = SimpleIntegerProperty()
    private var waterRows = SimpleIntegerProperty()
    private var woodRows = SimpleIntegerProperty()
    private var lightRows = SimpleIntegerProperty()
    private var darkRows = SimpleIntegerProperty()

    private var fireOEs = SimpleIntegerProperty()
    private var waterOEs = SimpleIntegerProperty()
    private var woodOEs = SimpleIntegerProperty()
    private var lightOEs = SimpleIntegerProperty()
    private var darkOEs = SimpleIntegerProperty()

    private var numTPA = SimpleIntegerProperty()
    private var numCoop = SimpleIntegerProperty()
    private var numATK = SimpleIntegerProperty()
    private var num7c = SimpleIntegerProperty()
    private var numVoidPen = SimpleIntegerProperty()

    private var mainAttImage = SimpleObjectProperty<Image>(resources.image("/blank.png"))
    private var subAttImage = SimpleObjectProperty<Image>(resources.image("/blank.png"))
    private var assistAttImage = SimpleObjectProperty<Image>(resources.image("/blank.png"))

    val mainAttDamage = SimpleLongProperty(0)
    val subAttDamage = SimpleLongProperty(0)

    fun setfromMonster(monster: Monster) {
        reset()

        name.set(monster.name)
        mainAtt.set(monster.mainAtt)
        subAtt.set(monster.subAtt)
        atk.set(monster.attack)
        plusses.set(monster.plusses)
        atkLatents.set(monster.atkLatents)
        atkPLatents.set(monster.atkPLatents)
        assistName.set(monster.assist.name)
        assistAtk.set(monster.assist.attack)
        assistAtt.set(monster.assist.mainAtt)
        assistPlusses.set(monster.assist.plusses)

        monster.awakenings.forEach {
            when (it) {
                Awakening.FIREROW -> fireRows.set(fireRows.get() + 1)
                Awakening.WATERROW -> waterRows.set(waterRows.get() + 1)
                Awakening.WOODROW -> woodOEs.set(woodOEs.get() + 1)
                Awakening.LIGHTROW -> lightRows.set(lightRows.get() + 1)
                Awakening.DARKROW -> darkRows.set(darkRows.get() + 1)

                Awakening.FIREOE -> fireOEs.set(fireOEs.get() + 1)
                Awakening.WATEROE -> waterOEs.set(waterOEs.get() + 1)
                Awakening.WOODOE -> woodOEs.set(woodOEs.get() + 1)
                Awakening.LIGHTOE -> lightOEs.set(lightOEs.get() + 1)
                Awakening.DARKOE -> darkOEs.set(darkOEs.get() + 1)

                Awakening.TPA -> numTPA.set(numTPA.get() + 1)
                Awakening.COOP -> numCoop.set(numCoop.get() + 1)
                Awakening.ATK -> numATK.set(numATK.get() + 1)
                Awakening.SEVENC -> num7c.set(num7c.get() + 1)

                Awakening.VOIDPEN -> numVoidPen.set(numVoidPen.get() + 1)

                else -> {
                }
            }
        }
    }

    private fun getAwakenings(): Array<Awakening> {
        val awakenings = mutableListOf<Awakening>()
        for (i in 0 until fireRows.get()) awakenings.add(Awakening.FIREROW)
        for (i in 0 until waterRows.get()) awakenings.add(Awakening.WATERROW)
        for (i in 0 until woodRows.get()) awakenings.add(Awakening.WOODROW)
        for (i in 0 until lightRows.get()) awakenings.add(Awakening.LIGHTROW)
        for (i in 0 until darkRows.get()) awakenings.add(Awakening.DARKROW)

        for (i in 0 until fireOEs.get()) awakenings.add(Awakening.FIREOE)
        for (i in 0 until waterOEs.get()) awakenings.add(Awakening.WATEROE)
        for (i in 0 until woodOEs.get()) awakenings.add(Awakening.WOODOE)
        for (i in 0 until lightOEs.get()) awakenings.add(Awakening.LIGHTOE)
        for (i in 0 until darkOEs.get()) awakenings.add(Awakening.DARKOE)

        for (i in 0 until numTPA.get()) awakenings.add(Awakening.TPA)
        for (i in 0 until numCoop.get()) awakenings.add(Awakening.COOP)
        for (i in 0 until numATK.get()) awakenings.add(Awakening.ATK)
        for (i in 0 until num7c.get()) awakenings.add(Awakening.SEVENC)
        for (i in 0 until numVoidPen.get()) awakenings.add(Awakening.VOIDPEN)

        return awakenings.toTypedArray()
    }

    fun name() = name
    fun monster() = Monster(name.get(), mainAtt.get(), subAtt.get(), atk.get(), plusses.get(), getAwakenings(),
            AssistMonster(assistName.get(), assistAtt.get(), assistAtk.get(), assistPlusses.get()))

    fun reset() {
        name.set("")
        mainAtt.set(Attribute.NONE)
        subAtt.set(Attribute.NONE)
        atk.set(0)
        plusses.set(0)
        atkLatents.set(0)
        atkPLatents.set(0)
        assistName.set("")
        assistAtt.set(Attribute.NONE)
        assistAtk.set(0)
        assistPlusses.set(0)

        fireRows.set(0)
        waterRows.set(0)
        woodRows.set(0)
        lightRows.set(0)
        darkRows.set(0)

        fireOEs.set(0)
        waterOEs.set(0)
        woodOEs.set(0)
        lightOEs.set(0)
        darkOEs.set(0)

        numTPA.set(0)
        numCoop.set(0)
        numATK.set(0)
        num7c.set(0)
        numVoidPen.set(0)

        mainAttImage.set(resources.image("/blank.png"))
        subAttImage.set(resources.image("/blank.png"))
        assistAttImage.set(resources.image("/blank.png"))

        mainAttDamage.set(0)
        subAttDamage.set(0)
    }

    override val root = gridpane {
        minWidth = 50.0
        prefWidth = 120.0

        // Slot Name
        row { add(styledTitleLabel(teamSlot)) }

        // Monster Name
        row { this += styled2ColFieldField(name) }

        // Main and Sub Att Dropdowns
        val attlist = FXCollections.observableArrayList<Attribute>()
        attlist.addAll(Attribute.values())
        row {
            this += styledImage(mainAttImage)

            val mainDropdown = styledDropdown(mainAtt, attlist)
            mainDropdown.valueProperty().addListener { _, _, new ->
                when (new) {
                    Attribute.FIRE -> mainAttImage.set(resources.image("/fire.png"))
                    Attribute.WATER -> mainAttImage.set(resources.image("/water.png"))
                    Attribute.WOOD -> mainAttImage.set(resources.image("/wood.png"))
                    Attribute.LIGHT -> mainAttImage.set(resources.image("/light.png"))
                    Attribute.DARK -> mainAttImage.set(resources.image("/dark.png"))
                    else -> mainAttImage.set(resources.image("/blank.png"))
                }
            }
            this += mainDropdown
        }
        row {
            this += styledImage(subAttImage)

            val subDropdown = styledDropdown(subAtt, attlist)
            subDropdown.valueProperty().addListener { _, _, new ->
                when (new) {
                    Attribute.FIRE -> subAttImage.set(resources.image("/fire.png"))
                    Attribute.WATER -> subAttImage.set(resources.image("/water.png"))
                    Attribute.WOOD -> subAttImage.set(resources.image("/wood.png"))
                    Attribute.LIGHT -> subAttImage.set(resources.image("/light.png"))
                    Attribute.DARK -> subAttImage.set(resources.image("/dark.png"))
                    else -> subAttImage.set(resources.image("/blank.png"))
                }
            }
            this += subDropdown
        }

        // Stat Fields
        val fields = arrayOf(atk, plusses, atkLatents, atkPLatents)
        val statImages = arrayOf("atkText.png", "Plus.png", "ATK+.png", "ATK++.png")
        for (i in 0 until fields.size) {
            row {
                this += styledImage(resources.image("/${statImages[i]}"))
                this += styledFieldField(fields[i])
            }
        }

        // Assist Stat Fields
        row { this += styledTitleLabel("Assist") }

        // Assist Name
        row { this += styled2ColFieldField(assistName) }

        // Assist Att Dropdown
        row {
            this += styledImage(assistAttImage)

            val assistDropdown = styledDropdown(assistAtt, attlist)
            assistDropdown.valueProperty().addListener { _, _, new ->
                when (new) {
                    Attribute.FIRE -> assistAttImage.set(resources.image("/fire.png"))
                    Attribute.WATER -> assistAttImage.set(resources.image("/water.png"))
                    Attribute.WOOD -> assistAttImage.set(resources.image("/wood.png"))
                    Attribute.LIGHT -> assistAttImage.set(resources.image("/light.png"))
                    Attribute.DARK -> assistAttImage.set(resources.image("/dark.png"))
                    else -> assistAttImage.set(resources.image("/blank.png"))
                }
            }
            this += assistDropdown
        }

        // Assist Attack
        row {
            this += styledImage(resources.image("/atkText.png"))
            this += styledFieldField(assistAtk)
        }

        // Assist Plusses
        row {
            this += styledImage(resources.image("/Plus.png"))
            this += styledFieldField(assistPlusses)
        }

        val atts = Attribute.values().filter { it != Attribute.NONE }

        // Row Awakenings
        row { this += styledTitleLabel("Rows") }

        val rowsProps = arrayOf(fireRows, waterRows, woodRows, lightRows, darkRows)
        val rowImages = arrayOf("FireRow.png", "WaterRow.png", "WoodRow.png", "LightRow.png", "DarkRow.png")
        for (i in 0 until atts.size) {
            row {
                this += styledImage(resources.image("/${rowImages[i]}"))
                this += styledFieldField(rowsProps[i])
            }
        }

        // OE Awakenings
        row { this += styledTitleLabel("Orb Enhance") }

        val oeProps = arrayOf(fireOEs, waterOEs, woodOEs, lightOEs, darkOEs)
        val oeImages = arrayOf("FireOE.png", "WaterOE.png", "WoodOE.png", "LightOE.png", "DarkOE.png")
        for (i in 0 until atts.size) {
            row {
                this += styledImage(resources.image("/${oeImages[i]}"))
                this += styledFieldField(oeProps[i])
            }
        }

        // Other Awakenings
        row { this += styledTitleLabel("Other") }

        row {
            this += styledImage(resources.image("/ATK.png"))
            this += styledFieldField(numATK)
        }
        row {
            this += styledImage(resources.image("/TPA.png"))
            this += styledFieldField(numTPA)
        }
        row {
            this += styledImage(resources.image("/CoopBoost.png"))
            this += styledFieldField(numCoop)
        }
        row {
            this += styledImage(resources.image("/7c.png"))
            this += styledFieldField(num7c)
        }
        row {
            this += styledImage(resources.image("/VoidPen.png"))
            this += styledFieldField(numVoidPen)
        }
    }

    init {
        // Auto update listeners
        name.addListener { _, _, _ -> parentView.updateDamage() }
        mainAtt.addListener { _, _, _ -> parentView.updateDamage() }
        subAtt.addListener { _, _, _ -> parentView.updateDamage() }
        atk.addListener { _, _, _ -> parentView.updateDamage() }
        plusses.addListener { _, _, _ -> parentView.updateDamage() }
        atkLatents.addListener { _, _, _ -> parentView.updateDamage() }
        atkPLatents.addListener { _, _, _ -> parentView.updateDamage() }
        assistName.addListener { _, _, _ -> parentView.updateDamage() }
        assistAtt.addListener { _, _, _ -> parentView.updateDamage() }
        assistAtk.addListener { _, _, _ -> parentView.updateDamage() }
        assistPlusses.addListener { _, _, _ -> parentView.updateDamage() }

        fireRows.addListener { _, _, _ -> parentView.updateDamage() }
        waterRows.addListener { _, _, _ -> parentView.updateDamage() }
        woodRows.addListener { _, _, _ -> parentView.updateDamage() }
        lightRows.addListener { _, _, _ -> parentView.updateDamage() }
        darkRows.addListener { _, _, _ -> parentView.updateDamage() }

        fireOEs.addListener { _, _, _ -> parentView.updateDamage() }
        waterOEs.addListener { _, _, _ -> parentView.updateDamage() }
        woodOEs.addListener { _, _, _ -> parentView.updateDamage() }
        lightOEs.addListener { _, _, _ -> parentView.updateDamage() }
        darkOEs.addListener { _, _, _ -> parentView.updateDamage() }

        numTPA.addListener { _, _, _ -> parentView.updateDamage() }
        numCoop.addListener { _, _, _ -> parentView.updateDamage() }
        numATK.addListener { _, _, _ -> parentView.updateDamage() }
        num7c.addListener { _, _, _ -> parentView.updateDamage() }
        numVoidPen.addListener { _, _, _ -> parentView.updateDamage() }

        mainAttImage.addListener { _, _, _ -> parentView.updateDamage() }
        subAttImage.addListener { _, _, _ -> parentView.updateDamage() }
        assistAttImage.addListener { _, _, _ -> parentView.updateDamage() }
    }
}

class MultiView(private val parentView: MainView) : View() {
    var leaderMulti = SimpleDoubleProperty(1.0)
    var friendMulti = SimpleDoubleProperty(1.0)
    private var multiplier = SimpleDoubleProperty(1.0)
    var burst = SimpleDoubleProperty(1.0)

    fun reset() {
        leaderMulti.set(1.0)
        friendMulti.set(1.0)
        multiplier.set(1.0)
        burst.set(1.0)
    }

    override val root = gridpane {
        leaderMulti.addListener { _, _, _ -> parentView.updateDamage() }
        friendMulti.addListener { _, _, _ -> parentView.updateDamage() }
        burst.addListener { _, _, _ -> parentView.updateDamage() }

        row { this += styledTitleLabel("Multipliers") }

        row {
            this += styledFieldLabel("Leader")
            val leaderField = styledFieldField(leaderMulti)
            leaderField.textProperty().addListener { _, _, new ->
                try {
                    multiplier.set(new.toDouble() * friendMulti.get())
                } catch (e: NumberFormatException) {
                    leaderMulti.set(0.0)
                } catch (e: ParseException) {
                    leaderMulti.set(0.0)
                }
            }
            this += leaderField
        }

        row {
            this += styledFieldLabel("Friend")
            val friendField = styledFieldField(friendMulti)
            friendField.textProperty().addListener { _, _, new ->
                try {
                    multiplier.set(leaderMulti.get() * new.toDouble())
                } catch (e: NumberFormatException) {
                    friendMulti.set(0.0)
                } catch (e: ParseException) {
                    friendMulti.set(0.0)
                }
            }
            this += friendField
        }

        row {
            this += styledFieldLabel("Multiplier")
            val multiField = styledTitleLabel("")
            multiField.bind(multiplier)
            this += multiField
        }

        row { this += styledTitleLabel("Burst") }
        row {
            val burstField = styledFieldField(burst)
            burstField.gridpaneConstraints { columnSpan = 2 }
            this += burstField
        }
    }
}

class CombosTableView(private val parentView: MainView) : View() {
    private var combos = mutableListOf(Combo()).observable()
    private var selected = SimpleObjectProperty<Combo>(combos[0])
    private var comboTable = TableView(combos)
    private fun numCombos() = if (combos.size < 10) "Combos: 0${combos.size}" else "Combos: ${combos.size}"
    private val numCombosLabel = styledTitleLabel(numCombos())

    fun combos() = combos.toTypedArray()

    fun reset() {
        combos.clear()
        combos.add(Combo())
        selected.set(combos[0])
    }

    fun setCombos(cmbs: Array<Combo>) {
        combos.clear()
        for (c in cmbs) {
            combos.add(c)
        }
    }

    fun combosJSON() = json { obj("combos" to array(combos.map { it.toJSON() })) }

    override val root = vbox {
        hbox {

            useMaxWidth = true
            alignment = Pos.CENTER
            button("Delete Combo").action {
                combos.remove(selected.get())
                if (combos.size > 0) selected.set(combos.last())
                numCombosLabel.text = numCombos()
                parentView.updateDamage()
            }
            button("Add Combo").action {
                if (combos.size < 12) combos.add(Combo())
                selected.set(combos.last())
                numCombosLabel.text = numCombos()
                SmartResize.POLICY.requestResize(comboTable)
                parentView.updateDamage()
            }
            this += styledFieldLabel("      ")
            this += numCombosLabel
        }

        this += comboTable.tableview(combos) {
            maxWidth = 265.0
            minWidth = 0.0

            val attList = FXCollections.observableArrayList<Attribute>()
            attList.addAll(Attribute.values())

            val typeList = FXCollections.observableArrayList<ComboType>()
            typeList.addAll(ComboType.NORMAL, ComboType.ROW, ComboType.BOX)

            fixedCellSize = 30.0
            column("Att", Combo::orbType)
                    .contentWidth(padding = 30.0, useAsMin = true, useAsMax = true)
                    .useComboBox(attList) {
                        alignment = Pos.CENTER
                    }
                    .onEditCommitProperty().addListener { _ -> parentView.updateDamage() }
            column("#Orb", Combo::numOrbs)
                    .contentWidth(useAsMin = true, useAsMax = true)
                    .makeEditable()
                    .setOnEditCommit { value ->
                        val new = value.newValue
                        val combo = combos[value.tablePosition.row]
                        if (new < 9 && combo.comboType == ComboType.BOX) {
                            combo.comboType = ComboType.NORMAL
                        }
                        if (new < 6 && combo.comboType == ComboType.ROW) {
                            combo.comboType = ComboType.NORMAL
                        }
                        combos[value.tablePosition.row].numOrbs = value.newValue

                        parentView.updateDamage()
                    }
            column("#OE", Combo::numOE)
                    .contentWidth(useAsMin = true, useAsMax = true)
                    .makeEditable()
                    .onEditCommitProperty().addListener { _, _, _ -> parentView.updateDamage() }
            column("Type", Combo::comboType)
                    .contentWidth(useAsMin = true, useAsMax = true)
                    .remainingWidth()
                    .useComboBox(typeList).combobox<ComboType> {
                onEditCommit {
                    when (it.comboType) {
                        ComboType.NORMAL -> it.numOrbs = maxOf(it.numOrbs, 3)
                        ComboType.ROW -> it.numOrbs = maxOf(it.numOrbs, 6)
                        ComboType.BOX -> it.numOrbs = maxOf(it.numOrbs, 9)
                        else -> it.numOrbs = 3
                    }
                    parentView.updateDamage()
                    refresh()
                }
            }

            // https://stackoverflow.com/questions/27945817/javafx-adapt-tableview-height-to-number-of-rows
//            prefHeightProperty().bind(fixedCellSizeProperty().multiply(Bindings.size(items) + 1))
//            minHeightProperty().bind(prefHeightProperty())
//            maxHeightProperty().bind(prefHeightProperty())

            columnResizePolicy = SmartResize.POLICY

            bindSelected(selected)
        }
    }
}

class DamageDoneMenuView(private val monsvs: List<MonsterView>,
                         private val leaderMulti: SimpleDoubleProperty,
                         private val friendMulti: SimpleDoubleProperty,
                         private val combos: CombosTableView,
                         private val coop: SimpleBooleanProperty,
                         private val burst: SimpleDoubleProperty,
                         private val parentView: MainView) : View() {
    private val totalDmg = SimpleStringProperty("0")
    private var dmgTotals = SimpleObjectProperty<List<Pair<Long, Long>>>(listOf(
            Pair(0L, 0L),
            Pair(0L, 0L),
            Pair(0L, 0L),
            Pair(0L, 0L),
            Pair(0L, 0L),
            Pair(0L, 0L)))

    private val leaderK = SimpleIntegerProperty()
    private val leaderLK = SimpleIntegerProperty()
    private val sub1K = SimpleIntegerProperty()
    private val sub1LK = SimpleIntegerProperty()
    private val sub2K = SimpleIntegerProperty()
    private val sub2LK = SimpleIntegerProperty()
    private val sub3K = SimpleIntegerProperty()
    private val sub3LK = SimpleIntegerProperty()
    private val sub4K = SimpleIntegerProperty()
    private val sub4LK = SimpleIntegerProperty()
    private val friendK = SimpleIntegerProperty()
    private val friendLK = SimpleIntegerProperty()

    private fun killers() = listOf(
            Pair(leaderK.get(), leaderLK.get()),
            Pair(sub1K.get(), sub1LK.get()),
            Pair(sub2K.get(), sub2LK.get()),
            Pair(sub3K.get(), sub3LK.get()),
            Pair(sub4K.get(), sub4LK.get()),
            Pair(friendK.get(), friendLK.get()))

    private fun killerProps() = listOf(
            Pair(leaderK, leaderLK),
            Pair(sub1K, sub1LK),
            Pair(sub2K, sub2LK),
            Pair(sub3K, sub3LK),
            Pair(sub4K, sub4LK),
            Pair(friendK, friendLK))

    fun dmgTotals() = dmgTotals

    fun updateDamages() {
        val t = Team(leaderMulti.get(),
                friendMulti.get(),
                monsvs[0].monster(),
                monsvs[1].monster(),
                monsvs[2].monster(),
                monsvs[3].monster(),
                monsvs[4].monster(),
                monsvs[5].monster())

        val dmgs = calcTeamDamageWithKillers(calcTeamDamage(t, combos.combos(), coop.get(), burst.get()), killers())
        dmgTotals.set(dmgs)
        for (i in 0 until monsvs.size) {
            monsvs[i].mainAttDamage.set(dmgs[i].first)
            monsvs[i].subAttDamage.set(dmgs[i].second)
        }
        totalDmg.set(DecimalFormat.getInstance().format(totalDamage(dmgs)))
    }

    fun reset() {
        val props = killerProps()
        props.forEach {
            it.first.set(0)
            it.second.set(0)
        }
        totalDmg.set("0")
    }

    fun setKillers(killers: List<Pair<Int, Int>>) {
        val props = killerProps()
        for (i in 0 until killers.size) {
            props[i].first.set(killers[i].first)
            props[i].second.set(killers[i].second)
        }
    }

    fun killersJSON(): JsonObject {
        val killers = killers()
        return json {
            obj("killers" to array(listOf(
                    obj("awoken" to killers[0].first, "latent" to killers[0].second),
                    obj("awoken" to killers[1].first, "latent" to killers[1].second),
                    obj("awoken" to killers[2].first, "latent" to killers[2].second),
                    obj("awoken" to killers[3].first, "latent" to killers[3].second),
                    obj("awoken" to killers[4].first, "latent" to killers[4].second),
                    obj("awoken" to killers[5].first, "latent" to killers[5].second)
            )))
        }
    }

    override val root = vbox {
        gridpane {
            leaderK.addListener { _, _, _ -> parentView.updateDamage() }
            leaderLK.addListener { _, _, _ -> parentView.updateDamage() }
            sub1K.addListener { _, _, _ -> parentView.updateDamage() }
            sub1LK.addListener { _, _, _ -> parentView.updateDamage() }
            sub2K.addListener { _, _, _ -> parentView.updateDamage() }
            sub2LK.addListener { _, _, _ -> parentView.updateDamage() }
            sub3K.addListener { _, _, _ -> parentView.updateDamage() }
            sub3LK.addListener { _, _, _ -> parentView.updateDamage() }
            sub4K.addListener { _, _, _ -> parentView.updateDamage() }
            sub4LK.addListener { _, _, _ -> parentView.updateDamage() }
            friendK.addListener { _, _, _ -> parentView.updateDamage() }
            friendLK.addListener { _, _, _ -> parentView.updateDamage() }

            alignment = Pos.CENTER
            label("DAMAGE") {
                alignment = Pos.CENTER
                style {
                    fontSize = 12.px
                    fontWeight = FontWeight.EXTRA_BOLD
                    borderColor += box(Color.BLACK)
                }
                gridpaneConstraints {
                    useMaxHeight = true
                    useMaxWidth = true
                    columnIndex = 0
                    rowIndex = 0
                }
            }
            label("KILLERS") {
                alignment = Pos.CENTER
                style {
                    fontSize = 12.px
                    fontWeight = FontWeight.EXTRA_BOLD
                    borderColor += box(Color.BLACK)
                }
                gridpaneConstraints {
                    useMaxHeight = true
                    useMaxWidth = true
                    columnIndex = 0
                    rowIndex = 1
                }
            }

            val props = killerProps()
            for (i in 0 until monsvs.size) {
                alignment = Pos.CENTER
                this += DamageDoneView(monsvs[i]).root.gridpaneConstraints {
                    columnIndex = i + 1
                    rowIndex = 0
                }
                this += KillerView(props[i].first, props[i].second).root.gridpaneConstraints {
                    columnIndex = i + 1
                    rowIndex = 1
                }

            }
        }

        label("Total Damage") {
            alignment = Pos.CENTER
            useMaxWidth = true
            style {
                fontSize = 20.px
                fontWeight = FontWeight.EXTRA_BOLD
                borderColor += box(Color.BLACK)
            }
        }
        label(totalDmg) {
            alignment = Pos.CENTER
            useMaxWidth = true
            style {
                fontSize = 30.px
                fontWeight = FontWeight.EXTRA_BOLD
                borderColor += box(Color.BLACK)
            }
        }
    }
}

class DamageDoneView(monsv: MonsterView) : View() {
    override val root = vbox {
        minWidth = 50.0
        prefWidth = 65.0
        this += styledTitleLabel(monsv.name())
        this += styledTitleLabel(monsv.mainAttDamage)
        this += styledTitleLabel(monsv.subAttDamage)
    }
}

class EnemyStatsView(private val att: SimpleObjectProperty<Attribute>,
                     private val hp: SimpleLongProperty,
                     private val def: SimpleLongProperty,
                     private val parentView: MainView) : View() {
    private val attImage = SimpleObjectProperty<Image>(resources.image("/blank.png"))
    private val damageDone = SimpleLongProperty(0)
    private val damageDoneString = SimpleStringProperty("0")

    fun updateHPLeft(dmgs: SimpleObjectProperty<List<Pair<Long, Long>>>, team: Team) {
        damageDone.set(hp.get() - totalDamage(calcTeamDamageAgainstEnemy(dmgs.get(), team, att.get(), def.get())))
        damageDoneString.set(DecimalFormat.getInstance().format(damageDone.get()))
    }

    fun reset() {
        attImage.set(resources.image("/blank.png"))
        resetDamageDone()
        att.set(Attribute.NONE)
        hp.set(0L)
        def.set(0L)
    }

    fun resetDamageDone() {
        damageDone.set(0)
        damageDoneString.set("0")
    }

    fun setEnemy(att: Attribute, hp: Long, def: Long) {
        this.att.set(att)
        this.hp.set(hp)
        this.def.set(def)
    }

    fun enemyJSON(): JsonObject {
        return json {
            obj("enemy" to obj(
                    "att" to att.get().toString(),
                    "hp" to hp.get(),
                    "def" to def.get()))
        }
    }

    override val root = vbox {
        this += styledTitleLabel("Enemy Stats")

        gridpane {
            attImage.addListener { _, _, _ -> parentView.updateDamage() }

            alignment = Pos.CENTER
            val attlist = FXCollections.observableArrayList<Attribute>()
            attlist.addAll(Attribute.values())

            row {
                this += styledImage(attImage)

                val mainDropdown = styledDropdown(att, attlist)
                mainDropdown.valueProperty().addListener { _, _, new ->
                    when (new) {
                        Attribute.FIRE -> attImage.set(resources.image("/fire.png"))
                        Attribute.WATER -> attImage.set(resources.image("/water.png"))
                        Attribute.WOOD -> attImage.set(resources.image("/wood.png"))
                        Attribute.LIGHT -> attImage.set(resources.image("/light.png"))
                        Attribute.DARK -> attImage.set(resources.image("/dark.png"))
                        else -> attImage.set(resources.image("/blank.png"))
                    }
                }
                this += mainDropdown
            }

            val numRegex = Regex("([0-9]+,?[0-9]+)*")
            row {
                this += styledImage(resources.image("/HP.png"))
                val hpField = styledFieldField(hp)
                hpField.textProperty().addListener { _, _, new ->
                    if (new.isNotEmpty() && new.matches(numRegex))
                        hpField.text = DecimalFormat.getInstance().format(new.replace(",", "").toLong())
                }
                this += hpField
            }

            row {
                this += styledImage(resources.image("/DEF.png"))
                val defField = styledFieldField(def)
                defField.textProperty().addListener { _, _, new ->
                    if (new.isNotEmpty() && new.matches(numRegex))
                        defField.text = DecimalFormat.getInstance().format(new.replace(",", "").toLong())
                }
                this += defField
            }
        }
        label("HP Left") {
            alignment = Pos.CENTER
            useMaxWidth = true
            style {
                fontSize = 20.px
                fontWeight = FontWeight.EXTRA_BOLD
                borderColor += box(Color.BLACK)
            }
        }
        label(damageDoneString) {
            alignment = Pos.CENTER
            useMaxWidth = true
            style {
                fontSize = 30.px
                fontWeight = FontWeight.EXTRA_BOLD
                borderColor += box(Color.BLACK)
            }
        }
    }

    init {
        att.addListener { _, _, _ -> parentView.updateDamage() }
        hp.addListener { _, _, _ -> parentView.updateDamage() }
        def.addListener { _, _, _ -> parentView.updateDamage() }
    }
}

class KillerView(private val awokenKillers: SimpleIntegerProperty,
                 private val latentKillers: SimpleIntegerProperty) : View() {
    override val root = hbox {
        alignment = Pos.CENTER
        useMaxWidth = true

        vbox {
            alignment = Pos.CENTER
            imageview(resources.image("/awakening.png")) {
                fitHeight = image.height * 0.50
                fitWidth = image.width * 0.50
            }
            button("+") {
                style { fontSize = 10.px }
                action { awokenKillers.plusAssign(1) }
            }
            label(awokenKillers) {}
            button("-") {
                style { fontSize = 10.px }
                action { awokenKillers.set(Math.max(awokenKillers.get() - 1, 0)) }
            }
        }

        vbox {
            alignment = Pos.CENTER
            imageview(resources.image("/latent.png")) {
                fitHeight = image.height * 0.50
                fitWidth = image.width * 0.50
            }
            button("+") {
                style { fontSize = 10.px }
                action { latentKillers.plusAssign(1) }
            }
            label(latentKillers) {}
            button("-") {
                style { fontSize = 10.px }
                action { latentKillers.set(Math.max(latentKillers.get() - 1, 0)) }
            }
        }
    }
}

class MenuBarView(private val parentView: MainView) : View() {
    override val root = menubar {
        //        menu(graphic = styledImage(resources.image("/Save.png"))) { action { parentView.readFile() } }
        //        menu(graphic = styledImage(resources.image("/Open.png"))) { action { parentView.readFile() } }
        menu("File") {
            item("New", keyCombination = KeyCombination.keyCombination("Shortcut+N")) { action { parentView.new() } }
            item("Open...", keyCombination = KeyCombination.keyCombination("Shortcut+O")) { action { parentView.readFile() } }
            item("Save", keyCombination = KeyCombination.keyCombination("Shortcut+S")) { action { parentView.saveCurrent() } }
            separator()
            item("Save All") { action { parentView.saveAll() } }
            item("Save Team") { action { parentView.saveTeam() } }
            item("Save Combos") { action { parentView.saveCombos() } }
            item("Save Enemy") { action { parentView.saveEnemy() } }
            separator()
            item("Save All As..") { action { parentView.saveAllAs() } }
            item("Save Team As..") { action { parentView.saveTeamAs() } }
            item("Save Combos As..") { action { parentView.saveCombosAs() } }
            item("Save Enemy As..") { action { parentView.saveEnemyAs() } }
        }
        menu("Edit") {
            item("Reset Team") { action { parentView.resetTeam() } }
            item("Reset Combos") { action { parentView.resetCombos() } }
            item("Reset Enemy") { action { parentView.resetEnemy() } }
            item("Reset All") { action { parentView.resetAll() } }
            item("Preferences™") {}.isDisable = true
        }
        menu("?") {
            item("About This") { action { parentView.about() } }
            item("Help™") { action {} }.isDisable = true
        }
    }
}

class TeamStatsView(private val team: Team,
                    private val coop: Boolean) : View() {
    override val root = vbox {
        gridpane {
            val multiplier = team.multiplier()
            val monsters = team.monsters()
            val awakenings = team.totalAwakenings()
            row {
                this += styledTitleLabel("Multiplier: $multiplier")
                        .gridpaneConstraints {
                            fillHeightWidth = true
                            columnSpan = 10
                        }
            }
            row {
                vbox {
                    this += styledTitleLabel("Name")
                    this += styledTitleLabel("Buffed Atk")
                }

                monsters.forEach {
                    vbox {
                        this += styledTitleLabel(it.name)
                        this += styledTitleLabel("${if (coop) it.buffedCoopAtk() else it.buffedAtk()}")
                    }
                }
            }

            row {
                borderpane {
                    useMaxWidth = true
                    alignment = Pos.CENTER
                    left = vbox {
                        alignment = Pos.CENTER
                        val pics = arrayOf(
                                "FireRow.png",
                                "WaterRow.png",
                                "WoodRow.png",
                                "LightRow.png",
                                "DarkRow.png")
                        val ords = arrayOf(
                                Awakening.FIREROW.ordinal,
                                Awakening.WATERROW.ordinal,
                                Awakening.WOODROW.ordinal,
                                Awakening.LIGHTROW.ordinal,
                                Awakening.DARKROW.ordinal)

                        (0 until pics.size)
                                .filter { awakenings[ords[it]] > 0 }
                                .forEach {
                                    hbox {
                                        alignment = Pos.CENTER
                                        this += styledImage(resources.image("/${pics[it]}"))
                                        this += styledTitleLabel("${awakenings[ords[it]]}")
                                    }
                                }
                    }

                    center = hbox {
                        alignment = Pos.CENTER
                        vbox {
                            alignment = Pos.CENTER
                            val pics = arrayOf(
                                    "FireOE.png",
                                    "WaterOE.png",
                                    "WoodOE.png",
                                    "LightOE.png",
                                    "DarkOE.png")
                            val ords = arrayOf(
                                    Awakening.FIREOE.ordinal,
                                    Awakening.WATEROE.ordinal,
                                    Awakening.WOODOE.ordinal,
                                    Awakening.LIGHTOE.ordinal,
                                    Awakening.DARKOE.ordinal)

                            (0 until pics.size)
                                    .filter { awakenings[ords[it]] > 0 }
                                    .forEach {
                                        hbox {
                                            alignment = Pos.CENTER
                                            this += styledImage(resources.image("/${pics[it]}"))
                                            this += styledTitleLabel("${awakenings[ords[it]]}")
                                        }
                                    }
                        }
                    }

                    right = vbox {
                        alignment = Pos.CENTER
                        val pics = arrayOf(
                                "ATK.png",
                                "TPA.png",
                                "CoopBoost.png",
                                "7c.png",
                                "VoidPen.png")
                        val ords = arrayOf(
                                Awakening.ATK.ordinal,
                                Awakening.TPA.ordinal,
                                Awakening.COOP.ordinal,
                                Awakening.SEVENC.ordinal,
                                Awakening.VOIDPEN.ordinal)

                        (0 until pics.size)
                                .filter { awakenings[ords[it]] > 0 }
                                .forEach {
                                    hbox {
                                        alignment = Pos.CENTER
                                        this += styledImage(resources.image("/${pics[it]}"))
                                        this += styledTitleLabel("${awakenings[ords[it]]}")
                                    }
                                }
                    }
                }.gridpaneConstraints { columnSpan = 10 }
            }
        }.vboxConstraints {
            marginLeftRight(10.0)
            marginTop = 5.0
            marginBottom = 10.0
        }
    }
}

private fun styledTitleLabel(s: String) =
        Label(s).label(s) {
            alignment = Pos.CENTER
            useMaxWidth = true
            gridpaneConstraints {
                marginTopBottom(tbMargin)
                marginLeftRight(lrMargin)
                columnSpan = 2
            }
            style {
                fontWeight = FontWeight.EXTRA_BOLD
                borderColor += box(Color.BLACK)
            }
        }

private fun styledTitleLabel(prop: SimpleStringProperty) =
        Label().label(prop) {
            alignment = Pos.CENTER
            useMaxWidth = true
            gridpaneConstraints {
                marginTopBottom(tbMargin)
                marginLeftRight(lrMargin)
            }
            style {
                fontWeight = FontWeight.EXTRA_BOLD
                borderColor += box(Color.BLACK)
            }
        }

private fun styledTitleLabel(prop: SimpleLongProperty) =
        Label().label(prop) {
            alignment = Pos.CENTER
            useMaxWidth = true
            gridpaneConstraints {
                marginTopBottom(tbMargin)
                marginLeftRight(lrMargin)
            }
            style {
                fontWeight = FontWeight.EXTRA_BOLD
                borderColor += box(Color.BLACK)
            }
        }

private fun styledFieldLabel(s: String) = Label(s).label(s) { gridpaneConstraints { marginLeftRight(lrMargin) } }

private fun styledFieldField(prop: SimpleIntegerProperty) =
        TextField().textfield(prop) {
            alignment = Pos.CENTER
            gridpaneConstraints { marginLeftRight(lrMargin) }
        }

private fun styledFieldField(prop: SimpleDoubleProperty) =
        TextField().textfield(prop) {
            alignment = Pos.CENTER
            gridpaneConstraints { marginLeftRight(lrMargin) }
        }

private fun styledFieldField(prop: SimpleLongProperty) =
        TextField().textfield(prop) {
            alignment = Pos.CENTER
            gridpaneConstraints { marginLeftRight(lrMargin) }
        }

private fun styled2ColFieldField(prop: SimpleStringProperty) =
        TextField().textfield(prop) {
            alignment = Pos.CENTER
            gridpaneConstraints {
                columnSpan = 2
                marginLeftRight(lrMargin)
            }
        }

private fun styledDropdown(prop: SimpleObjectProperty<Attribute>, list: ObservableList<Attribute>) =
        ComboBox(list).combobox(prop, list) {
            selectionModel.select(Attribute.NONE)
            gridpaneConstraints {
                useMaxWidth = true
                marginLeftRight(lrMargin)
            }
        }

private fun styledImage(img: Image) =
        ImageView().imageview(img) {
            fitWidth = img.width * 0.75
            fitHeight = img.height * 0.75
            gridpaneConstraints {
                hAlignment = HPos.RIGHT
                vAlignment = VPos.CENTER
            }
        }

private fun styledImage(prop: SimpleObjectProperty<Image>) =
        ImageView().imageview {
            imageProperty().bind(prop)
            fitWidth = prop.get().width * 0.75
            fitHeight = prop.get().height * 0.75
            gridpaneConstraints {
                hAlignment = HPos.RIGHT
                vAlignment = VPos.CENTER
            }
        }