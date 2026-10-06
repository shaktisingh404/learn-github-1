package com.brainpal.counter

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Counts one reel/short each time the vertical pager in Instagram Reels or
 * YouTube Shorts settles on a new item, and mirrors the counts to the notch.
 *
 * View ids come from the apps' own layouts and can change between releases;
 * keep them in the lists below if a count stops working.
 */
class ReelCounterService : AccessibilityService() {
    private lateinit var store: Store
    private lateinit var overlay: NotchOverlay
    private var lastCountAt = 0L

    override fun onServiceConnected() {
        store = Store(this)
        overlay = NotchOverlay(this)
        instance = this
    }

    // An uncaught exception here makes Android switch the service off, so never let one escape.
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            handleEvent(event)
        } catch (t: Throwable) {
            Log.e("BrainPal", "event failed", t)
        }
    }

    private fun handleEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString()
        val source = when (pkg) {
            IG -> Source.INSTAGRAM
            YT -> Source.YOUTUBE
            else -> {
                // Another app/launcher took the foreground: drop the pill.
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                    pkg != packageName && pkg != "com.android.systemui"
                ) overlay.hide()
                return
            }
        }
        val root = rootInActiveWindow ?: return
        try {
            if (!isReelScreen(root, source)) {
                overlay.hide()
                return
            }
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                val now = System.currentTimeMillis()
                if (now - lastCountAt > DEBOUNCE_MS) {
                    lastCountAt = now
                    store.increment(source)
                }
            }
            refreshOverlay()
        } finally {
            root.recycle()
        }
    }

    private fun isReelScreen(root: AccessibilityNodeInfo, source: Source): Boolean {
        val ids = if (source == Source.INSTAGRAM) IG_IDS else YT_IDS
        return ids.any { root.findAccessibilityNodeInfosByViewId("${root.packageName}:id/$it").isNotEmpty() }
    }

    private fun refreshOverlay() {
        if (store.notchEnabled) overlay.show(store.get(Source.INSTAGRAM), store.get(Source.YOUTUBE))
        else overlay.hide()
    }

    override fun onInterrupt() = overlay.hide()

    override fun onDestroy() {
        if (::overlay.isInitialized) overlay.hide()
        instance = null
        super.onDestroy()
    }

    companion object {
        private const val IG = "com.instagram.android"
        private const val YT = "com.google.android.youtube"
        private const val DEBOUNCE_MS = 700L
        private val IG_IDS = listOf("clips_viewer_view_pager", "root_clips_layout", "clips_video_container")
        private val YT_IDS = listOf("reel_recycler", "reel_player_page_container", "shorts_player_container")

        var instance: ReelCounterService? = null
            private set
    }
}
