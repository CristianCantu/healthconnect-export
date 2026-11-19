package com.healthconnect.export.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.healthconnect.export.BuildConfig
import com.healthconnect.export.R

/**
 * Google Cloud project used for sign-in.
 *
 * The release build ships with the maintainer's project, whose Drive scope cannot
 * be granted to arbitrary users. This card lets a user point the installed APK at
 * their own project — the client ID is the only project-specific value the app
 * itself needs — and shows the package name and signing SHA-1 that have to be
 * registered as an Android OAuth client for it.
 */
@Composable
fun GoogleClientIdCard(
    clientId: String,
    hasCustomClientId: Boolean,
    hasError: Boolean,
    signingSha1: String?,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(hasCustomClientId || hasError) }
    var draft by remember(clientId, hasCustomClientId) { mutableStateOf(clientId) }
    val clipboard = LocalClipboardManager.current

    MaterialCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = if (hasCustomClientId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.google_client_id_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        stringResource(
                            if (expanded) R.string.google_client_id_hide else R.string.google_client_id_configure,
                        ),
                    )
                }
            }

            Text(
                text =
                    stringResource(
                        if (hasCustomClientId) {
                            R.string.google_client_id_status_custom
                        } else {
                            R.string.google_client_id_status_default
                        },
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = stringResource(R.string.google_client_id_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    SignatureRow(
                        label = stringResource(R.string.google_client_id_package),
                        value = BuildConfig.APPLICATION_ID,
                        onCopy = { clipboard.setText(AnnotatedString(BuildConfig.APPLICATION_ID)) },
                    )
                    signingSha1?.let { sha1 ->
                        SignatureRow(
                            label = stringResource(R.string.google_client_id_sha1),
                            value = sha1,
                            onCopy = { clipboard.setText(AnnotatedString(sha1)) },
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text(stringResource(R.string.google_client_id_field)) },
                        placeholder = {
                            Text(stringResource(R.string.google_client_id_placeholder))
                        },
                        singleLine = true,
                        isError = hasError,
                        supportingText =
                            if (hasError) {
                                { Text(stringResource(R.string.google_client_id_invalid)) }
                            } else {
                                null
                            },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onReset, enabled = hasCustomClientId) {
                            Text(stringResource(R.string.google_client_id_reset))
                        }
                        TextButton(onClick = { onSave(draft) }) {
                            Text(stringResource(R.string.google_client_id_save))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignatureRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.google_client_id_copy),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
