fun ask(question: String): String {
    print("$question ")
    return readln()
}

fun String.span(predicate: (Char) -> Boolean): Pair<String, String> {
    val first = takeWhile(predicate)
    val second = dropWhile(predicate)
    return first to second
}