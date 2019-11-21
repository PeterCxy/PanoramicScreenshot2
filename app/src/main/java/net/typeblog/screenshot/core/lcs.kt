package net.typeblog.screenshot.core

data class CommonSubstring(
    // Left = str1
    val leftStart: Int,
    val leftEnd: Int,

    // Right = str2
    val rightStart: Int,
    val rightEnd: Int
)

// Implementation of Longest Common Substring with Generalized Type and DP
// Reference: <https://en.wikipedia.org/wiki/Longest_common_substring_problem>
// When writing the algorithm, I realized that our application is no more than
// a LCS with BitmapLines instead of Strings
fun <T> lcs(src: List<T>, target: List<T>): List<CommonSubstring> {
    // The dynamic programming cache memory (keep name consistent with wiki)
    @Suppress("LocalVariableName")
    val L = Array(src.size) { IntArray(target.size) }
    // The current longest substring length
    var z = 0
    var ret = mutableListOf<CommonSubstring>()
    for (i in src.indices) {
        for (j in target.indices) {
            if (src[i] == target[j]) {
                if (i == 0 || j == 0) {
                    L[i][j] = 1
                } else {
                    L[i][j] = L[i - 1][j - 1] + 1
                }

                if (L[i][j] > z) {
                    z = L[i][j]
                    ret.clear()
                    ret.add(CommonSubstring(i - z + 1, i, j - z + 1, j))
                } else if (L[i][j] == z) {
                    ret.add(CommonSubstring(i - z + 1, i, j - z + 1, j))
                }
            } else {
                L[i][j] = 0
            }
        }
    }

    return ret
}