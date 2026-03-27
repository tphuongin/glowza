package com.sgroupmobile.glowza.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.sgroupmobile.glowza.R

class PermissionHelper(
    private val caller: ActivityResultCaller,
    private val context: Context
) {
    private var onGranted: (() -> Unit)? = null
    private var denyLayoutRes: Int = 0
    private var currentPermission: String = ""

    private val launcher = caller.registerForActivityResult(
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
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                onGranted.invoke()
            }
            else -> launcher.launch(permission)
        }
    }

    private fun handleDeny() {
        if (denyLayoutRes == 0) return

        val activity = context as? Activity ?: return

        // Kiểm tra xem có nên hiển thị giải thích không
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, currentPermission)

        val view = LayoutInflater.from(context).inflate(denyLayoutRes, null)
        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnAction = view.findViewById<MaterialButton>(R.id.btn_setting)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel)

        if (shouldShowRationale) {
            // TRƯỜNG HỢP 1: Từ chối lần 1
            btnAction?.text = context.getString(R.string.btn_retry) ?: "Thử lại"
            btnAction?.setOnClickListener {
                launcher.launch(currentPermission)
                dialog.dismiss()
            }
        } else {
            // TRƯỜNG HỢP 2: Từ chối vĩnh viễn - Phải vào Cài đặt
            btnAction?.text = context.getString(R.string.btn_setting) ?: "Cài đặt"
            btnAction?.setOnClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
                dialog.dismiss()
            }
        }

        btnCancel?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}