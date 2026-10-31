package com.example.blecontroller

data class MacroStep(
    val id: String,
    val label: String,
    val rawHexOrPayload: ByteArray,
    val delayMs: Long,
    val rgbPreview: Int? = null,
    val isRaw: Boolean = false // If true, send raw bytes, else build 20-byte packet (assume 0x33, 0x05 for rgb)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MacroStep

        if (id != other.id) return false
        if (label != other.label) return false
        if (!rawHexOrPayload.contentEquals(other.rawHexOrPayload)) return false
        if (delayMs != other.delayMs) return false
        if (rgbPreview != other.rgbPreview) return false
        if (isRaw != other.isRaw) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + rawHexOrPayload.contentHashCode()
        result = 31 * result + delayMs.hashCode()
        result = 31 * result + (rgbPreview ?: 0)
        result = 31 * result + isRaw.hashCode()
        return result
    }
}

data class MacroSequence(
    val id: String,
    val name: String,
    val steps: List<MacroStep>,
    val loopIndefinitely: Boolean,
    val repeatCount: Int
)
