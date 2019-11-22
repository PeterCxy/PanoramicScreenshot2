package net.typeblog.screenshot.core

data class CommonSubstring(
    // Left = str1
    val leftStart: Int,
    val leftEnd: Int,

    // Right = str2
    val rightStart: Int,
    val rightEnd: Int
)

fun <T, U> Sequence<T>.cartesianProduct(b: Sequence<U>): Sequence<Pair<T, U>> =
    flatMap { itemA -> b.map { itemB -> Pair(itemA, itemB) } }

// Implementation of Longest Common Substring with Generalized Type and DP
// Reference: <https://en.wikipedia.org/wiki/Longest_common_substring_problem>
// When writing the algorithm, I realized that our application is no more than
// a LCS with BitmapLines instead of Strings
fun <T> lcs(src: List<T>, target: List<T>): List<CommonSubstring> =
    src.asSequence().withIndex()
        .cartesianProduct(target.asSequence().withIndex())
        .filter { (itemA, itemB) -> itemA.value == itemB.value }
        .map { (itemA, itemB) -> Pair(itemA.index, itemB.index) }
        .fold(Triple(
            Array(src.size) { IntArray(target.size) } /* L */,
            0 /* Z */,
            mutableListOf<CommonSubstring>() /* ret */)
        ) {
                (L, z, ret), (i, j) ->

            if (i == 0 || j == 0) {
                L[i][j] = 1
            } else {
                L[i][j] = L[i - 1][j - 1] + 1
            }

            when {
                L[i][j] > z -> {
                    ret.clear()
                    ret.add(CommonSubstring(i - z + 1, i, j - z + 1, j))
                    Triple(L, L[i][j], ret)
                }
                L[i][j] == z -> {
                    ret.add(CommonSubstring(i - z + 1, i, j - z + 1, j))
                    Triple(L, z, ret)
                }
                else -> {
                    Triple(L, z, ret)
                }
            }
        }.third