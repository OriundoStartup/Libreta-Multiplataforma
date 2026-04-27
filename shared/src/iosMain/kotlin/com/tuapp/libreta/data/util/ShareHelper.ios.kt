package com.tuapp.libreta.data.util

import platform.UIKit.UIActivityViewController

actual object ShareHelper {
    actual fun shareText(text: String) {
        val rootViewController = platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController
        val activity = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null
        )
        rootViewController?.presentViewController(activity, animated = true, completion = null)
    }
}
