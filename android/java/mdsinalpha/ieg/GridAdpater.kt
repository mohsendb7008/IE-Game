package mdsinalpha.ieg

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class CellViewHolder(itemView: View, val ctx: Context): RecyclerView.ViewHolder(itemView){

    enum class State{CLEAR, GOAL, MY_PLAYER, MY_TEAMMATE_PLAYER, OPPONENT_PLAYER}
    var state = State.CLEAR
    var isHighlighted = false

    val card = itemView.findViewById<MaterialCardView>(R.id.card)
    val image = itemView.findViewById<ImageView>(R.id.giImage)
    val topLine = itemView.findViewById<View>(R.id.topLine)
    val bottomLine = itemView.findViewById<View>(R.id.bottomLine)
    val rightLine = itemView.findViewById<View>(R.id.rightLine)
    val leftLine = itemView.findViewById<View>(R.id.leftLine)

    fun highlight(){
        card.setBackgroundColor(ctx.resources.getColor(R.color.colorHighlight))
        isHighlighted = true
    }

    fun clearHighlight(){
        card.setBackgroundColor(Color.WHITE)
        isHighlighted = false
    }

    fun isGoalCell(){
        image.setImageDrawable(ctx.getDrawable(R.drawable.goal))
        state = State.GOAL
    }

    fun isMyPlayerCell(){
        image.setImageDrawable(ctx.getDrawable(R.drawable.boldgreen))
        state = State.MY_PLAYER
    }

    fun isMyTeammatePlayerCell(){
        image.setImageDrawable(ctx.getDrawable(R.drawable.green))
        state = State.MY_TEAMMATE_PLAYER
    }

    fun isOpponentPlayerCell(){
        image.setImageDrawable(ctx.getDrawable(R.drawable.red))
        state = State.OPPONENT_PLAYER
    }

    fun clearCell(){
        image.setImageResource(0)
        state = State.CLEAR
    }

    private fun blockLine(view: View) =
        view.setBackgroundColor(ctx.resources.getColor(R.color.colorAccent))

    fun blockTop() = blockLine(topLine)

    fun blockBottom() = blockLine(bottomLine)

    fun blockRight() = blockLine(rightLine)

    fun blockLeft() = blockLine(leftLine)

    fun setClickListener(listener: View.OnClickListener) =
        card.setOnClickListener(listener)

    fun setLongClickListener(listener: View.OnLongClickListener) =
        card.setOnLongClickListener(listener)
}

class GridAdapter(val ctx: Context, val game: Game): RecyclerView.Adapter<CellViewHolder>(){

    interface OnCellClickListener{
        fun onClick(position: Int, view: View)
    }

    interface OnCellHoldListener{
        fun onHold(position: Int, view: View)
    }

    var onCellClickListener: OnCellClickListener? = null
    var onCellHoldListener: OnCellHoldListener? = null

    override fun getItemCount(): Int = Game.gridRows * Game.gridColumns

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder =
        CellViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.gi_cell, parent, false), ctx)

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        onCellClickListener?.let {
            holder.setClickListener(View.OnClickListener { view ->
                it.onClick(position, view)
            })
        }
        onCellHoldListener?.let {
            holder.setLongClickListener(View.OnLongClickListener {view ->
                it.onHold(position, view)
                true
            })
        }
        val(x, y) = Game.coordinate(position)
        // Bind walls:
        game.walls[Point(x, y)]?.forEach {
            when(it){
                'U' -> holder.blockTop()
                'D' -> holder.blockBottom()
                'L' -> holder.blockLeft()
                'R' -> holder.blockRight()
            }
        }
        // Bind highlights:
        if(game.grid[x][y].isHighlighted)
            holder.highlight()
        else
            holder.clearHighlight()
        // Bind players:
        if(x == Game.gridRows / 2 && y == Game.gridColumns / 2){
            holder.isGoalCell()
            return
        }
        if(holder.state == CellViewHolder.State.CLEAR && game.grid[x][y].player != null){
            when(game.grid[x][y].player) {
                game.myPlayer -> holder.isMyPlayerCell()
                game.myTeammatePlayer -> holder.isMyTeammatePlayerCell()
                else -> holder.isOpponentPlayerCell()
            }
            return
        }
        if(holder.state != CellViewHolder.State.CLEAR && game.grid[x][y].player == null) {
            holder.clearCell()
            return
        }
    }

}