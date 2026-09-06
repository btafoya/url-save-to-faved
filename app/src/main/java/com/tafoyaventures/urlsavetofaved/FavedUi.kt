package com.tafoyaventures.urlsavetofaved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun FavedLoginDialog(
    initialServerUrl: String,
    initialUsername: String,
    loading: Boolean,
    error: String?,
    onSubmit: (FavedConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = !loading),
        title = { Text("Sign in to Faved") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    singleLine = true,
                    enabled = !loading
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    enabled = !loading
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !loading
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !loading && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                onClick = { onSubmit(FavedConfig(serverUrl, username, password)) }
            ) { Text(if (loading) "Signing in…" else "Sign in") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("Cancel") }
        }
    )
}

@Composable
fun TagPickerDialog(
    tags: List<FavedTag>,
    selectedIds: Set<Int>,
    loading: Boolean,
    error: String?,
    onToggle: (Int) -> Unit,
    onCreateNewTag: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign tags") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (tags.isEmpty() && !loading) {
                    Text("No tags yet.", style = MaterialTheme.typography.bodySmall)
                }
                tags.forEach { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = (tag.depth * 16).dp)
                    ) {
                        Checkbox(checked = tag.id in selectedIds, onCheckedChange = { onToggle(tag.id) })
                        Text(tag.name)
                    }
                }
                TextButton(onClick = onCreateNewTag, enabled = !loading) { Text("+ New tag") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = !loading, onClick = onConfirm) { Text(if (loading) "Saving…" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("Cancel") }
        }
    )
}

@Composable
fun NewTagDialog(
    parentCandidates: List<FavedTag>,
    loading: Boolean,
    error: String?,
    onCreate: (name: String, parentId: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<Int?>(null) }
    var parentMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    enabled = !loading
                )
                Box {
                    OutlinedButton(onClick = { parentMenuOpen = true }, enabled = !loading) {
                        Text(parentCandidates.firstOrNull { it.id == parentId }?.name ?: "No parent")
                    }
                    DropdownMenu(expanded = parentMenuOpen, onDismissRequest = { parentMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("No parent") },
                            onClick = { parentId = null; parentMenuOpen = false }
                        )
                        parentCandidates.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag.name) },
                                onClick = { parentId = tag.id; parentMenuOpen = false }
                            )
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !loading && name.isNotBlank(),
                onClick = { onCreate(name.trim(), parentId) }
            ) { Text(if (loading) "Creating…" else "Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("Cancel") }
        }
    )
}
