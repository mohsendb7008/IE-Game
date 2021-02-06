package mdsinalpha.ieg

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.exercise_layout.*
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.net.InetAddress
import java.net.Socket
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    fun move(){
        button6.setOnClickListener {
            startActivity(Intent(this@MainActivity, GameActivity::class.java))
        }
    }



    fun loadingState(state: Boolean){
        name.isEnabled = !state
        createNewGame.isEnabled = !state
        gameID.isEnabled = !state
        joinGame.isEnabled = !state
        progressBar.visibility = if(state) View.VISIBLE else View.GONE
        if(state)
            message.text = ""
    }

    override fun onResume() {
        super.onResume()
        loadingState(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        name.requestFocus()
        createNewGame.setOnClickListener {
            val nameS = name.text.toString().trim()
            if (nameS == ""){
                name.error = "Whitespace!"
                return@setOnClickListener
            }
            loadingState(true)
            thread {
                try {
                    socket = Socket(InetAddress.getByName(serverIPAddress), serverPort)
                    inStream = DataInputStream(socket.getInputStream())
                    outStream = PrintWriter(socket.getOutputStream())
                    outStream.println(CreateMessage(nameS).serialize())
                    outStream.flush()
                    val response = inStream.readL()
                    val gameId: Int = JSONObject(response).getInt("id")
                    runOnUiThread {
                        message.text = "Game created with id: $gameId\nwaiting for other players to connect..."
                    }
                    val start = StartMessage.deserialize(inStream.readL())
                    game = Game(start!!.players2DArray)
                    startActivity(Intent(this@MainActivity, GameActivity::class.java))
                }catch (e: Exception){
                    e.printStackTrace()
                    runOnUiThread {
                        loadingState(false)
                        Snackbar.make(mainL, "Error occurred!", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
        joinGame.setOnClickListener {
            val nameS = name.text.toString().trim()
            if (nameS == ""){
                name.error = "Whitespace!"
                return@setOnClickListener
            }
            val gameId = gameID.text.toString().trim()
            if (gameId == ""){
                gameID.error = "Whitespace!"
                return@setOnClickListener
            }
            loadingState(true)
            thread {
                try {
                    socket = Socket(InetAddress.getByName(serverIPAddress), serverPort)
                    inStream = DataInputStream(socket.getInputStream())
                    outStream = PrintWriter(socket.getOutputStream())
                    outStream.println(JoinMessage(nameS, gameId.toInt()).serialize())
                    outStream.flush()
                    val response = inStream.readL()
                    runOnUiThread {
                        message.text = "Joined game with id: $gameId\nwaiting for other players to connect..."
                    }
                    val start = StartMessage.deserialize(inStream.readL())
                    game = Game(start!!.players2DArray)
                    startActivity(Intent(this@MainActivity, GameActivity::class.java))
                }catch (e: Exception){
                    e.printStackTrace()
                    runOnUiThread {
                        loadingState(false)
                        Snackbar.make(mainL, "Error occurred!", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
