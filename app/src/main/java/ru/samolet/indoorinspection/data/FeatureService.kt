package ru.samolet.indoorinspection.data

import android.util.Log
import com.jessecorbett.diskord.bot.bot
import com.jessecorbett.diskord.bot.events

class FeatureService {

    private val separator = "#"

    suspend fun getFeatureFlags(onFlagsReceived: (HashMap<String, Int>) -> Unit) {
        bot(token) {
            events {
                onReady {
                    val messages = HashMap<String, Int>()
                    channel(channelId).getMessages(numberOfFlags)
                        .forEachIndexed { i, message ->
                            Log.d("Tests", "${message.sentAt} ${message.content} ")
                            try {
                                val (flag, value) = message.content.split(separator)
                                messages[flag] = value.toInt()
                            } catch (e: Exception) {
                                Log.e("Tests", e.toString())
                            }
                        }
                    onFlagsReceived(messages)
                    Log.d("Tests", messages.toList().toString())
                }
            }
        }
    }

    fun getDefaultValue(): Int {
        return 1
    }

    companion object {
        private const val token = "API TOKEN"
        private const val channelId = "CHANNEL ID"
        private const val numberOfFlags = 10
    }
}