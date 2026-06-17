package de.szalkowski.activitylauncher

/**
 * Startup type for portal launcher startup items.
 */
enum class StartupType(val value: Int) {
    /** Launch immediately after device boot. */
    IMMEDIATE(0),

    /** Wait for network connectivity before launching. */
    ON_NETWORK(1),
    ;

    companion object {
        fun fromValue(value: Int): StartupType =
            entries.find { it.value == value } ?: IMMEDIATE
    }
}

/**
 * Represents a startup item that is automatically launched after device boot.
 */
data class StartupItem(
    val packageName: String,
    val className: String,
    val startupType: StartupType = StartupType.IMMEDIATE,
    val delaySeconds: Int = 0,
)
