package com.scenefinder.domain.model

data class AccessibilityDumpNode(
    val className: String,
    val viewId: String?,
    val text: String?,
    val contentDescription: String?,
    val bounds: String,
    val depth: Int
) {
    fun toDumpLine(): String {
        val indent = "  ".repeat(depth)
        val idStr = viewId?.let { " id=[$it]" } ?: ""
        val textStr = text?.let { " text=\"${it.replace("\n", "\\n")}\"" } ?: ""
        val cdStr = contentDescription?.let { " desc=\"${it.replace("\n", "\\n")}\"" } ?: ""
        return "$indent- $className$idStr$textStr$cdStr bounds=[$bounds]"
    }
}
