package com.biomedixai.ricefieldsamplelogger.biokt

import kotlin.math.abs

enum class SequenceType {
    DNA,
    RNA,
    PROTEIN
}

data class FastaRecord(
    val id: String,
    val description: String,
    val sequence: String,
    val type: SequenceType = SequenceType.DNA
) {
    val length: Int get() = sequence.length

    fun calculateGcContent(): Double {
        if (sequence.isEmpty()) return 0.0
        val gcCount = sequence.count { it == 'G' || it == 'C' || it == 'g' || it == 'c' }
        return (gcCount.toDouble() / sequence.length) * 100.0
    }

    fun nucleotideCounts(): Map<Char, Int> {
        val counts = mutableMapOf('A' to 0, 'T' to 0, 'C' to 0, 'G' to 0, 'N' to 0)
        for (char in sequence.uppercase()) {
            counts[char] = (counts[char] ?: 0) + 1
        }
        return counts
    }
}

object SeqIO {
    /**
     * Parses standard FASTA formatted text string into FastaRecord objects.
     */
    fun parseString(fastaContent: String): List<FastaRecord> {
        return BioKt.parseFasta(fastaContent)
    }
}

object BioKt {

    fun isValidDna(sequence: String): Boolean {
        val valid = setOf('A', 'C', 'G', 'T', 'N', 'a', 'c', 'g', 't', 'n')
        return sequence.isNotEmpty() && sequence.all { it in valid }
    }

    fun parseFasta(fastaContent: String): List<FastaRecord> {
        val records = mutableListOf<FastaRecord>()
        var currentHeader = ""
        var currentDescription = ""
        val currentSeq = StringBuilder()

        for (rawLine in fastaContent.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith(">")) {
                if (currentHeader.isNotEmpty()) {
                    records.add(
                        FastaRecord(
                            id = currentHeader,
                            description = currentDescription,
                            sequence = currentSeq.toString().uppercase(),
                            type = SequenceType.DNA
                        )
                    )
                    currentSeq.clear()
                }
                val headerParts = line.substring(1).trim().split(" ", limit = 2)
                currentHeader = headerParts.firstOrNull() ?: "seq"
                currentDescription = if (headerParts.size > 1) headerParts[1] else ""
            } else {
                currentSeq.append(line)
            }
        }

        if (currentHeader.isNotEmpty() && currentSeq.isNotEmpty()) {
            records.add(
                FastaRecord(
                    id = currentHeader,
                    description = currentDescription,
                    sequence = currentSeq.toString().uppercase(),
                    type = SequenceType.DNA
                )
            )
        }

        return records
    }

    fun reverseComplement(dna: String): String {
        return dna.reversed().map {
            when (it.uppercaseChar()) {
                'A' -> 'T'
                'T' -> 'A'
                'C' -> 'G'
                'G' -> 'C'
                'U' -> 'A'
                else -> 'N'
            }
        }.joinToString("")
    }
}
