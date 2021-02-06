package mdsinalpha.ieg

import java.io.DataInputStream
import java.io.PrintWriter
import java.net.Socket

const val serverIPAddress = "192.168.43.171"
const val serverPort = 5002

lateinit var socket: Socket
lateinit var inStream: DataInputStream
lateinit var outStream: PrintWriter
lateinit var game: Game

fun DataInputStream.readL(): String{
    val line = this.readLine()
    if(line == null){
        Thread.sleep(1000)
        return this.readL()
    }
    return line
}
