package com.josvpn.india

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class MainActivity : Activity() {
    companion object { private const val PICK_PROFILE = 4101; private const val VPN_PERMISSION = 4102 }

    private val bg = Color.rgb(11, 18, 32)
    private val panel = Color.rgb(22, 32, 51)
    private val mint = Color.rgb(53, 208, 186)
    private val white = Color.WHITE
    private var profileText: String? = null
    private var profileName = "No profile imported"
    private var selectedApps: Set<String> = emptySet()
    private var backend: Backend? = null
    private var connected = false
    private lateinit var status: TextView
    private lateinit var profileLabel: TextView
    private lateinit var connectButton: Button
    private val tunnel = object : Tunnel {
        override fun getName() = "josvpn"
        override fun onStateChange(newState: Tunnel.State) {
            runOnUiThread {
                connected = newState == Tunnel.State.UP
                renderStatus()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backend = GoBackend(this)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(26), dp(22), dp(20)); setBackgroundColor(bg)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val brand = TextView(this).apply {
            text = "JOS VPN INDIA"; textSize = 25f; setTextColor(white); typeface = Typeface.DEFAULT_BOLD
        }
        val subtitle = TextView(this).apply {
            text = "PRIVATE CONNECTION  •  WIREGUARD"; textSize = 11f; setTextColor(mint); setPadding(0, dp(8), 0, dp(26))
        }
        status = TextView(this).apply { textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(white) }
        val statusCard = card().apply { addView(status); addView(TextView(this@MainActivity).apply {
            text = "Your server location depends on the imported profile."; textSize = 13f; setTextColor(Color.LTGRAY); setPadding(0, dp(8), 0, 0)
        }) }
        profileLabel = TextView(this).apply { text = profileName; textSize = 14f; setTextColor(white) }
        val profileCard = card().apply { addView(TextView(this@MainActivity).apply {
            text = "WIREGUARD PROFILE"; textSize = 11f; setTextColor(mint); typeface = Typeface.DEFAULT_BOLD
        }); addView(profileLabel.apply { setPadding(0, dp(10), 0, 0) }) }
        connectButton = Button(this).apply { text = "CONNECT VPN"; setOnClickListener { onConnectClicked() } }
        val importButton = actionButton("Import WireGuard .conf") { openProfilePicker() }
        val appsButton = actionButton("Select apps for VPN") { chooseApps() }
        val note = TextView(this).apply {
            text = "Security note: your .conf file contains private-key material. Keep it private. App selection is split tunnelling, not a firewall."
            textSize = 12f; setTextColor(Color.LTGRAY); setPadding(dp(2), dp(22), dp(2), dp(10))
        }
        content.addView(brand); content.addView(subtitle); content.addView(statusCard); content.addView(profileCard)
        content.addView(connectButton, matchWrap()); content.addView(importButton, matchWrap()); content.addView(appsButton, matchWrap()); content.addView(note)
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        renderStatus()
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)); setBackgroundColor(panel)
        val p = LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(14); layoutParams = p
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; setOnClickListener { action() }
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun renderStatus() {
        if (!::status.isInitialized) return
        status.text = if (connected) "● Connected" else "● Disconnected"
        status.setTextColor(if (connected) mint else white)
        if (::connectButton.isInitialized) connectButton.text = if (connected) "DISCONNECT VPN" else "CONNECT VPN"
        if (::profileLabel.isInitialized) profileLabel.text = "$profileName\n${selectedApps.size} app(s) selected for VPN"
    }

    private fun openProfilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "text/*"
        }
        startActivityForResult(intent, PICK_PROFILE)
    }

    @Deprecated("Deprecated by Android, retained for broad API compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_PERMISSION) {
            if (resultCode == RESULT_OK) connect() else toast("VPN permission was not granted.")
            return
        }
        if (requestCode == PICK_PROFILE && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                contentResolver.openInputStream(uri).use { stream ->
                    val text = stream?.bufferedReader()?.readText() ?: error("Could not read selected file")
                    require(text.contains("[Interface]") && text.contains("[Peer]")) { "This does not look like a WireGuard .conf file." }
                    profileText = text
                    profileName = uri.lastPathSegment?.substringAfterLast('/') ?: "WireGuard profile imported"
                    selectedApps = emptySet()
                    renderStatus()
                    toast("Profile imported. Keep it private.")
                }
            } catch (e: Exception) { toast("Import failed: ${e.message ?: "invalid profile"}") }
        }
    }

    private data class AppChoice(val label: String, val packageName: String)
    private fun chooseApps() {
        val pm = packageManager
        val choices = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.packageName != packageName }
                .map { AppChoice(pm.getApplicationLabel(it).toString(), it.packageName) }
                .sortedBy { it.label.lowercase() }
        } catch (_: Exception) { emptyList() }
        if (choices.isEmpty()) { toast("No apps found"); return }
        val labels = choices.map { "${it.label}  (${it.packageName})" }.toTypedArray()
        val checked = BooleanArray(choices.size) { choices[it].packageName in selectedApps }
        AlertDialog.Builder(this).setTitle("Apps routed through VPN")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton("Save") { _, _ ->
                selectedApps = choices.indices.filter { checked[it] }.map { choices[it].packageName }.toSet()
                renderStatus()
                toast("App selection saved. Reconnect to apply it.")
            }.setNegativeButton("Cancel", null).show()
    }

    private fun onConnectClicked() {
        if (connected) { disconnect(); return }
        val original = profileText
        if (original.isNullOrBlank()) { toast("Import a WireGuard .conf profile first."); return }
        val permissionIntent = android.net.VpnService.prepare(this)
        if (permissionIntent != null) startActivityForResult(permissionIntent, VPN_PERMISSION) else connect()
    }

    private fun connect() {
        Thread {
            try {
                val raw = profileText ?: error("No profile")
                val configured = applyAppSelection(raw, selectedApps)
                val config = Config.parse(ByteArrayInputStream(configured.toByteArray(StandardCharsets.UTF_8)))
                backend?.setState(tunnel, Tunnel.State.UP, config)
                runOnUiThread { connected = true; renderStatus(); toast("VPN connected") }
            } catch (e: Exception) {
                runOnUiThread { connected = false; renderStatus(); toast("Connection failed: ${e.message ?: "check profile"}") }
            }
        }.start()
    }

    private fun disconnect() {
        Thread {
            try { backend?.setState(tunnel, Tunnel.State.DOWN, null) }
            catch (e: Exception) { runOnUiThread { toast("Disconnect error: ${e.message}") } }
            runOnUiThread { connected = false; renderStatus() }
        }.start()
    }

    private fun applyAppSelection(source: String, apps: Set<String>): String {
        val lines = source.lines().filterNot {
            val t = it.trim()
            t.startsWith("IncludedApplications", true) || t.startsWith("ExcludedApplications", true)
        }.toMutableList()
        if (apps.isEmpty()) return lines.joinToString("\n")
        val interfaceIndex = lines.indexOfFirst { it.trim().equals("[Interface]", true) }
        require(interfaceIndex >= 0) { "Missing [Interface] section" }
        var insertAt = interfaceIndex + 1
        while (insertAt < lines.size && !lines[insertAt].trim().startsWith("[")) insertAt++
        lines.add(insertAt, "IncludedApplications = ${apps.sorted().joinToString(", ")}")
        return lines.joinToString("\n")
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        if (connected) disconnect()
        super.onDestroy()
    }
}
