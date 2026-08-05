package cn.james.music.kugou.api.session

class KugouCookies private constructor(
    private val values: Map<String, String>,
) {
    fun value(name: String): String? = values[name]

    fun asMap(): Map<String, String> = values.toMap()

    fun headerValue(): String = values.toSortedMap().entries.joinToString(separator = "; ") { (name, value) -> "$name=$value" }

    fun mergeSetCookie(headers: List<String>): KugouCookies {
        val merged = values.toMutableMap()
        headers.mapNotNull(::parseSetCookie).forEach { (name, value) ->
            if (value.isEmpty()) {
                merged.remove(name)
            } else {
                merged[name] = value
            }
        }
        return from(merged)
    }

    override fun equals(other: Any?): Boolean = other is KugouCookies && values == other.values

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String = "KugouCookies(names=${values.keys.sorted()})"

    companion object {
        val Empty = KugouCookies(emptyMap())

        fun from(values: Map<String, String>): KugouCookies {
            require(values.keys.all(::isValidName)) { "Cookie names contain invalid characters" }
            require(values.values.none { '\r' in it || '\n' in it }) { "Cookie values contain invalid characters" }
            return KugouCookies(values.filterValues(String::isNotEmpty).toMap())
        }

        private fun parseSetCookie(header: String): Pair<String, String>? {
            val firstSegment = header.substringBefore(';').trim()
            val separator = firstSegment.indexOf('=')
            if (separator <= 0) return null
            val name = firstSegment.substring(0, separator).trim()
            val value = firstSegment.substring(separator + 1).trim()
            if (!isValidName(name) || '\r' in value || '\n' in value) return null
            return name to value
        }

        private fun isValidName(name: String): Boolean =
            name.isNotBlank() && name.none { character -> character.isWhitespace() || character in "=;,\r\n" }
    }
}
