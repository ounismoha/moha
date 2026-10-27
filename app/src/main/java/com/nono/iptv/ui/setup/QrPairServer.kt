package com.nono.iptv.ui.setup

import fi.iki.elonen.NanoHTTPD

/** Credentials submitted from the phone's browser. */
data class QrCreds(
    val name: String,
    val server: String,
    val user: String,
    val pass: String,
    val live: Boolean,
    val vod: Boolean
)

/**
 * Tiny embedded web server shown via a QR code. The phone (on the same Wi-Fi) opens the server's
 * URL, fills a form, and submits — the credentials arrive in [onCredentials] on a server thread.
 */
class QrPairServer(
    port: Int,
    private val onCredentials: (QrCreds) -> Unit
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response = try {
        if (session.method == Method.POST) {
            val body = HashMap<String, String>()
            session.parseBody(body)
            val p = session.parameters
            fun v(k: String) = p[k]?.firstOrNull()?.trim().orEmpty()
            val creds = QrCreds(
                name = v("name"),
                server = v("server"),
                user = v("user"),
                pass = v("pass"),
                live = p["live"] != null,
                vod = p["vod"] != null
            )
            if (creds.server.isNotBlank() && creds.user.isNotBlank() && creds.pass.isNotBlank()) {
                onCredentials(creds)
                newFixedLengthResponse(Response.Status.OK, "text/html", successPage())
            } else {
                newFixedLengthResponse(Response.Status.OK, "text/html", formPage("Veuillez remplir le serveur, l'utilisateur et le mot de passe."))
            }
        } else {
            newFixedLengthResponse(Response.Status.OK, "text/html", formPage(null))
        }
    } catch (t: Throwable) {
        newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "error: ${t.message}")
    }

    private fun formPage(error: String?): String = """
        <!doctype html><html lang="fr"><head>
        <meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>nono app — Connexion</title>
        <style>
          body{font-family:system-ui,Arial,sans-serif;background:linear-gradient(135deg,#18253D,#0E1420 55%,#080C14);background-attachment:fixed;color:#fff;margin:0;padding:24px}
          .card{max-width:480px;margin:0 auto}
          h1{font-size:30px;font-weight:800;margin:0 0 4px;letter-spacing:-.5px}
          h1 .p{color:#FF3D7F}
          h1 .w{color:#fff;font-weight:300}
          h1 .a{color:#9AA7BD;font-weight:400;font-size:18px}
          label{display:block;margin:14px 0 4px;color:#9AA7BD;font-size:14px}
          input[type=text],input[type=password]{width:100%;box-sizing:border-box;padding:12px;border-radius:8px;border:1px solid #2a3547;background:#1C2533;color:#fff;font-size:16px}
          .row{margin-top:14px;display:flex;align-items:center;gap:8px}
          button{margin-top:22px;width:100%;padding:14px;border:0;border-radius:10px;background:#4C8BF5;color:#fff;font-size:17px;font-weight:600}
          .err{background:#512; color:#ffbcbc; padding:10px;border-radius:8px;margin-top:10px}
        </style></head><body><div class="card">
        <h1><span class="p">no</span><span class="w">no</span> <span class="a">app</span></h1>
        <p>Entrez les informations de votre abonnement Xtream.</p>
        ${if (error != null) "<div class=err>$error</div>" else ""}
        <form method="POST" action="/">
          <label>Nom de la playlist (facultatif)</label><input type="text" name="name">
          <label>Adresse du serveur (http://host:port)</label><input type="text" name="server" inputmode="url" autocapitalize="off" autocorrect="off">
          <label>Nom d'utilisateur</label><input type="text" name="user" autocapitalize="off" autocorrect="off">
          <label>Mot de passe</label><input type="text" name="pass" autocapitalize="off" autocorrect="off">
          <div class="row"><input type="checkbox" name="live" checked><span>Inclure les chaînes de télévision</span></div>
          <div class="row"><input type="checkbox" name="vod" checked><span>Inclure la VOD (Films &amp; Séries)</span></div>
          <button type="submit">Envoyer au téléviseur</button>
        </form></div></body></html>
    """.trimIndent()

    private fun successPage(): String = """
        <!doctype html><html lang="fr"><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Envoyé</title>
        <style>body{font-family:system-ui,Arial,sans-serif;background:#0E1420;color:#fff;text-align:center;padding:60px 20px}
        h1{color:#4C8BF5}</style></head><body>
        <h1>✓ Envoyé</h1><p>Vos informations ont été envoyées au téléviseur.<br>Regardez l'écran du téléviseur.</p>
        </body></html>
    """.trimIndent()
}
