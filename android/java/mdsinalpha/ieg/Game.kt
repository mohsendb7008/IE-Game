package mdsinalpha.ieg

class Game(val players: Array<Array<Player>>){

    companion object{
        const val gridRows: Int = 13
        const val gridColumns: Int = 13
        fun index(row: Int, column: Int) = row * gridColumns + column
        fun coordinate(index: Int) = index / gridColumns to index % gridColumns
    }

    val grid = Array(gridRows){ row -> Array(gridColumns){ column -> Cell(Point(row, column))}}
    val walls = HashMap<Point, HashSet<Char>>()
    var cycle = 0
    var turn = 0

    val playersWithId = HashMap<Int, Player>()
    val myPlayer: Player get() = players[0][0]
    val myTeammatePlayer: Player get() = players[0][1]
    val opponentPlayers: Pair<Player, Player> get() = players[1][0] to players[1][1]

    init {
        players.forEach { row ->
            row.forEach {
                playersWithId[it.id] = it
                grid[it.coordinate.row][it.coordinate.column].player = it
            }
        }
    }

    fun adjacentList(cell: Cell, shouldNotContainPlayer: Boolean = true): ArrayList<Cell>{
        val adjacentList = ArrayList<Cell>()
        for(i in listOf(-1 to 0, +1 to 0, 0 to -1, 0 to +1)){
                if(walls[cell.coordinate]?.contains('U') == true && i.first == -1 && i.second == 0)
                    continue
                if(walls[cell.coordinate]?.contains('D') == true && i.first == +1 && i.second == 0)
                    continue
                if(walls[cell.coordinate]?.contains('L') == true && i.first == 0 && i.second == -1)
                    continue
                if(walls[cell.coordinate]?.contains('R') == true && i.first == 0 && i.second == +1)
                    continue
                val(x, y) = cell.coordinate.row + i.first to cell.coordinate.column + i.second
                Point(x, y).takeIf{ it.row in 0 until gridRows && it.column in 0 until gridColumns && (!shouldNotContainPlayer || grid[x][y].player == null) }?.let {
                    adjacentList.add(grid[it.row][it.column])
                }
        }
        return adjacentList
    }

    fun update(message: Message): Pair<Boolean, List<Int>>{
        val updates = ArrayList<Int>()
        cycle++
        when(message){
            is NOPMessage -> {
                turn = message.turn
                return (turn == game.myPlayer.id) to updates
            }
            is MoveMessage -> {
                turn = message.turn
                val player = game.playersWithId[message.player]
                player?.coordinate?.let {
                    game.grid[it.row][it.column].player = null
                    updates.add(index(it.row, it.column))
                    val newCoordinate = when(message.action){
                        "U" -> Point(it.row - 1, it.column)
                        "D" -> Point(it.row + 1, it.column)
                        "R" -> Point(it.row, it.column + 1)
                        "L" -> Point(it.row, it.column - 1)
                        else -> return@let
                    }
                    player.coordinate = newCoordinate
                    game.grid[newCoordinate.row][newCoordinate.column].player = player
                    updates.add(index(newCoordinate.row, newCoordinate.column))
                }
                return (turn == game.myPlayer.id) to updates
            }
            is BlockMessage -> {
                turn = message.turn
                val(p1, p2) = message.point1 to message.point2
                if(p1 !in game.walls)
                    game.walls[p1] = HashSet()
                if(p2 !in game.walls)
                    game.walls[p2] = HashSet()
                if(p1.row == p2.row){
                    if(p1.column == p2.column + 1){
                        // p1 -> L, p2 -> R
                        game.walls[p1]?.add('L')
                        game.walls[p2]?.add('R')
                    }
                    else if(p2.column == p1.column + 1){
                        // p1 -> R, p2 -> L
                        game.walls[p1]?.add('R')
                        game.walls[p2]?.add('L')
                    }
                }
                else if(p1.column == p2.column){
                    if(p1.row == p2.row + 1){
                        // p1 -> U, p2 -> D
                        game.walls[p1]?.add('U')
                        game.walls[p2]?.add('D')
                    }
                    else if(p2.row == p1.row + 1){
                        // p1 -> D, p2 -> U
                        game.walls[p1]?.add('D')
                        game.walls[p2]?.add('U')
                    }
                }
                updates.add(index(p1.row, p1.column))
                updates.add(index(p2.row, p2.column))
                return (turn == game.myPlayer.id) to updates
            }
        }
        return false to updates
    }

}