package com.zennature.wallpaper.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Wraps an action that writes to shared storage. On API 29+ MediaStore needs no permission; on 24-28 the
 * legacy path needs WRITE_EXTERNAL_STORAGE, which is requested once here before running [action].
 */
@Composable
fun rememberStorageGate(): (action: () -> Unit) -> Unit {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<() -> Unit>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pending[0]
        pending[0] = null
        if (granted) action?.invoke()
        else Toast.makeText(context, "Storage permission is needed to save files on this Android version.", Toast.LENGTH_LONG).show()
    }
    return remember(context) {
        { action: () -> Unit ->
            val needs = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            if (needs) {
                pending[0] = action
                launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                action()
            }
        }
    }
}

fun toast(context: android.content.Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
