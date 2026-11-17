package com.biomedixai.ricefieldsamplelogger.biokt.rice

data class CisElementResult(
    val name: String,
    val motif: String,
    val position: Int,
    val description: String
)

data class SsrMarkerResult(
    val motif: String,
    val repeatCount: Int,
    val startIndex: Int,
    val endIndex: Int
)

object RiceBioKt {

    private val RICE_PROMOTER_ELEMENTS = listOf(
        Triple("TATA-box", "TATAAA", "Core promoter motif required for transcription initiation"),
        Triple("CAAT-box", "CCAAT", "Enhancer & promoter element regulating transcription efficiency"),
        Triple("W-box", "TTGACC", "WRKY transcription factor binding site (Biotic/Abiotic stress)"),
        Triple("G-box", "CACGTG", "Light and Abscisic Acid (ABA) responsive promoter motif"),
        Triple("DRE/CRT", "GCCGAC", "Dehydration, drought and cold responsive element"),
        Triple("ABRE", "ACGTG", "Abscisic Acid (ABA) responsiveness in rice drought stress"),
        Triple("HSE", "AAAAACGT", "Heat shock response promoter element"),
        Triple("MYB motif", "TAACCA", "Drought & flavonoid biosynthesis regulation in Oryza sativa"),
        Triple("E-box", "CANNTG", "Basic Helix-Loop-Helix (bHLH) factor binding site")
    )

    /**
     * Scans sequence for rice promoter cis-regulatory elements.
     */
    fun scanRicePromoter(sequence: String): List<CisElementResult> {
        val results = mutableListOf<CisElementResult>()
        val upper = sequence.uppercase()

        for ((name, pattern, desc) in RICE_PROMOTER_ELEMENTS) {
            if (pattern.contains("N")) {
                // Regex pattern matching
                val regexPattern = pattern.replace("N", "[ATCG]")
                val regex = Regex(regexPattern)
                for (match in regex.findAll(upper)) {
                    results.add(
                        CisElementResult(
                            name = name,
                            motif = match.value,
                            position = match.range.first + 1,
                            description = desc
                        )
                    )
                }
            } else {
                // Exact pattern search
                var index = upper.indexOf(pattern)
                while (index != -1) {
                    results.add(
                        CisElementResult(
                            name = name,
                            motif = pattern,
                            position = index + 1,
                            description = desc
                        )
                    )
                    index = upper.indexOf(pattern, index + 1)
                }
            }
        }
        return results.sortedBy { it.position }
    }

    /**
     * Detects Simple Sequence Repeats (SSRs / microsatellites) in rice DNA sequences.
     */
    fun detectSSRs(sequence: String, minRepeats: Int = 3): List<SsrMarkerResult> {
        val results = mutableListOf<SsrMarkerResult>()
        val upper = sequence.uppercase()

        // Scan for motif lengths 2, 3, and 4
        for (motifLen in 2..4) {
            var i = 0
            while (i <= upper.length - motifLen * minRepeats) {
                val motif = upper.substring(i, i + motifLen)

                // Skip non-variable motifs like AAAA or TTTT for 2-4bp
                if (motif.toSet().size == 1 && motifLen > 1) {
                    i++
                    continue
                }

                var count = 0
                var currentPos = i
                while (currentPos + motifLen <= upper.length && upper.substring(currentPos, currentPos + motifLen) == motif) {
                    count++
                    currentPos += motifLen
                }

                if (count >= minRepeats) {
                    results.add(
                        SsrMarkerResult(
                            motif = "($motif)$count",
                            repeatCount = count,
                            startIndex = i + 1,
                            endIndex = currentPos
                        )
                    )
                    i = currentPos
                } else {
                    i++
                }
            }
        }
        return results.distinctBy { Pair(it.startIndex, it.motif) }.sortedBy { it.startIndex }
    }
}

// Global convenience functions matching prompt requirements
fun scanRicePromoter(sequence: String): List<CisElementResult> {
    return RiceBioKt.scanRicePromoter(sequence)
}

fun detectSSRs(sequence: String, minRepeats: Int = 3): List<SsrMarkerResult> {
    return RiceBioKt.detectSSRs(sequence, minRepeats)
}
