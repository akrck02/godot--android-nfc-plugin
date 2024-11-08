package org.akrck02.godot.plugin.nfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.tech.NfcA
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.core.app.OnNewIntentProvider
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot


class PluginNFC(godot: Godot): GodotPlugin(godot) {

    private val TAG : String = "NfcPlugin"
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

                val techListsArray = arrayOf(arrayOf(NfcA::class.java.name))
                val pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE)
                it.enableForegroundDispatch(activity, pendingIntent, intentFilters, techListsArray)
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
                Log.d("NFC", intentToString(currentIntent))

                if (NfcAdapter.ACTION_TAG_DISCOVERED == currentIntent.action || NfcAdapter.ACTION_TECH_DISCOVERED == currentIntent.action) {
                    Log.d("NFC", "Tag Intent received.")

                    val msgs: Array<NdefMessage?> = getNdefMessagesFromIntent(currentIntent)
                    msgs.let {
                        Log.d("NFC", "NFC tag detected")
                        // emitSignal("tag_readed", msgs[0]?.toByteArray())
                    }
                }
            }
        }
    }

    private fun getNdefMessagesFromIntent(intent: Intent): Array<NdefMessage?> {

        // Parse the intent
        var msgs: Array<NdefMessage?> = arrayOf()
        val action = intent.action
        if (action == NfcAdapter.ACTION_TAG_DISCOVERED || action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMsgs != null) {
                msgs = arrayOfNulls(rawMsgs.size)
                for (i in rawMsgs.indices) {
                    msgs[i] = rawMsgs[i] as NdefMessage
                }
            } else {
                // Unknown tag type
                val empty = byteArrayOf()
                val record = NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty)
                val msg = NdefMessage(arrayOf(record))
                msgs = arrayOf(msg)
            }
        } else {
            Log.e(TAG, "Unknown intent.")
        }

        return msgs
    }

    private fun intentToString(intent: Intent?): String {
        if (intent == null) {
            return "null"
        }
        return intent.toString() + " " + bundleToString(intent.extras)
    }

    private fun bundleToString(bundle: Bundle?): String {
        val out = java.lang.StringBuilder("Bundle[")
        if (bundle == null) {
            out.append("null")
        } else {
            var first = true
            for (key in bundle.keySet()) {
                if (!first) {
                    out.append(", ")
                }
                out.append(key).append('=')
                val value = bundle[key]
                if (value is IntArray) {
                    out.append(value.contentToString())
                } else if (value is ByteArray) {
                    out.append(value.contentToString())
                } else if (value is BooleanArray) {
                    out.append(value.contentToString())
                } else if (value is ShortArray) {
                    out.append(value.contentToString())
                } else if (value is LongArray) {
                    out.append(value.contentToString())
                } else if (value is FloatArray) {
                    out.append(value.contentToString())
                } else if (value is DoubleArray) {
                    out.append(value.contentToString())
                } else if (value is Array<*> && value.isArrayOf<String>()) {
                    out.append((value as Array<String?>).contentToString())
                } else if (value is Array<*> && value.isArrayOf<CharSequence>()) {
                    out.append((value as Array<CharSequence?>).contentToString())
                } else if (value is Array<*> && value.isArrayOf<Parcelable>()) {
                    out.append((value as Array<Parcelable?>).contentToString())
                } else if (value is Bundle) {
                    out.append(bundleToString(value))
                } else {
                    out.append(value)
                }
                first = false
            }
        }
        out.append("]")
        return out.toString()
    }


}