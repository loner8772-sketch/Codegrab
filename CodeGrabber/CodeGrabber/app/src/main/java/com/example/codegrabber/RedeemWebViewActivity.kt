package com.example.codegrabber

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

/**
 * Loads the redeem site in a WebView and injects JS to auto-fill the code
 * into the redeem input field once the page finishes loading.
 *
 * IMPORTANT: reward.ff.garena.com is a JS-rendered page, so the exact input
 * selector can change with site updates. The JS below tries several common
 * patterns. If it doesn't find the field, open the site once in Chrome,
 * right-click the code input -> Inspect, and update the selectors array
 * below with the exact id/class you see.
 */
class RedeemWebViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CODE = "extra_code"
        const val EXTRA_URL = "extra_url"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val code = intent.getStringExtra(EXTRA_CODE) ?: ""
        val url = intent.getStringExtra(EXTRA_URL) ?: "https://reward.ff.garena.com/en"

        val webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, finishedUrl: String) {
                super.onPageFinished(view, finishedUrl)
                injectCode(view, code)
            }
        }

        webView.loadUrl(url)
    }

    private fun injectCode(webView: WebView, code: String) {
        // Tries a list of likely selectors for the redeem code input field.
        // Update this list if you inspect the real field and it differs.
        val js = """
            (function() {
                var code = ${JSONObject.quote(code)};
                var selectors = [
                    'input[placeholder*="code" i]',
                    'input[name*="code" i]',
                    'input[id*="code" i]',
                    'input[class*="code" i]',
                    'input[type="text"]'
                ];
                var field = null;
                for (var i = 0; i < selectors.length; i++) {
                    field = document.querySelector(selectors[i]);
                    if (field) break;
                }
                if (field) {
                    field.focus();
                    var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
                    setter.call(field, code);
                    field.dispatchEvent(new Event('input', { bubbles: true }));
                    field.dispatchEvent(new Event('change', { bubbles: true }));
                }
            })();
        """.trimIndent()

        // Retry a couple of times in case the SPA renders the field slightly
        // after onPageFinished fires.
        webView.postDelayed({ webView.evaluateJavascript(js, null) }, 500)
        webView.postDelayed({ webView.evaluateJavascript(js, null) }, 1500)
        webView.postDelayed({ webView.evaluateJavascript(js, null) }, 3000)
    }
}
