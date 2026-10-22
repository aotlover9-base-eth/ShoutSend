package app.shoutsend.modem.fec

import app.shoutsend.modem.profile.RsProfile

/**
 * High-performance Errors-and-Erasures Reed-Solomon Decoder for GF(256).
 *
 * Implements Berlekamp-Massey with Forney algorithm and Chien root search.
 * Never throws exceptions on malformed or corrupted audio data, returning [RsDecodeResult.Failure].
 *
 * Correction capacity:
 *   2 * errors + erasures <= nsym
 */
public object RsDecoder {

    /**
     * Decodes [received] codeword, correcting up to [profile.nsym] erasure and error symbols.
     *
     * @param received The raw codeword bytes (length <= profile.n and >= profile.nsym).
     * @param erasurePositions Array of zero-based byte indexes known to be corrupted/unreliable.
     * @param profile The Reed-Solomon profile specifying codeword parameters.
     */
    public fun decode(
        received: ByteArray,
        erasurePositions: IntArray = IntArray(0),
        profile: RsProfile = RsProfile.STANDARD
    ): RsDecodeResult {
        val n = received.size
        val nsym = profile.nsym
        val k = n - nsym

        if (n < nsym) {
            return RsDecodeResult.Failure("Codeword length ($n) is less than parity count ($nsym)")
        }
        if (n > profile.n) {
            return RsDecodeResult.Failure("Codeword length ($n) exceeds profile limit (${profile.n})")
        }

        // Validate and deduplicate erasure positions
        val uniqueErasures = IntArray(erasurePositions.size)
        var erasureCount = 0
        for (pos in erasurePositions) {
            if (pos !in 0 until n) {
                return RsDecodeResult.Failure("Erasure position $pos out of bounds (0 until $n)")
            }
            var duplicate = false
            for (i in 0 until erasureCount) {
                if (uniqueErasures[i] == pos) {
                    duplicate = true
                    break
                }
            }
            if (!duplicate) {
                uniqueErasures[erasureCount++] = pos
            }
        }

        if (erasureCount > nsym) {
            return RsDecodeResult.Failure("Erasure count ($erasureCount) exceeds parity capacity ($nsym)")
        }

        // 1. Calculate syndromes: S_i = sum_{j=0}^{n-1} received[j] * (alpha^i)^(n-1-j)
        val s = IntArray(nsym)
        var hasNonZeroSyndrome = false
        for (i in 0 until nsym) {
            val root = Gf256.exp[i]
            var eval = 0
            for (j in 0 until n) {
                eval = Gf256.multiply(eval, root) xor (received[j].toInt() and 0xFF)
            }
            s[i] = eval
            if (eval != 0) {
                hasNonZeroSyndrome = true
            }
        }

        // If all syndromes are zero and no erasures were flagged, the codeword is already clean
        if (!hasNonZeroSyndrome && erasureCount == 0) {
            val data = ByteArray(k)
            System.arraycopy(received, 0, data, 0, k)
            return RsDecodeResult.Corrected(data = data, errorCount = 0, erasureCount = 0)
        }

        // 2. Build erasure locator polynomial: Lambda_erasures(x) = prod_{pos in erasures} (1 - X_pos * x)
        // Stored low degree first (constant term at index 0)
        var lambdaErasures = IntArray(1) { 1 }
        for (i in 0 until erasureCount) {
            val pos = uniqueErasures[i]
            val xLocator = Gf256.exp[(n - 1 - pos) % Gf256.FIELD_ORDER]

            val newLe = IntArray(lambdaErasures.size + 1)
            for (idx in lambdaErasures.indices) {
                val c = lambdaErasures[idx]
                newLe[idx] = newLe[idx] xor c
                newLe[idx + 1] = newLe[idx + 1] xor Gf256.multiply(c, xLocator)
            }
            lambdaErasures = newLe
        }

        // 3. Berlekamp-Massey algorithm initialized from erasure locator
        var cPoly = lambdaErasures.copyOf()
        var bPoly = lambdaErasures.copyOf()
        var l = erasureCount
        var m = 1
        var b = 1

        for (step in erasureCount until nsym) {
            // Compute discrepancy d
            var d = s[step]
            for (i in 1 until cPoly.size) {
                if (step - i >= 0) {
                    d = d xor Gf256.multiply(cPoly[i], s[step - i])
                }
            }

            if (d == 0) {
                m++
            } else {
                val factor = Gf256.divide(d, b)
                val newLen = maxOf(cPoly.size, bPoly.size + m)
                val tPoly = IntArray(newLen)
                for (idx in cPoly.indices) {
                    tPoly[idx] = tPoly[idx] xor cPoly[idx]
                }
                for (idx in bPoly.indices) {
                    tPoly[idx + m] = tPoly[idx + m] xor Gf256.multiply(factor, bPoly[idx])
                }

                if (2 * l <= step + erasureCount) {
                    l = step + 1 + erasureCount - l
                    bPoly = cPoly
                    b = d
                    m = 1
                } else {
                    m++
                }
                cPoly = tPoly
            }
        }

        // Trim leading zero coefficients from highest degree end
        var cLen = cPoly.size
        while (cLen > 1 && cPoly[cLen - 1] == 0) {
            cLen--
        }
        val lambdaFull = if (cLen == cPoly.size) cPoly else cPoly.copyOf(cLen)

        val totalDegree = lambdaFull.size - 1
        val errorCount = totalDegree - erasureCount

        // 4. Verification against theoretical correction capacity (2*e + s <= nsym)
        if (2 * errorCount + erasureCount > nsym || errorCount < 0) {
            return RsDecodeResult.Failure("Error capacity exceeded: 2*$errorCount + $erasureCount > $nsym")
        }

        // 5. Chien search: evaluate Lambda_full(X^-1) for each codeword position
        val errorPositions = IntArray(totalDegree)
        var foundRoots = 0

        for (pos in 0 until n) {
            val xLocator = Gf256.exp[(n - 1 - pos) % Gf256.FIELD_ORDER]
            val xInv = Gf256.inverse(xLocator)

            // Evaluate lambdaFull(xInv) [low degree to high]
            var eval = 0
            var xPow = 1
            for (coeff in lambdaFull) {
                eval = eval xor Gf256.multiply(coeff, xPow)
                xPow = Gf256.multiply(xPow, xInv)
            }

            if (eval == 0) {
                if (foundRoots < totalDegree) {
                    errorPositions[foundRoots++] = pos
                } else {
                    return RsDecodeResult.Failure("Chien search found more roots than degree ($totalDegree)")
                }
            }
        }

        if (foundRoots != totalDegree) {
            return RsDecodeResult.Failure("Chien search found $foundRoots roots, expected $totalDegree")
        }

        // 6. Forney algorithm:
        // Omega(x) = (S(x) * Lambda_full(x)) mod x^nsym
        val omega = IntArray(nsym)
        for (i in 0 until nsym) {
            val si = s[i]
            if (si == 0) continue
            for (j in lambdaFull.indices) {
                if (i + j < nsym) {
                    omega[i + j] = omega[i + j] xor Gf256.multiply(si, lambdaFull[j])
                }
            }
        }

        // Formal derivative Lambda'(x) [low degree first]
        val lambdaDeriv = IntArray(lambdaFull.size)
        for (i in 1 until lambdaFull.size) {
            if (i % 2 == 1) {
                lambdaDeriv[i - 1] = lambdaFull[i]
            }
        }

        // 7. Correct errors and erasures
        val correctedCodeword = received.copyOf()
        for (i in 0 until foundRoots) {
            val pos = errorPositions[i]
            val xLocator = Gf256.exp[(n - 1 - pos) % Gf256.FIELD_ORDER]
            val xInv = Gf256.inverse(xLocator)

            // Evaluate Omega(X^-1)
            var num = 0
            var xPow = 1
            for (coeff in omega) {
                num = num xor Gf256.multiply(coeff, xPow)
                xPow = Gf256.multiply(xPow, xInv)
            }

            // Evaluate Lambda'(X^-1)
            var den = 0
            xPow = 1
            for (coeff in lambdaDeriv) {
                den = den xor Gf256.multiply(coeff, xPow)
                xPow = Gf256.multiply(xPow, xInv)
            }

            if (den == 0) {
                return RsDecodeResult.Failure("Zero denominator in Forney magnitude calculation at pos $pos")
            }

            // Since roots start at alpha^0 (b = 0): Y = X * Omega(X^-1) / Lambda'(X^-1)
            val magnitude = Gf256.multiply(xLocator, Gf256.divide(num, den))
            correctedCodeword[pos] = (correctedCodeword[pos].toInt() xor magnitude).toByte()
        }

        // 8. Post-correction syndrome check
        for (i in 0 until nsym) {
            val root = Gf256.exp[i]
            var eval = 0
            for (j in 0 until n) {
                eval = Gf256.multiply(eval, root) xor (correctedCodeword[j].toInt() and 0xFF)
            }
            if (eval != 0) {
                return RsDecodeResult.Failure("Post-correction syndrome S_$i is non-zero ($eval)")
            }
        }

        // Extract systematic payload data
        val data = ByteArray(k)
        System.arraycopy(correctedCodeword, 0, data, 0, k)
        return RsDecodeResult.Corrected(
            data = data,
            errorCount = errorCount,
            erasureCount = erasureCount
        )
    }
}
