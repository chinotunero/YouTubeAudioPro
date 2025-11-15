package com.yourname.youtubeaudiopro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    
    private val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            YoutubeDL.getInstance().init(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al inicializar: ${e.message}", Toast.LENGTH_LONG).show()
        }
        
        requestPermissions()
        
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }

    private fun requestPermissions() {
        val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (!permissions.all { it.value }) {
                Toast.makeText(this, "Permisos necesarios para guardar archivos", Toast.LENGTH_LONG).show()
            }
        }
        launcher.launch(permissions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var url by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var status by remember { mutableStateOf("Esperando URL...") }
    var selectedQuality by remember { mutableStateOf("192") }
    var selectedFormat by remember { mutableStateOf("mp3") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "YouTubeAudioPro")
    if (!downloadDir.exists()) downloadDir.mkdirs()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube Audio Pro") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL de YouTube") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDownloading,
                singleLine = true
            )

            Text("Formato:", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("mp3" to "MP3", "m4a" to "M4A", "opus" to "OPUS", "wav" to "WAV").forEach { (value, label) ->
                    FilterChip(
                        selected = selectedFormat == value,
                        onClick = { selectedFormat = value },
                        label = { Text(label) },
                        enabled = !isDownloading
                    )
                }
            }

            Text("Calidad:", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("320" to "320k", "192" to "192k", "128" to "128k", "64" to "64k").forEach { (value, label) ->
                    FilterChip(
                        selected = selectedQuality == value,
                        onClick": { selectedQuality = value },
                        label": { Text(label) },
                        enabled": !isDownloading
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estado: $status")
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = {
                    if (url.isNotEmpty()) {
                        scope.launch {
                            isDownloading = true
                            progress = 0f
                            status = "Iniciando descarga..."
                            
                            val success = downloadAudio(
                                url = url,
                                format = selectedFormat,
                                quality = selectedQuality,
                                downloadDir = downloadDir,
                                onProgress = { p, s ->
                                    progress = p
                                    status = s
                                }
                            )
                            
                            isDownloading = false
                            status = if (success) "✅ Descarga completada" else "❌ Error"
                            if (success) url = ""
                            
                            Toast.makeText(
                                context,
                                if (success) "Descarga exitosa" else "Error en la descarga",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDownloading && url.isNotEmpty()
            ) {
                Text(if (isDownloading) "⏳ DESCARGANDO..." else "▶️ DESCARGAR AUDIO")
            }

            Text(
                text = "Guardado en: ${downloadDir.absolutePath}",
                style = MaterialTheme.typography.bodySmall",
                color = MaterialTheme.colorScheme.onSurfaceVariant",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

suspend fun downloadAudio(
    url: String,
    format": String,
    quality": String,
    downloadDir": File,
    onProgress": (Float, String) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    try {
        val request = YoutubeDLRequest(url).apply {
            addOption("-o", File(downloadDir, "%(title)s.%(ext)s").absolutePath)
            addOption("-f", "bestaudio/best")
            addOption("--extract-audio")
            addOption("--audio-format", format)
            addOption("--audio-quality", quality)
            addOption("--add-metadata")
            addOption("--embed-thumbnail")
            addOption("--no-playlist")
        }

        YoutubeDL.getInstance().execute(request) { progress, _, line ->
            val progressPercent = progress / 100f
            onProgress(progressPercent, line ?: "Procesando...")
        }

        true
    } catch (e: Exception) {
        e.printStackTrace()
        onProgress(0f, "Error: ${e.message}")
        false
    }
}