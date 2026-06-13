package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          WebViewApp(
            url = "https://higauravnb.blogspot.com",
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          )
        }
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewApp(url: String, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var webView by remember { mutableStateOf<WebView?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var progress by remember { mutableStateOf(0) }
  var hasError by remember { mutableStateOf(false) }

  // Detect internet connection initially
  val isInitialConnected = remember(context) { isNetworkAvailable(context) }
  var isOffline by remember { mutableStateOf(!isInitialConnected) }

  // Clean back-navigation handling for the WebView history
  BackHandler(enabled = webView != null && webView!!.canGoBack()) {
    webView?.goBack()
  }

  Box(modifier = modifier.fillMaxSize()) {
    if (isOffline || hasError) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning Icon",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "No Connection",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Unable to load the blog because there is no active internet connection. Please check your settings and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = {
              if (isNetworkAvailable(context)) {
                isOffline = false
                hasError = false
                webView?.reload()
              }
            },
            modifier = Modifier.testTag("submit_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Retry"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
          }
        }
      }
    } else {
      AndroidView(
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              databaseEnabled = true
              useWideViewPort = true
              loadWithOverviewMode = true
              builtInZoomControls = true
              displayZoomControls = false
              setSupportZoom(true)
              cacheMode = WebSettings.LOAD_DEFAULT
              allowFileAccess = true
              allowContentAccess = true
            }
            webViewClient = object : WebViewClient() {
              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isLoading = true
                hasError = false
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false
              }

              override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
              ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                  hasError = true
                  isLoading = false
                }
              }
            }
            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progress = newProgress
                if (newProgress >= 100) {
                  isLoading = false
                }
              }
            }
            loadUrl(url)
            webView = this
          }
        },
        update = {
          // No dynamic attribute updates needed, WebView processes loads internally
        },
        modifier = Modifier.fillMaxSize()
      )

      AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
      ) {
        LinearProgressIndicator(
          progress = progress / 100f,
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp),
          color = MaterialTheme.colorScheme.primary,
          trackColor = Color.Transparent
        )
      }
    }
  }
}

fun isNetworkAvailable(context: Context): Boolean {
  val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  val network = connectivityManager.activeNetwork ?: return false
  val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
  return when {
    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
    else -> false
  }
}
