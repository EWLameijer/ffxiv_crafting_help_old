fun String.span(predicate: (Char) -> Boolean): Pair<String, String> {
    val first = takeWhile(predicate)
    val second = dropWhile(predicate)
    return first to second
}

fun <T> List<T>.span(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val first = takeWhile(predicate)
    val second = dropWhile(predicate)
    return first to second
}