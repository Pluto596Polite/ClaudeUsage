package com.adriaan.claudeusage.ui.screen

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(onLoginSuccess: (cookies: String) -> Unit) {
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    // Guard so the success callback fires exactly once.
    val triggered = remember { mutableStateOf(false) }

    // claude.ai is a single-page app: after login it routes client-side without a full page
    // load, so URL-based detection is unreliable. Instead we detect the auth cookie (sessionKey),
    // which is exactly what the /usage API call needs. We're logged in once it appears while on
    // claude.ai (and not on a third-party OAuth page).
    fun maybeFinish(url: String?) {
        if (triggered.value) return
        val u = (url ?: "").lowercase()
        if (u.contains("accounts.google") || u.contains("appleid.apple")) return
        val cookies = CookieManager.getInstance().getCookie("https://claude.ai") ?: ""
        if (cookies.contains("sessionKey=")) {
            triggered.value = true
            CookieManager.getInstance().flush()
            onLoginSuccess(cookies)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Sign in to Claude",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (isLoading) {
            LinearProgressIndicator(
                progress = { pageProgress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportZoom(false)
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    val webView = this
                    // Poll for the session cookie — covers SPA route changes and async login
                    // completion that never trigger onPageFinished.
                    val handler = Handler(Looper.getMainLooper())
                    val poll = object : Runnable {
                        override fun run() {
                            maybeFinish(webView.url)
                            if (!triggered.value) handler.postDelayed(this, 1000)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            isLoading = false
                            maybeFinish(url)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            pageProgress = newProgress / 100f
                            isLoading = newProgress < 100
                        }
                    }

                    loadUrl("https://claude.ai/login")
                    handler.postDelayed(poll, 1500)
                }
            }
        )
    }
}
