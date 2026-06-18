package com.adriaan.claudeusage.ui.screen

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            val url = request.url.toString()
                            // Detect successful login: redirected to main chat or home
                            if (isLoggedInUrl(url)) {
                                view.postDelayed({
                                    val cookies = CookieManager.getInstance()
                                        .getCookie("https://claude.ai") ?: ""
                                    if (cookies.isNotEmpty()) {
                                        onLoginSuccess(cookies)
                                    }
                                }, 1500)
                            }
                            return false
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            isLoading = false
                            if (url != null && isLoggedInUrl(url)) {
                                view.postDelayed({
                                    val cookies = CookieManager.getInstance()
                                        .getCookie("https://claude.ai") ?: ""
                                    if (cookies.isNotEmpty()) {
                                        onLoginSuccess(cookies)
                                    }
                                }, 1000)
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            pageProgress = newProgress / 100f
                            isLoading = newProgress < 100
                        }
                    }

                    loadUrl("https://claude.ai/login")
                }
            }
        )
    }
}

private fun isLoggedInUrl(url: String): Boolean {
    val lower = url.lowercase()
    return (lower.contains("claude.ai") || lower.contains("claude.ai/new") ||
            lower.contains("claude.ai/chats") || lower.contains("claude.ai/projects")) &&
            !lower.contains("/login") &&
            !lower.contains("/auth") &&
            !lower.contains("accounts.google") &&
            !lower.contains("appleid.apple")
}
