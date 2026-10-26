package com.dakota.store

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DakotaStoreApp()
            }
        }
    }
}

class StoreViewModel : ViewModel() {
    private val _releases = MutableStateFlow<Map<String, GitHubRelease?>>(emptyMap())
    val releases: StateFlow<Map<String, GitHubRelease?>> = _releases

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            val map = mutableMapOf<String, GitHubRelease?>()
            for (repo in AppList.repos) {
                val key = "${repo.owner}/${repo.repo}"
                map[key] = GitHubApi.getLatestRelease(repo.owner, repo.repo)
            }
            _releases.value = map
            _loading.value = false
        }
    }
}

@Composable
fun DakotaStoreApp(viewModel: StoreViewModel = viewModel()) {
    val releases by viewModel.releases.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dakota Store") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Download, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (loading && releases.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(AppList.repos) { repo ->
                    val key = "${repo.owner}/${repo.repo}"
                    val release = releases[key]
                    AppCard(
                        repo = repo,
                        release = release,
                        onDownload = { asset ->
                            downloadAndInstall(context, asset.browserDownloadUrl, asset.name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppCard(
    repo: RepoConfig,
    release: GitHubRelease?,
    onDownload: (GitHubAsset) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(repo.displayName, style = MaterialTheme.typography.titleLarge)
            Text(repo.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            if (release == null) {
                Text("No release found", color = MaterialTheme.colorScheme.error)
            } else {
                Text("Latest: ${release.tagName}", style = MaterialTheme.typography.labelLarge)
                release.body?.takeIf { it.isNotBlank() }?.let {
                    Text(it.take(200) + if (it.length > 200) "…" else "", style = MaterialTheme.typography.bodySmall)
                }

                val apks = release.assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
                if (apks.isEmpty()) {
                    Text("No APK assets in this release", color = MaterialTheme.colorScheme.error)
                } else {
                    Spacer(Modifier.height(8.dp))
                    apks.forEach { asset ->
                        Button(
                            onClick = { onDownload(asset) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Install ${asset.name} (${asset.size / 1024 / 1024} MB)")
                        }
                    }
                }
            }
        }
    }
}

private fun downloadAndInstall(context: android.content.Context, url: String, fileName: String) {
    Toast.makeText(context, "Downloading $fileName…", Toast.LENGTH_SHORT).show()
    val vmScope = (context as? ComponentActivity)?.let { } // just for simplicity; use coroutine in real
    // For demo we do a simple blocking-ish approach via a thread; production would use ViewModel + progress
    Thread {
        try {
            val cacheDir = context.cacheDir
            val apkFile = File(cacheDir, fileName)
            val success = kotlinx.coroutines.runBlocking {
                GitHubApi.downloadApk(url, apkFile)
            }
            if (!success) {
                (context as android.app.Activity).runOnUiThread {
                    Toast.makeText(context, "Download failed", Toast.LENGTH_LONG).show()
                }
                return@Thread
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            (context as android.app.Activity).runOnUiThread {
                context.startActivity(intent)
                Toast.makeText(context, "Opening installer…", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            (context as android.app.Activity).runOnUiThread {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }.start()
}
