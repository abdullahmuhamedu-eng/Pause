package ch.dulli.pause

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GuardService : AccessibilityService() {
    private lateinit var prefs: Prefs
    private var lastScan = 0L
    private var lastHit = 0L
    private var visited = 0

    override fun onServiceConnected() { prefs = Prefs(this) }
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (!::prefs.isInitialized) return
        val pkg = e.packageName?.toString() ?: return
        val now = SystemClock.uptimeMillis()
        if (now - lastHit < 2000) return

        val rules = Rules.all.filter { pkg in it.pkgs && prefs.on(it.key) }
        if (rules.isEmpty()) return

        rules.firstOrNull { it.whole }?.let { r ->
            if (!prefs.isUnlocked(r.key)) hit(r)
            return
        }

        if (now - lastScan < 400) return
        lastScan = now
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return
        for (r in rules) {
            visited = 0
            if (!prefs.isUnlocked(r.key) && matches(root, r, 0)) { hit(r); return }
        }
    }

    private fun matches(n: AccessibilityNodeInfo?, r: Rule, depth: Int): Boolean {
        if (n == null || depth > 45 || ++visited > 2500) return false
        val id = n.viewIdResourceName
        if (id != null && n.isVisibleToUser && r.ids.any { id.contains(it, true) }) return true
        if (r.tab != null && n.isSelected &&
            n.contentDescription?.toString()?.startsWith(r.tab, true) == true) return true
        for (i in 0 until n.childCount) if (matches(n.getChild(i), r, depth + 1)) return true
        return false
    }

    private fun hit(r: Rule) {
        lastHit = SystemClock.uptimeMillis()
        prefs.addBlock()
        performGlobalAction(if (r.whole) GLOBAL_ACTION_HOME else GLOBAL_ACTION_BACK)
        startActivity(
            Intent(this, GateActivity::class.java)
                .putExtra("key", r.key)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }
}
