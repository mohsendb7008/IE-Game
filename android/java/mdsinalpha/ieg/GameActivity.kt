package mdsinalpha.ieg

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_game.*
import java.lang.Exception
import kotlin.concurrent.thread

class GameActivity : AppCompatActivity(){

    val adapter = GridAdapter(this, game)
    var myNeighbors = ArrayList<Cell>()
    var onHold = false
    lateinit var selectedPoint: Point

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        recycler.layoutManager = GridLayoutManager(this, Game.gridColumns)
        recycler.adapter = adapter
        adapter.onCellClickListener = object : GridAdapter.OnCellClickListener{
            override fun onClick(position: Int, view: View){
                val coordinate = Game.coordinate(position)
                if(onHold && game.grid[coordinate.first][coordinate.second].isHighlighted) thread{
                    try {
                        outStream.println(BlockMessage(game.myPlayer.id, selectedPoint, Point(coordinate.first, coordinate.second), game.turn).serialize())
                        outStream.flush()
                        myNeighbors.forEach {
                            it.isHighlighted = false
                        }
                        myNeighbors.clear()
                        runOnUiThread {
                            adapter.notifyDataSetChanged()
                        }
                    }
                    catch (e: Exception){
                        e.printStackTrace()
                        runOnUiThread {
                            Snackbar.make(gameL, "Error occurred!", Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
                else if(game.grid[coordinate.first][coordinate.second].isHighlighted) thread {
                    try {
                        val(x1, y1, x2, y2) = listOf(game.myPlayer.coordinate.row, game.myPlayer.coordinate.column, coordinate.first, coordinate.second)
                        var a = ""
                        if(x1 == x2 && y2 == y1 + 1)
                            a = "R"
                        else if(x1 == x2 && y2 == y1 - 1)
                            a = "L"
                        else if(y1 == y2 && x2 == x1 + 1)
                            a = "D"
                        else if(y1 == y2 && x2 == x1 - 1)
                            a = "U"
                        if(a == "")
                            throw Exception("R,L,D,U Expected!")
                        outStream.println(MoveMessage(game.myPlayer.id, a, game.turn).serialize())
                        outStream.flush()
                        myNeighbors.forEach {
                            it.isHighlighted = false
                        }
                        runOnUiThread {
                            adapter.notifyDataSetChanged()
                        }
                        myNeighbors.clear()
                    }
                    catch (e: Exception){
                        e.printStackTrace()
                        runOnUiThread {
                            Snackbar.make(gameL, "Error occurred!", Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        adapter.onCellHoldListener = object: GridAdapter.OnCellHoldListener{
            override fun onHold(position: Int, view: View) {
                if(game.turn == game.myPlayer.id) {
                    val coordinate = Game.coordinate(position)
                    selectedPoint = Point(coordinate.first, coordinate.second)
                    myNeighbors.forEach {
                        it.isHighlighted = false
                    }
                    myNeighbors = game.adjacentList(game.grid[selectedPoint.row][selectedPoint.column], false)
                    myNeighbors.forEach {
                        it.isHighlighted = true
                    }
                    adapter.notifyDataSetChanged()
                    onHold = true
                }
            }
        }
        thread {
            while(true){
                try {
                    val message = Message.deserialize(inStream.readL())!!
                    if(message is WinMessage){
                        runOnUiThread {
                            val dialog = Dialog(this)
                            dialog.setContentView(R.layout.dialog_win)
                            val(i1, i2) = if(message.group == game.players[0][0].group) (0 to 0) to (0 to 1) else (1 to 0) to (1 to 1)
                            dialog.findViewById<TextView>(R.id.win).text = "${game.players[i1.first][i1.second].name} and ${game.players[i2.first][i2.second].name} won!"
                            dialog.show()
                        }
                        break
                    }
                    if(message is NOPMessage){
                        myNeighbors.forEach {
                            it.isHighlighted = false
                        }
                        myNeighbors.clear()
                        runOnUiThread {
                            adapter.notifyDataSetChanged()
                        }
                    }
                    val (myTurn, updates) = game.update(message)
                    runOnUiThread {
                        title = "Cycle : ${game.cycle}"
                        turn.text = "It's ${if(myTurn) "your" else "${game.playersWithId[game.turn]?.name}'s"} turn!"
                        updates.forEach {
                            adapter.notifyItemChanged(it)
                        }
                    }
                    if(myTurn){
                        myNeighbors = game.adjacentList(game.grid[game.myPlayer.coordinate.row][game.myPlayer.coordinate.column])
                        myNeighbors.forEach {
                            it.isHighlighted = true
                            runOnUiThread {
                                adapter.notifyItemChanged(Game.index(it.coordinate.row, it.coordinate.column))
                            }
                        }
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                    Thread.sleep(10000)
                    runOnUiThread {
                        Snackbar.make(gameL, "Error occurred!", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}