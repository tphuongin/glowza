package com.sgroupmobile.glowza.helper

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.sgroupmobile.glowza.R
class PermissionHelper(
    private val activity: AppCompatActivity
) {
    private var onGranted: (() -> Unit)? = null
    private var denyLayoutRes: Int = 0
    private var currentPermission: String = ""

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onGranted?.invoke()
        } else {
            handleDeny()
        }
    }

    fun requestPermission(
        permission: String,
        layoutRes: Int,
        onGranted: () -> Unit,
    ) {
        this.onGranted = onGranted
        this.currentPermission = permission
        this.denyLayoutRes = layoutRes

        when {
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED -> {
                onGranted.invoke()
            }
            else -> launcher.launch(permission)
        }
    }

    private fun handleDeny() {
        if (denyLayoutRes == 0) {
            return
        }
        val view = LayoutInflater.from(activity).inflate(denyLayoutRes, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(view)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<MaterialButton>(R.id.btn_setting)?.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
            dialog.dismiss()
        }

        view.findViewById< MaterialButton>(R.id.btn_cancel)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}