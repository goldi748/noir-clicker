package com.noir.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * The real "hands" of the app. Android only allows a tap or a text fill to
 * reach another app (a game, Instagram, WhatsApp...) through an
 * AccessibilityService that the user has explicitly turned on in
 * Settings > Accessibility. This class cannot act until that toggle is on.
 */
class ClickerAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ClickerAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    /** Dispatches a single tap at the given screen coordinates. */
    fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 1)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    /**
     * Sets [text] on whichever editable field currently has focus in the
     * foreground app (a chat box, a comment field, a search bar...).
     * Returns false if nothing editable is focused right now.
     */
    fun typeIntoFocusedField(text: String): Boolean {
        val node = findFocusedEditable(rootInActiveWindow) ?: return false
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findFocusedEditable(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isFocused && root.isEditable) return root
        for (i in 0 until root.childCount) {
            val found = findFocusedEditable(root.getChild(i))
            if (found != null) return found
        }
        return null
    }
}
