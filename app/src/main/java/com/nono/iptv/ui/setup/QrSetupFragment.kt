package com.nono.iptv.ui.setup

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.nono.iptv.NonoApp
import com.nono.iptv.R
import com.nono.iptv.util.DebugLog
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Shows a QR code pointing at an embedded web server. The user enters credentials on their phone
 * (same Wi-Fi) and submits; the TV receives them and connects. No more typing with the remote.
 */
class QrSetupFragment : Fragment(R.layout.fragment_qr_setup) {

    private var server: QrPairServer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var handled = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val qr: ImageView = view.findViewById(R.id.iv_qr)
        val urlView: TextView = view.findViewById(R.id.tv_qr_url)
        val status: TextView = view.findViewById(R.id.tv_qr_status)

        val ip = localIpAddress()
        if (ip == null) {
            status.text = "Connexion WiFi non détectée. Connectez le téléviseur au WiFi puis réessayez."
            return
        }

        val port = 8686
        val url = "http://$ip:$port/"
        try {
            server = QrPairServer(port) { creds -> onCredentials(creds) }.also { it.start() }
        } catch (t: Throwable) {
            DebugLog.e("QR", "server start failed", t)
            status.text = "Impossible de démarrer le serveur: ${t.message}"
            return
        }

        qr.setImageBitmap(makeQr(url, 600))
        urlView.text = url
        status.text = "Scannez ce code avec votre téléphone (même WiFi)."
        DebugLog.d("QR", "pairing server on $url")
    }

    private fun onCredentials(c: QrCreds) {
        if (handled) return
        handled = true
        mainHandler.post {
            if (!isAdded) return@post
            val status = view?.findViewById<TextView>(R.id.tv_qr_status)
            status?.text = "Informations reçues — connexion…"
            lifecycleScope.launch {
                val result = NonoApp.repository().importXtream(
                    name = c.name, server = c.server, username = c.user, password = c.pass,
                    includeLive = c.live, includeVod = c.vod
                )
                result.onSuccess {
                    (activity as? SetupActivity)?.finishToMain()
                }.onFailure {
                    status?.text = "Échec: ${it.message}"
                    handled = false // allow another attempt from the phone
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        server?.stop()
        server = null
    }

    private fun makeQr(text: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val offset = y * size
            for (x in 0 until size) {
                pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
            setPixels(pixels, 0, size, 0, 0, size, size)
        }
    }

    /**
     * The TV's real LAN address. We must NOT return the Wi-Fi Direct / hotspot interface
     * (p2p0 → 192.168.49.x), which the phone on the home Wi-Fi can't reach — that was the bug
     * behind the blank page. Prefer wlan*, then eth*, then any other usable site-local address.
     */
    private fun localIpAddress(): String? {
        return try {
            val candidates = ArrayList<Pair<String, String>>()
            for (ni in NetworkInterface.getNetworkInterfaces()) {
                if (!ni.isUp || ni.isLoopback || ni.isVirtual) continue
                val name = (ni.name ?: "").lowercase()
                if (name.startsWith("p2p") || name.startsWith("dummy") ||
                    name.startsWith("rmnet") || name.startsWith("tun") ||
                    name.startsWith("docker")
                ) continue
                for (addr in ni.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address && addr.isSiteLocalAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("192.168.49.")) continue // Wi-Fi Direct group owner
                        candidates.add(name to ip)
                    }
                }
            }
            DebugLog.d("QR", "ip candidates: " + candidates.joinToString { "${it.first}=${it.second}" })
            candidates.firstOrNull { it.first.startsWith("wlan") }?.second
                ?: candidates.firstOrNull { it.first.startsWith("eth") }?.second
                ?: candidates.firstOrNull()?.second
        } catch (t: Throwable) {
            DebugLog.e("QR", "ip detection failed", t)
            null
        }
    }
}
