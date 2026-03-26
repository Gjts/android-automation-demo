package com.example.automationapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutomationService : AccessibilityService() {

    companion object {
        const val TAG = "AutomationService"
        const val TARGET_PACKAGE = "com.example.targetapp"

        @Volatile
        var isArmed = false
            set(value) {
                field = value
                if (value) {
                    // Reset step when re-arming so repeated runs work
                    currentStepStatic = Step.IDLE
                    Log.i(TAG, "Armed! Step reset to IDLE")
                }
            }

        // Use static step so it persists across service lifecycle
        // and can be reset from MainActivity
        @Volatile
        private var currentStepStatic = Step.IDLE
    }

    enum class Step {
        IDLE, LOGIN, DECISION, PIN, DONE
    }

    private val handler = Handler(Looper.getMainLooper())
    private var retryRunnable: Runnable? = null
    private var progressWatchdog: Runnable? = null
    private var lastProcessedPage: String? = null
    private var lastActionAt: Long = 0L

    private var currentStep: Step
        get() = currentStepStatic
        set(value) { currentStepStatic = value }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!isArmed) return
        if (currentStep == Step.DONE) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName != TARGET_PACKAGE) return

        Log.d(TAG, "Event: type=${event.eventType}, class=${event.className}, text=${event.text}")

        // Schedule processing with a delay to let UI settle
        scheduleProcess()
    }

    private fun scheduleProcess(delayMs: Long = 350L) {
        retryRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable {
            try {
                processCurrentState()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing state: ${e.message}", e)
            }
        }
        retryRunnable = runnable
        handler.postDelayed(runnable, delayMs)
    }

    private fun scheduleProgressWatchdog() {
        progressWatchdog?.let { handler.removeCallbacks(it) }
        val watchdog = Runnable {
            if (!isArmed || currentStep == Step.DONE) return@Runnable
            val elapsed = System.currentTimeMillis() - lastActionAt
            if (elapsed >= 1500L) {
                Log.d(TAG, "Watchdog retry triggered for step=$currentStep after ${elapsed}ms")
                scheduleProcess(0L)
            } else {
                scheduleProgressWatchdog()
            }
        }
        progressWatchdog = watchdog
        handler.postDelayed(watchdog, 1600L)
    }

    private fun markAction(page: String) {
        lastProcessedPage = page
        lastActionAt = System.currentTimeMillis()
        scheduleProgressWatchdog()
    }

    private fun processCurrentState() {
        val rootNode = rootInActiveWindow ?: run {
            Log.w(TAG, "rootInActiveWindow is null, scheduling retry")
            scheduleProcess(600L)
            return
        }

        try {
            Log.d(TAG, "Processing state: $currentStep, window=${rootNode.packageName}, lastPage=$lastProcessedPage")
            when (currentStep) {
                Step.IDLE -> handleLogin(rootNode)
                Step.LOGIN -> handleDecision(rootNode)
                Step.DECISION -> handlePin(rootNode)
                Step.PIN -> checkComplete(rootNode)
                Step.DONE -> { /* no-op */ }
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun handleLogin(root: AccessibilityNodeInfo) {
        logNodeTree(root, "login-root")
        val onLoginPage = hasNodeWithText(root, "Login Page") ||
            hasNodeWithDescription(root, "Login Page") ||
            hasNodeWithText(root, "Password") ||
            hasNodeWithDescription(root, "Password") ||
            hasNodeWithText(root, "Login") ||
            hasNodeWithDescription(root, "Login") ||
            looksLikeLoginPage(root)
        if (!onLoginPage) {
            Log.d(TAG, "Not on Login Page yet")
            scheduleProcess(700L)
            return
        }

        Log.d(TAG, "=== Detected Login Page ===")
        markAction("login")

        val editNode = findBestInputNode(root, listOf("Password"))
        if (editNode == null) {
            Log.w(TAG, "No editable node found on Login Page")
            scheduleProcess(700L)
            return
        }

        val textSet = setNodeText(editNode, "Test@2026")
        Log.d(TAG, "Set password text: $textSet")

        if (!textSet) {
            scheduleProcess(700L)
            return
        }

        handler.postDelayed({
            try {
                val freshRoot = rootInActiveWindow ?: return@postDelayed
                try {
                    val clicked = clickNodeWithExactText(freshRoot, "Login")
                    Log.d(TAG, "Clicked Login button: $clicked")
                    if (clicked) {
                        currentStep = Step.LOGIN
                        markAction("login-clicked")
                        Log.d(TAG, "Step -> LOGIN (waiting for Decision Page)")
                        scheduleProcess(900L)
                    } else {
                        scheduleProcess(700L)
                    }
                } finally {
                    freshRoot.recycle()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clicking Login", e)
                scheduleProcess(700L)
            }
        }, 700)
    }

    private fun handleDecision(root: AccessibilityNodeInfo) {
        logNodeTree(root, "decision-root")
        val onDecisionPage = hasNodeWithText(root, "Decision Page") ||
            hasNodeWithDescription(root, "Decision Page") ||
            hasNodeWithText(root, "Test text") ||
            hasNodeWithDescription(root, "Test text") ||
            hasNodeWithText(root, "Not test text") ||
            hasNodeWithDescription(root, "Not test text") ||
            hasNodeWithText(root, "test text 1") ||
            hasNodeWithDescription(root, "test text 1") ||
            looksLikeDecisionPage(root)
        if (!onDecisionPage) {
            Log.d(TAG, "Not on Decision Page yet")
            scheduleProcess(700L)
            return
        }

        Log.d(TAG, "=== Detected Decision Page ===")
        markAction("decision")

        val hasTestText1 = hasNodeWithText(root, "test text 1") ||
            hasNodeWithDescription(root, "test text 1")

        Log.d(TAG, "Decision branch probe: hasTestText1=$hasTestText1")

        if (hasTestText1) {
            Log.d(TAG, "Found 'test text 1' -> clicking 'Test text'")
            val clicked = clickNodeWithExactText(root, "Test text")
            Log.d(TAG, "Clicked 'Test text': $clicked")
            if (clicked) {
                currentStep = Step.DECISION
                markAction("decision-test")
                Log.d(TAG, "Step -> DECISION (waiting for PIN Page)")
                scheduleProcess(900L)
            } else {
                scheduleProcess(700L)
            }
        } else {
            Log.d(TAG, "'test text 1' NOT found in text/description -> clicking 'Not test text'")
            val clicked = clickNodeWithExactText(root, "Not test text")
            Log.d(TAG, "Clicked 'Not test text': $clicked")
            if (clicked) {
                currentStep = Step.DONE
                isArmed = false
                Log.d(TAG, "Step -> DONE (exiting via Not test text)")
            } else {
                scheduleProcess(700L)
            }
        }
    }

    private fun handlePin(root: AccessibilityNodeInfo) {
        logNodeTree(root, "pin-root")
        val onPinPage = hasNodeWithText(root, "PIN Page") ||
            hasNodeWithDescription(root, "PIN Page") ||
            hasNodeWithText(root, "PIN") ||
            hasNodeWithDescription(root, "PIN") ||
            hasNodeWithText(root, "Automation complete") ||
            hasNodeWithDescription(root, "Automation complete") ||
            looksLikePinPage(root)
        if (!onPinPage) {
            Log.d(TAG, "Not on PIN Page yet")
            scheduleProcess(700L)
            return
        }

        Log.d(TAG, "=== Detected PIN Page ===")
        markAction("pin")

        val editNode = findBestInputNode(root, listOf("PIN"))
        if (editNode == null) {
            Log.w(TAG, "No editable node found on PIN Page")
            scheduleProcess(700L)
            return
        }

        val textSet = setNodeText(editNode, "8526")
        Log.d(TAG, "Set PIN text: $textSet")

        if (textSet) {
            currentStep = Step.PIN
            markAction("pin-filled")
            Log.d(TAG, "Step -> PIN (waiting for completion)")
            handler.postDelayed({
                try {
                    val freshRoot = rootInActiveWindow ?: return@postDelayed
                    try {
                        checkComplete(freshRoot)
                    } finally {
                        freshRoot.recycle()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking completion", e)
                }
            }, 900)
        } else {
            scheduleProcess(700L)
        }
    }

    private fun checkComplete(root: AccessibilityNodeInfo) {
        val isComplete = hasNodeWithText(root, "Automation complete") ||
            hasNodeWithDescription(root, "Automation complete")

        Log.d(TAG, "Completion probe: isComplete=$isComplete")

        if (isComplete) {
            Log.d(TAG, "=== Automation complete! ===")
            currentStep = Step.DONE
            isArmed = false
            retryRunnable?.let { handler.removeCallbacks(it) }
            progressWatchdog?.let { handler.removeCallbacks(it) }
        } else {
            Log.d(TAG, "Completion text/description not found yet, scheduling retry")
            scheduleProcess(700L)
        }
    }

    // --- Helper methods ---

    private fun hasNodeWithText(root: AccessibilityNodeInfo, text: String): Boolean {
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return !nodes.isNullOrEmpty()
    }

    private fun hasNodeWithDescription(root: AccessibilityNodeInfo, text: String): Boolean {
        if (root.contentDescription?.toString() == text) return true
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            if (hasNodeWithDescription(child, text)) return true
        }
        return false
    }

    private fun clickNodeWithExactText(root: AccessibilityNodeInfo, text: String): Boolean {
        val nodes = root.findAccessibilityNodeInfosByText(text) ?: emptyList()

        for (node in nodes) {
            val nodeText = node.text?.toString()
            if (nodeText == text) {
                if (performClick(node)) return true
            }
        }

        for (node in nodes) {
            val desc = node.contentDescription?.toString()
            if (desc == text) {
                if (performClick(node)) return true
            }
        }

        val descNode = findNodeByDescription(root, text)
        if (descNode != null && performClick(descNode)) {
            return true
        }

        Log.d(TAG, "No exact match for '$text', trying all ${nodes.size} text candidate nodes")
        for (node in nodes) {
            if (performClick(node)) return true
        }

        return false
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }

        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 10) {
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            parent = parent.parent
            depth++
        }

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            val x = bounds.centerX().toFloat()
            val y = bounds.centerY().toFloat()
            val clicked = dispatchGestureClick(x, y)
            if (clicked) {
                Log.d(TAG, "Gesture click succeeded at ($x, $y)")
                return true
            }
        }

        return false
    }

    private fun dispatchGestureClick(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.d(TAG, "Gesture click not supported on this Android version")
            return false
        }

        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()

        var result = false
        val latch = Object()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                synchronized(latch) {
                    result = true
                    latch.notifyAll()
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                synchronized(latch) {
                    result = false
                    latch.notifyAll()
                }
            }
        }, null)

        synchronized(latch) {
            try {
                latch.wait(300)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.w(TAG, "Interrupted while waiting for gesture result", e)
            }
        }

        return result
    }

    private fun setNodeText(node: AccessibilityNodeInfo, value: String): Boolean {
        val focused = if (node.isFocused) true else {
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS) ||
                node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
        }
        Log.d(TAG, "Input focus result: $focused")

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                value
            )
        }
        val setByAction = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (setByAction) return true

        return performPasteFallback(node, value)
    }

    private fun performPasteFallback(node: AccessibilityNodeInfo, value: String): Boolean {
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                value
            )
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findBestInputNode(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? {
        findEditableNode(root)?.let { return it }

        for (label in labels) {
            val labelNodes = root.findAccessibilityNodeInfosByText(label).orEmpty()
            for (labelNode in labelNodes) {
                findEditableInSubtree(labelNode)?.let { return it }

                var parent = labelNode.parent
                var depth = 0
                while (parent != null && depth < 6) {
                    findEditableInSubtree(parent)?.let { return it }
                    parent = parent.parent
                    depth++
                }
            }
        }

        return findFocusableInputNode(root) ?: findLikelyInputContainer(root)
    }

    private fun findEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable) return root

        val className = root.className?.toString() ?: ""
        if (className.contains("EditText")) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findEditableNode(child)
            if (result != null) return result
        }
        return null
    }

    private fun findEditableInSubtree(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        findEditableNode(root)?.let { return it }
        return null
    }

    private fun findFocusableInputNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val className = root.className?.toString() ?: ""
        if ((root.isFocusable || root.isFocused) &&
            (className.contains("EditText") || className.contains("TextField"))
        ) {
            return root
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findFocusableInputNode(child)
            if (result != null) return result
        }
        return null
    }

    private fun findLikelyInputContainer(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val className = root.className?.toString() ?: ""
        if ((root.isFocusable || root.isClickable) &&
            (className.contains("View") || className.contains("TextField"))
        ) {
            return root
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findLikelyInputContainer(child)
            if (result != null) return result
        }
        return null
    }

    private fun findNodeByDescription(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (root.contentDescription?.toString() == text) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findNodeByDescription(child, text)
            if (result != null) return result
        }
        return null
    }

    private fun countNodesWithText(root: AccessibilityNodeInfo, text: String): Int {
        return root.findAccessibilityNodeInfosByText(text)?.size ?: 0
    }

    private fun looksLikeLoginPage(root: AccessibilityNodeInfo): Boolean {
        val hasLogin = hasNodeWithText(root, "Login") || hasNodeWithDescription(root, "Login")
        val hasPassword = hasNodeWithText(root, "Password") || hasNodeWithDescription(root, "Password")
        val hasEditable = findEditableNode(root) != null || findFocusableInputNode(root) != null
        return hasLogin && (hasPassword || hasEditable)
    }

    private fun looksLikeDecisionPage(root: AccessibilityNodeInfo): Boolean {
        val actionButtons = (countNodesWithText(root, "Test text") > 0 || hasNodeWithDescription(root, "Test text")) &&
            (countNodesWithText(root, "Not test text") > 0 || hasNodeWithDescription(root, "Not test text"))
        return actionButtons ||
            hasNodeWithText(root, "test text 1") ||
            hasNodeWithDescription(root, "test text 1")
    }

    private fun looksLikePinPage(root: AccessibilityNodeInfo): Boolean {
        val hasPinLabel = hasNodeWithText(root, "PIN") || hasNodeWithDescription(root, "PIN")
        val hasInput = findEditableNode(root) != null ||
            findFocusableInputNode(root) != null ||
            findLikelyInputContainer(root) != null
        return hasPinLabel && hasInput
    }

    private fun logNodeTree(root: AccessibilityNodeInfo, label: String) {
        logNodeTreeRecursive(root, label, 0)
    }

    private fun logNodeTreeRecursive(node: AccessibilityNodeInfo?, label: String, depth: Int) {
        if (node == null || depth > 4) return

        val indent = " ".repeat(depth * 2)
        Log.d(
            TAG,
            "$label $indent class=${node.className} text=${node.text} desc=${node.contentDescription} clickable=${node.isClickable} focusable=${node.isFocusable} editable=${node.isEditable}"
        )

        for (i in 0 until node.childCount) {
            logNodeTreeRecursive(node.getChild(i), label, depth + 1)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AutomationService interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        lastActionAt = System.currentTimeMillis()
        Log.i(TAG, "AutomationService connected and ready")
    }

    override fun onDestroy() {
        super.onDestroy()
        retryRunnable?.let { handler.removeCallbacks(it) }
        progressWatchdog?.let { handler.removeCallbacks(it) }
        Log.i(TAG, "AutomationService destroyed")
    }
}
