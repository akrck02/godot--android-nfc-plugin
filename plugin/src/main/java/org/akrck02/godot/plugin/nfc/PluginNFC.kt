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
     * Poll tags
     */
    @UsedByGodot
    fun pollTags() {

        val intent: Intent = GodotFragment.getCurrentIntent()
        if (intent === previousIntent) return
        previousIntent = intent


        Log.d("NFC", "INTENT FOUND")
        Log.d("NFC", intentToString(intent))

        if (intent.hasExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)) {
            val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, Tag::class.java)
            } else {
                intent.getParcelableExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            }

            Log.d("NFC", "MESSAGES FOUND $messages")

            messages?.let {
                Log.d("NFC", "NFC tag detected: $messages")
                emitSignal("tag_readed", messages)
            }
        }

    }
}
fun intentToString(intent: Intent?): String {
    if (intent == null) {
        return "null"
    }

    return intent.toString() + " " + bundleToString(intent.extras)
}

fun bundleToString(bundle: Bundle?): String {
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
