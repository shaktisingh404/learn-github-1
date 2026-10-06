package com.brainpal.counter

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Counts a reel/short when the vertical pager has stopped scrolling AND the
 * screen content is different from the last counted item. Counting after the
 * scroll settles (instead of per scroll event) avoids both dropped counts on
 * quick swipes and double counts on one long swipe.
 *
 * View ids come from the apps' own layouts and can change between releases;
 * keep them in the lists below if detection stops working.
 */
class ReelCounterService : AccessibilityService() {
    private lateinit var store: Store
    private lateinit var overlay: NotchOverlay
    private val handler = Handler(Looper.getMainLooper())

    private var activeSource: Source? = null // non-null while a reel screen is showing
    private var lastSignature: Int? = null
    private var lastCountAt = 0L
    private var missCount = 0

    private val settle = Runnable { safely { onScrollSettled() } }

    override fun onServiceConnected() {
        store = Store(this)
        overlay = NotchOverlay(this)
        instance = this
    }

    // An uncaught exception makes Android switch the service off, so never let one escape.
    private inline fun safely(block: () -> Unit) {
        try { block() } catch (t: Throwable) { Log.e("BrainPal", "failed", t) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) = safely { handleEvent(event) }

    private fun handleEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString()
        val source = when (pkg) {
            IG -> Source.INSTAGRAM
            YT -> Source.YOUTUBE
            else -> {
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                    pkg != packageName && pkg != "com.android.systemui"
                ) leaveReels()
                return
            }
        }

        val onReels = isReelEvent(event, source)
        if (onReels) {
            missCount = 0
            if (activeSource != source) enterReels(source)
        } else if (activeSource == source && ++missCount >= MISSES_BEFORE_LEAVE) {
            leaveReels()
            return
        }
        if (activeSource == null) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && onReels) {
            handler.removeCallbacks(settle)
            handler.postDelayed(settle, SETTLE_MS)
        }
    }

    /** True if the event's own view, its ancestors, or the whole window show a reel pager. */
    private fun isReelEvent(event: AccessibilityEvent, source: Source): Boolean {
        val ids = if (source == Source.INSTAGRAM) IG_IDS else YT_IDS
        var node: AccessibilityNodeInfo? = event.source
        var depth = 0
        while (node != null && depth < 12) {
            val id = node.viewIdResourceName?.substringAfter(":id/")
            if (id != null && id in ids) return true
            node = node.parent
            depth++
        }
        val root = rootInActiveWindow ?: return activeSource == source // transient null: keep state
        return ids.any { root.findAccessibilityNodeInfosByViewId("${root.packageName}:id/$it").isNotEmpty() }
    }

    private fun enterReels(source: Source) {
        activeSource = source
        lastSignature = null
        refreshOverlay()
    }

    private fun leaveReels() {
        handler.removeCallbacks(settle)
        activeSource = null
        lastSignature = null
        missCount = 0
        overlay.hide()
    }

    private fun onScrollSettled() {
        val source = activeSource ?: return
        val root = rootInActiveWindow ?: return
        val sig = signature(root)
        val now = System.currentTimeMillis()
        val changed = if (sig == null) now - lastCountAt > FALLBACK_GAP_MS else sig != lastSignature
        if (changed) {
            lastSignature = sig
            lastCountAt = now
            store.increment(source)
            refreshOverlay()
        }
    }

    /** Hash of all visible text on screen; differs from one reel to the next. Null if nothing readable. */
    private fun signature(root: AccessibilityNodeInfo): Int? {
        val sb = StringBuilder()
        var seen = 0
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null || seen > MAX_NODES) return
            seen++
            if (n.isVisibleToUser) {
                n.text?.let { sb.append(it).append('|') }
                n.contentDescription?.let { sb.append(it).append('|') }
            }
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(root)
        return if (sb.isEmpty()) null else sb.toString().hashCode()
    }

    private fun refreshOverlay() {
        if (store.notchEnabled) overlay.show(store.get(Source.INSTAGRAM), store.get(Source.YOUTUBE))
        else overlay.hide()
    }

    override fun onInterrupt() = leaveReels()

    override fun onDestroy() {
        handler.removeCallbacks(settle)
        if (::overlay.isInitialized) overlay.hide()
        instance = null
        super.onDestroy()
    }

    companion object {
        private const val IG = "com.instagram.android"
        private const val YT = "com.google.android.youtube"
        private const val SETTLE_MS = 450L
        private const val FALLBACK_GAP_MS = 600L
        private const val MAX_NODES = 400
        private const val MISSES_BEFORE_LEAVE = 3
        private val IG_IDS = setOf("clips_viewer_view_pager", "root_clips_layout", "clips_video_container")
        private val YT_IDS = setOf("reel_recycler", "reel_player_page_container", "shorts_player_container")

        var instance: ReelCounterService? = null
            private set
    }
}
