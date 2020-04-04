const val WORD_REGEX = "[A-Za-z_]\\w*"
const val SIMPLE_ID_REGEX = "($WORD_REGEX|'$WORD_REGEX')"
const val COMPOSITE_ID_REGEX = "($WORD_REGEX|'$WORD_REGEX(\\.$WORD_REGEX)*')"

class Parser(query: String) {
    private val query = query.trim()
    private var table: String? = null
    private val columns: MutableList<String> = mutableListOf()
    private var limit: String? = null
    private var offset: String? = null
    private val where: MutableList<String> = mutableListOf()
    private var whereSeparator: String = ""
    private var position: Int = 0

    fun parse(): String {
        processToken("select\\s+")

        // it is possible to select everything or only specific fields
        if (lookahead("*")) {
            processToken("*\\s+")
        } else {
            do {
                val columnName: String = processToken(COMPOSITE_ID_REGEX)
                columns.add(columnName)
                val separator: String = processToken("\\s*,?\\s*")
            } while (separator.contains(','))
        }

        // process table name
        processToken("from\\s+")
        table = processToken(SIMPLE_ID_REGEX).trim('\'')

        skipWhitespaces()

        // parse keywords offset / limit / where
        while (!isEnd()) {
            when {
                lookahead("offset") && offset == null -> {
                    processToken("offset\\s+")
                    offset = processToken("\\d+")

                    skipWhitespaces()
                }
                lookahead("limit") && limit == null -> {
                    processToken("limit\\s+")
                    limit = processToken("\\d+")

                    skipWhitespaces()
                }
                lookahead("where") && where.isEmpty() -> {
                    processToken("where\\s+")

                    // query can contain several conditions, but only one logical operator can be used
                    while (true) {
                        // scan column / field name which is compared to some value
                        val columnName: String = processToken(COMPOSITE_ID_REGEX)
                        // scan comparison operator
                        val operator: String = processToken("\\s*(<>|[=<>])\\s*").trim()
                        // scan value, to which field is compared
                        val value: String = processToken("(\\d+|'.*')")

                        where.add(encodeCondition(columnName, operator, value))

                        skipWhitespaces()

                        // look at next logical operator, or break otherwise
                        val logicalOperator: String =
                            if (lookahead("and")) "and" else if (lookahead("or")) "or" else break

                        // remember logical operator at first iteration
                        if (whereSeparator == "") {
                            whereSeparator = logicalOperator
                        } else if (logicalOperator != whereSeparator) {
                            throw ParserException("Incorrect logical operator")
                        }

                        processToken("$whereSeparator\\s+")
                    }
                }
                else -> {
                    throw ParserException("Invalid query")
                }
            }
        }
        val args: String = when {
            columns.isNotEmpty() -> "${selectCondition()}, ${projectCondition()}"
            where.isNotEmpty() -> selectCondition()
            else -> ""
        }
        return "db.$table.find($args)${if (offset != null) ".skip($offset)" else ""}${if (limit != null) ".limit($limit)" else ""}"
    }

    private fun selectCondition(): String = when (whereSeparator) {
        "or" -> "[${where.joinToString(", ") { "{$it}" }}]"
        else -> "{${where.joinToString(", ") { it }}}"
    }

    private fun encodeCondition(column: String, operator: String, value: String): String = when (operator) {
        "=" -> "$column: $value"
        "<>" -> "$column: {\$ne: $value}"
        ">" -> "$column: {\$gt: $value}"
        "<" -> "$column: {\$lt: $value}"
        else -> throw ParserException("Unknown operator")
    }

    private fun projectCondition(): String = when {
        columns.isEmpty() -> "{}"
        else -> "{${columns.joinToString(", ") { "$it: 1" }}}"
    }

    private fun processToken(pattern: String): String {
        val regex = Regex("^$pattern", RegexOption.IGNORE_CASE)
        val match: MatchResult = regex.find(query.substring(position)) ?: throw ParserException("failed to parse")
        position += match.range.last + 1
        return match.value
    }

    private fun lookahead(pattern: String): Boolean = query.startsWith(pattern, position, true)

    private fun isEnd(): Boolean = query.length == position

    private fun skipWhitespaces() = when {
        !isEnd() -> processToken("\\s+")
        else -> null
    }
}
