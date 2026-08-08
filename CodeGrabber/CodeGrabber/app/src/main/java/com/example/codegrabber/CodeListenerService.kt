package com.example.codegrabber

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/**
 * Watches for new WhatsApp notifications and, if the text contains a redeem code
 * matching the configured pattern, copies just the code to the clipboard and
 * fires a small confirmation notification.
 *
 * Only ever reads notifications already visible on this device (yours) —
 * it does not read anyone else's messages or send anything anywhere.
 */
class CodeListenerService : NotificationListenerService() {

    companion object {
        // WhatsApp's notification package. WhatsApp Business uses
        // "com.whatsapp.w4b" if you need to support that too.
        private const val WHATSAPP_PACKAGE = "com.whatsapp"

        private const val CHANNEL_ID = "code_grabber_channel"
        private const val CHANNEL_NAME = "Code Grabber"

        // Matches things like "6KED| N5VW| JKD5" (3-6 alnum chars, groups joined by "|")
        private val PIPE_CODE_REGEX =
            Regex("""[A-Z0-9]{3,6}(?:\s*\|\s*[A-Z0-9]{3,6}){1,5}""")

        // Matches an explicit "CODE:" label followed by the code on the same or next line
        private val LABELED_CODE_REGEX =
            Regex("""CODE\s*:\s*\n?\s*([A-Z0-9|\s\-]{6,40})""", RegexOption.IGNORE_CASE)

        // Matches an already-joined plain code like "6KEDN5VWJKD5" (no separators),
        // roughly 9-18 uppercase-alnum chars with at least one letter and one digit.
        private val PLAIN_CODE_REGEX =
            Regex("""\b(?=[A-Z0-9]{9,18}\b)(?=[A-Z0-9]*[A-Z])(?=[A-Z0-9]*[0-9])[A-Z0-9]{9,18}\b""")
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != WHATSAPP_PACKAGE) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        // Polls and multi-line/group notifications populate EXTRA_TEXT_LINES instead
        // of (or in addition to) EXTRA_TEXT, so pull those in too.
        val lines = extras.getCharSequenceArray(android.app.Notification.EXTRA_TEXT_LINES)
            ?.joinToString("\n") { it.toString() } ?: ""

        val fullText = listOf(title, text, bigText, lines).joinToString("\n") { it }

        val code = extractCode(fullText) ?: return
        copyToClipboard(code)
        showConfirmation(code)
        launchRedirectCountdown(code)
    }

    private fun launchRedirectCountdown(code: String) {
        val intent = Intent(this, RedirectCountdownActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(RedirectCountdownActivity.EXTRA_CODE, code)
            putExtra(RedirectCountdownActivity.EXTRA_URL, "https://reward.ff.garena.com/en")
        }
        startActivity(intent)
    }

    private fun extractCode(source: String): String? {
        val upper = source.uppercase()

        // "CODE:" label followed by a code (may itself contain "|" separators)
        LABELED_CODE_REGEX.find(upper)?.let {
            return normalize(it.groupValues[1])
        }

        // Pipe-separated groups like "6KED| N5VW| JKD5" -> joined "6KEDN5VWJKD5"
        PIPE_CODE_REGEX.find(upper)?.let {
            return normalize(it.value)
        }

        // Already-joined plain code like "6KEDN5VWJKD5"
        PLAIN_CODE_REGEX.find(upper)?.let {
            return normalize(it.value)
        }

        return null
    }

    /** Strips "|" and whitespace so "6KED| N5VW| JKD5" and "6KEDN5VWJKD5" both end up identical. */
    private fun normalize(raw: String): String =
        raw.replace("|", "").replace(Regex("""\s+"""), "").trim()

    private fun copyToClipboard(code: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Redeem code", code))
    }

    private fun showConfirmation(code: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("Code copied")
            .setContentText(code)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, notification)
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
