/*
 * (C) 2023 vkozhemi
 *      All rights reserved
 *
 * 4-24-2023; Volodymyr Kozhemiakin
 */

package com.vkozhemi.androidserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class ServerService : Service() {
    private lateinit var serverSocket: ServerSocket
    private lateinit var clientSocket: Socket
    private var out: BufferedWriter? = null
    private var ins: BufferedReader? = null

    override fun onCreate() {

        // Setup foreground service
        val NOTIFICATION_CHANNEL_ID = packageName
        val channelName = "TCP Server Foreground Service"
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        channel.lightColor = Color.GREEN

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification =
            notificationBuilder.setOngoing(true).setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("TCP Server is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE).build()

        startForeground(IMPORTANCE_LOW, notification)

        runServerSocket()
    }

    private fun runServerSocket() {
        // run the remote TCP/IP ServerSocket on an IO thread
        GlobalScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d(TAG, "Server created... $serverSocket")

                // Accept multiple connections
                while (true) {
                    try {
                        clientSocket = serverSocket.accept()
                        Log.i(TAG, "New client: $clientSocket")

                        // read Client massage
                        ins = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                        val message: String? = ins?.readLine()
                        Log.i(TAG, "Incoming: $message")

                        // write Server massage
                        out = BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream()))
                        out?.write(MESSAGE)
                        out?.flush()

                    } catch (e: IOException) {
                        Log.e(TAG, "Can't send message. IOException: ", e)
                    } finally {
                        clientSocket.close()
                        out?.close()
                        ins?.close()
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Can't create Server: ", e)
                try {
                    clientSocket.close()
                    serverSocket.close()
                } catch (ex: IOException) {
                    Log.e(TAG, "Can't close socket: ", e)
                }
            }
        }
    }

    override fun onDestroy() {
        clientSocket.close()
        serverSocket.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private val TAG = ServerService::class.java.simpleName

        // Port should be same as Client port
        private const val PORT = 8765

        // Message that sends to Client
        private const val MESSAGE = "Message from Server!"
    }
}