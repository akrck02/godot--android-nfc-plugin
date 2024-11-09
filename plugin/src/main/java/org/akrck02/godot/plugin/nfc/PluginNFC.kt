package org.akrck02.godot.plugin.nfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.tech.NfcA
import android.os.Build
import android.os.Parcelable

import androidx.core.app.OnNewIntentProvider
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

/**
 * Godot plugin for NFC reading
 * this plugin only reads plain/text mimetype
 * data for now.
 */
@Suppress("unused")
class PluginNFC(godot: Godot) : GodotPlugin(godot) {

    private var nfcAdapter: NfcAdapter? = null
    private var status: Int = 0

    override fun getPluginName() = "GodotNFC"

    /**
     * Get the plugin godot signals
     */
    override fun getPluginSignals(): Set<SignalInfo> {
        val signals: MutableSet<SignalInfo> = HashSet()
        signals.add(SignalInfo("nfc_enabled", String::class.java))
        signals.add(SignalInfo("nfc_scanned", String::class.java))
        return signals
    }

    /**
     * Enable the NFC service
     */
    @UsedByGodot
    fun enableNfc() {

        activity!!.runOnUiThread {
            nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
            status = -1

            nfcAdapter?.let {

                // if not enabled, return
                if (!it.isEnabled) {
                    status = 0
                    return@let
                }

                // if enabled enable dispatch
                status = 1

                val intent = Intent(activity, activity!!.javaClass)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

                val intentFilters = arrayOf(
                    IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                    IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                    IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
                )

                val techListsArray = arrayOf(arrayOf(NfcA::class.java.name))
                val pendingIntent =
                    PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE)
                it.enableForegroundDispatch(activity, pendingIntent, intentFilters, techListsArray)
            }

            emitSignal("nfc_enabled", "$status")
        }

    }

    /**
     * Set the NFC callback
     */
    @UsedByGodot
    fun setNfcCallback() {
        val currentActivity = activity as OnNewIntentProvider
        currentActivity.addOnNewIntentListener { currentIntent -> handleNewIntent(currentIntent) }
    }


    /**
     * Handle new intents
     */
    private fun handleNewIntent(intent: Intent) {

        if (NfcAdapter.ACTION_TAG_DISCOVERED != intent.action && NfcAdapter.ACTION_TECH_DISCOVERED != intent.action)
            return
        
        val messages: Array<NdefMessage?> = getNdefMessagesFromIntent(intent)
        if (messages.isEmpty())
            return

        val json: StringBuilder = StringBuilder()
        val message: NdefMessage? = messages[0]
        message?.records?.forEach { record ->
            when (record.toMimeType()) {
                "text/plain" -> json.append(record.payload.decodeToString())
            }
        }
        emitSignal("nfc_scanned", json.toString())
    }

    /**
     * Get NDEF messages from intent
     */
    @Suppress("DEPRECATION")
    private fun getNdefMessagesFromIntent(intent: Intent): Array<NdefMessage?> {

        val messages: Array<NdefMessage?>
        val rawMessages: Array<Parcelable>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES,
                    Parcelable::class.java
                )
            } else {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            }

        if (rawMessages == null)
            return arrayOf()

        messages = arrayOfNulls(rawMessages.size)
        for (i in rawMessages.indices) {
            messages[i] = rawMessages[i] as NdefMessage
        }

        return messages
    }

    /**
     * companion object
     */
    companion object {
        private const val TAG: String = "NfcPlugin"
    }
}