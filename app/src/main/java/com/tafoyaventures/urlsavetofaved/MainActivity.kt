package com.tafoyaventures.urlsavetofaved

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var incomingUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FavedSessionStore.init(applicationContext)
        ThemePreferenceStore.init(applicationContext)
        incomingUrl.value = extractUrl(intent)

        setContent {
            var themeMode by remember { mutableStateOf(ThemePreferenceStore.mode) }

            AppTheme(themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                        RelayScreen(
                            initialUrl = incomingUrl.value,
                            onShare = { title, description, url ->
                                shareResult(title, description, url)
                            }
                        )
                        IconButton(
                            onClick = {
                                themeMode = themeMode.next()
                                ThemePreferenceStore.mode = themeMode
                            },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Text(themeMode.glyph())
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUrl.value = extractUrl(intent)
    }

    private fun extractUrl(intent: Intent): String? {
        if (intent.action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim() ?: return null

        val url = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
            .find(text)?.value
            ?.trimEnd('.', ',', ';', ')', ']', '}')

        return url
    }

    private fun shareResult(title: String, description: String, url: String) {
        val body = buildString {
            if (title.isNotBlank()) append(title.trim())
            if (description.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append(description.trim())
            }
            if (isNotBlank()) append("\n\n")
            append(url.trim())
        }

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, body)
            clipData = android.content.ClipData.newPlainText("URL", body)
        }

        startActivity(Intent.createChooser(send, "Share link"))
    }
}

data class PageMetadata(
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val canonicalUrl: String? = null
)

@OptIn(ExperimentalLayoutApi::class)
@androidx.compose.runtime.Composable
private fun RelayScreen(
    initialUrl: String?,
    onShare: (String, String, String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl ?: "") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    var showLoginDialog by remember { mutableStateOf(false) }
    var loginLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    var showTagPicker by remember { mutableStateOf(false) }
    var tags by remember { mutableStateOf<List<FavedTag>>(emptyList()) }
    var selectedTagIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var tagPickerLoading by remember { mutableStateOf(false) }
    var tagPickerError by remember { mutableStateOf<String?>(null) }

    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagLoading by remember { mutableStateOf(false) }
    var newTagError by remember { mutableStateOf<String?>(null) }

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun loadTagsAndOpenPicker() {
        tagPickerError = null
        tagPickerLoading = true
        showTagPicker = true
        scope.launch {
            try {
                tags = withContext(Dispatchers.IO) { FavedApiClient.getTags() }
            } catch (e: Exception) {
                tagPickerError = e.message ?: "Failed to load tags."
            } finally {
                tagPickerLoading = false
            }
        }
    }

    fun openSaveFlow() {
        saveMessage = null
        if (!FavedSessionStore.isConfigured()) {
            loginError = null
            showLoginDialog = true
        } else {
            loadTagsAndOpenPicker()
        }
    }

    fun submitLogin(config: FavedConfig) {
        loginLoading = true
        loginError = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { FavedApiClient.login(config) }
                showLoginDialog = false
                loadTagsAndOpenPicker()
            } catch (e: Exception) {
                loginError = e.message ?: "Sign in failed."
            } finally {
                loginLoading = false
            }
        }
    }

    fun submitNewTag(name: String, parentId: Int?) {
        newTagLoading = true
        newTagError = null
        scope.launch {
            try {
                val parent = tags.firstOrNull { it.id == parentId }
                val tag = withContext(Dispatchers.IO) {
                    FavedApiClient.createTag(name, parentId, parent?.name)
                }.copy(depth = (parent?.depth ?: -1) + 1)
                tags = tags + tag
                selectedTagIds = selectedTagIds + tag.id
                showNewTagDialog = false
            } catch (e: Exception) {
                newTagError = e.message ?: "Failed to create tag."
            } finally {
                newTagLoading = false
            }
        }
    }

    fun submitItem() {
        tagPickerError = null
        tagPickerLoading = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    FavedApiClient.createItem(title, url, description, imageUrl, selectedTagIds.toList())
                }
                showTagPicker = false
                selectedTagIds = emptySet()
                saveMessage = "Saved to Faved."
            } catch (e: Exception) {
                tagPickerError = e.message ?: "Save failed."
            } finally {
                tagPickerLoading = false
            }
        }
    }

    suspend fun loadMetadata() {
        val normalized = normalizeUrl(url)
        if (normalized == null) {
            error = "Enter a valid http:// or https:// URL."
            return
        }

        url = normalized
        loading = true
        error = null

        try {
            val metadata = withContext(Dispatchers.IO) {
                MetadataFetcher.fetch(normalized)
            }
            title = metadata.title
            description = metadata.description
            imageUrl = metadata.imageUrl
            metadata.canonicalUrl?.let { url = it }
        } catch (e: Exception) {
            error = e.message ?: "Unable to load page metadata."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            loadMetadata()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("URL Save to Faved", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Receive a link from another app, fetch its metadata, then share it again.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                enabled = !loading && url.isNotBlank(),
                onClick = { scope.launch { loadMetadata() } }
            ) {
                Text("Fetch metadata")
            }
            Button(
                enabled = !loading && url.isNotBlank() && title.isNotBlank(),
                onClick = { openSaveFlow() }
            ) {
                Text("Save to Faved")
            }
            Button(
                enabled = !loading && url.isNotBlank(),
                onClick = {
                    val normalized = normalizeUrl(url)
                    if (normalized != null) onShare(title, description, normalized)
                    else error = "Enter a valid URL."
                }
            ) {
                Text("Share again")
            }
        }

        if (loading) {
            CircularProgressIndicator()
            Text("Fetching page metadata…")
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        saveMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        if (title.isNotBlank()) {
            Text("Preview", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            imageUrl?.let {
                Text("Image: $it", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
            Text(url, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showLoginDialog) {
        FavedLoginDialog(
            initialServerUrl = FavedSessionStore.serverUrl ?: "",
            initialUsername = FavedSessionStore.username ?: "",
            loading = loginLoading,
            error = loginError,
            onSubmit = { submitLogin(it) },
            onDismiss = { showLoginDialog = false }
        )
    }

    if (showTagPicker) {
        TagPickerDialog(
            tags = tags,
            selectedIds = selectedTagIds,
            loading = tagPickerLoading,
            error = tagPickerError,
            onToggle = { id ->
                selectedTagIds = if (id in selectedTagIds) selectedTagIds - id else selectedTagIds + id
            },
            onCreateNewTag = { showNewTagDialog = true },
            onConfirm = { submitItem() },
            onDismiss = { showTagPicker = false }
        )
    }

    if (showNewTagDialog) {
        NewTagDialog(
            parentCandidates = tags,
            loading = newTagLoading,
            error = newTagError,
            onCreate = { name, parentId -> submitNewTag(name, parentId) },
            onDismiss = { showNewTagDialog = false }
        )
    }
}

private fun normalizeUrl(value: String): String? {
    val candidate = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
        .find(value.trim())?.value
        ?.trimEnd('.', ',', ';', ')', ']', '}')
        ?: return null

    return try {
        val uri = Uri.parse(candidate)
        if (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
            if (!uri.host.isNullOrBlank()) candidate else null
        } else null
    } catch (_: Exception) {
        null
    }
}
