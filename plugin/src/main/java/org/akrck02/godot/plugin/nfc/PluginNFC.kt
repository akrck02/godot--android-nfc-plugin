package org.akrck02.godot.plugin.nfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.core.app.OnNewIntentProvider
import org.godotengine.godot.Godot
import org.godotengine.godot.GodotFragment
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot


class PluginNFC(godot: Godot): GodotPlugin(godot) {

    private var previousIntent: Intent? = null

    private var nfcAdapter: NfcAdapter? = null
    private var status : Int = 0

    private var queuedWriteData: ByteArray? = null


    override fun getPluginName() = "GodotNFC"

    /**
     * Get the plugin godot signals
     */
    override fun getPluginSignals(): Set<SignalInfo> {
        val signals: MutableSet<SignalInfo> = HashSet()

        signals.add(SignalInfo("nfc_enabled", String::class.java))
        signals.add(SignalInfo("tag_readed", ByteArray::class.java))

        return signals
    }

    /**
     * Enable the NFC service
     */
    @UsedByGodot
    fun enableNFC() {
        activity!!.runOnUiThread {
            nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
            status = -1

            nfcAdapter?.let { it ->

                // if not enabled, return
                if (!it.isEnabled){
                    status = 0
                    return@let
                }

                // if enabled enable dispatch
                status = 1

                val intent = Intent(activity, activity!!.javaClass)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

                val intentFilters = arrayOf<IntentFilter>(
                    IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                    IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                    IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
                )

                val pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                it.enableForegroundDispatch(activity, pendingIntent, intentFilters, null)
            }

            Toast.makeText(activity, "NFC World $status", Toast.LENGTH_LONG).show()
            emitSignal("nfc_enabled", "$status")
        }


    }
    
    /**
     * Set the NFC callback
     */
    @UsedByGodot
    fun setNfcCallback() {
        val currentActivity = activity as OnNewIntentProvider
        currentActivity.addOnNewIntentListener { currentIntent ->
            run {
                if (currentIntent.hasExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)) {
                    val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        currentIntent.getParcelableExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, Tag::class.java)
                    } else {
                        currentIntent.getParcelableExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                    }

                    Log.d("NFC", "MESSAGES FOUND $messages")

                    messages?.let {
                        Log.d("NFC", "NFC tag detected: $messages")
                        emitSignal("tag_readed", messages)
                    }
                }

            }
        }
    }
}