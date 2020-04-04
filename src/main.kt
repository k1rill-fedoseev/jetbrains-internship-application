fun main() {
    val query: String = readLine()!!
    val parser = Parser(query)
    print(parser.parse())
}
