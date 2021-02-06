package mdsinalpha.ieg

import com.beust.klaxon.Klaxon
import org.json.JSONObject

interface JStringSerializable{
    fun serialize(): String
}

interface JSONSerializable{
    fun toJSON(): JSONObject
}

data class Point(val row: Int, val column: Int): JStringSerializable, JSONSerializable{
    override fun serialize() =
        """{"row": $row, "column": $column}"""
    override fun toJSON() = JSONObject().put("row", row).put("column", column)
    companion object{
        fun deserialize(string: String): Point? = Klaxon().parse<Point>(string)
    }
}

data class Player(val id: Int, val name: String, val group: Int, val teammate: Int, var coordinate: Point): JStringSerializable{
    override fun serialize() =
        """{"id": $id, "name": "$name", "group": $group, "teammate": $teammate, "coordinate": ${coordinate.serialize()}}"""
    companion object{
        fun deserialize(string: String): Player? = Klaxon().parse<Player>(string)
    }
}

data class Cell(val coordinate: Point, var player: Player? = null): JStringSerializable{
    val isEmpty: Boolean get() = player == null
    var isHighlighted = false
    override fun serialize() =
        """"coordinate": ${coordinate.serialize()}, "player": ${player?.serialize()} "isEmpty": $isEmpty"""
    companion object{
        fun deserialize(string: String): Cell? = Klaxon().parse<Cell>(string)
    }
}

open class Message(val type: String, val attrs: MutableMap<String, Any> = HashMap()): JStringSerializable{
    override fun serialize(): String  =
            JSONObject().put("type", type).also {
                attrs.forEach { (key, value) ->
                    it.put(key, if(value is JSONSerializable) value.toJSON() else value)
                }
            }.toString()
    companion object{
        fun deserialize(string: String): Message? = when(JSONObject(string)["type"]){
            "CREATE" -> CreateMessage.deserialize(string)
            "JOIN" -> JoinMessage.deserialize(string)
            "START" -> StartMessage.deserialize(string)
            "WIN" -> WinMessage.deserialize(string)
            "NOP" -> NOPMessage.deserialize(string)
            "MOVE" -> MoveMessage.deserialize(string)
            "BLOCK" -> BlockMessage.deserialize(string)
            else -> null
        }
    }
}

class CreateMessage(val name: String): Message("CREATE"){
    init {
        attrs["name"] = name
    }
    companion object{
        fun deserialize(string: String): CreateMessage? = Klaxon().parse<CreateMessage>(string)
    }
}

class JoinMessage(val name: String, val id: Int): Message("JOIN"){
    init {
        attrs["name"] = name
        attrs["id"] = id
    }
    companion object{
        fun deserialize(string: String): JoinMessage? = Klaxon().parse<JoinMessage>(string)
    }
}

class StartMessage(val players: List<Player>): Message("START"){
    init {
        attrs["players"] = players
    }
    companion object{
        fun deserialize(string: String): StartMessage? = Klaxon().parse<StartMessage>(string)
    }
    val players2DArray: Array<Array<Player>> get() = arrayOf(arrayOf(players[0], players[1]), arrayOf(players[2], players[3]))
}

class WinMessage(val player: Int, val group: Int): Message("WIN"){
    init {
        attrs["group"] = group
    }
    companion object{
        fun deserialize(string: String): WinMessage? = Klaxon().parse<WinMessage>(string)
    }
}

class NOPMessage(val turn: Int): Message("NOP"){
    init {
        attrs["turn"] = turn
    }
    companion object{
        fun deserialize(string: String): NOPMessage? = Klaxon().parse<NOPMessage>(string)
    }
}

class MoveMessage(val player: Int, val action: String, val turn: Int): Message("MOVE"){
    init {
        attrs["player"] = player
        attrs["action"] = action
        attrs["turn"] = turn
    }
    companion object{
        fun deserialize(string: String): MoveMessage? = Klaxon().parse<MoveMessage>(string)
    }
}

class BlockMessage(val player: Int, val point1: Point, val point2: Point, val turn: Int): Message("BLOCK"){
    init {
        attrs["player"] = player
        attrs["point1"] = point1
        attrs["point2"] = point2
        attrs["turn"] = turn
    }
    companion object{
        fun deserialize(string: String): BlockMessage? = Klaxon().parse<BlockMessage>(string)
    }
}
