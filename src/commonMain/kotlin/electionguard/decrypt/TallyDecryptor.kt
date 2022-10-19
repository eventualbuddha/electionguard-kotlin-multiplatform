package electionguard.decrypt

import electionguard.ballot.EncryptedTally
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.Guardian
import electionguard.ballot.LagrangeCoordinate
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext
import electionguard.core.compatibleContextOrFail
import electionguard.core.hashElements
import electionguard.core.toElementModQ

/** Turn a EncryptedTally into a DecryptedTallyOrBallot. */
class TallyDecryptor(
    val group: GroupContext,
    val qbar: ElementModQ,
    val jointPublicKey: ElGamalPublicKey,
    val lagrangeCoordinates: Map<String, LagrangeCoordinate>,
    val guardians: List<Guardian>) {

    /**
     * After gathering the shares for all guardians (partial or compensated), we can decrypt the tally.
     * Shares are in a Map keyed by "${contestId}#@${selectionId}"
     */
    fun decryptTally(tally: EncryptedTally, trusteeDecryptions : TrusteeDecryptions): DecryptedTallyOrBallot {
        val contests: MutableMap<String, DecryptedTallyOrBallot.Contest> = HashMap()
        for (tallyContest in tally.contests) {
            val decryptedContest = decryptContest(tallyContest, trusteeDecryptions)
            contests[tallyContest.contestId] = decryptedContest
        }
        return DecryptedTallyOrBallot(tally.tallyId, contests)
    }

    private fun decryptContest(
        contest: EncryptedTally.Contest,
        trusteeDecryptions : TrusteeDecryptions,
    ): DecryptedTallyOrBallot.Contest {
        val selections: MutableMap<String, DecryptedTallyOrBallot.Selection> = HashMap()
        for (tallySelection in contest.selections) {
            val id = "${contest.contestId}#@${tallySelection.selectionId}"
            val shares = trusteeDecryptions.shares[id] ?: throw IllegalStateException("*** $id share not found") // TODO something better?
            val decryptedSelection = decryptSelection(tallySelection, shares, contest.contestId)
            selections[tallySelection.selectionId] = decryptedSelection
        }
        return DecryptedTallyOrBallot.Contest(contest.contestId, selections, null)
    }

    private fun decryptSelection(
        selection: EncryptedTally.Selection,
        results: DecryptionResults, // results for this selection
        contestId: String,
    ): DecryptedTallyOrBallot.Selection {

        // response is the sum of the individual responses
        val response: ElementModQ = with(group) { results.responses.values.map { it }.addQ() }
        // finally we can create the proof
        val proof = GenericChaumPedersenProof(results.challenge!!.toElementModQ(group), response)

        val result = DecryptedTallyOrBallot.Selection(
            selection.selectionId,
            results.dlogM!!,
            results.M!!,
            selection.ciphertext,
            proof
        )
        if (!result.verifySelection() && !results.detailedVerify()) {
            println("verify failed for  $contestId and ${selection.selectionId}")
        }
        return result
    }

    // this is the verifier proof. Replace with eq 64 and 65, which would indicate where theres a problem ??
    private fun DecryptedTallyOrBallot.Selection.verifySelection() : Boolean {
        val a = group.gPowP(this.proof.r) * (jointPublicKey powP this.proof.c) // 8.1
        val b = (this.message.pad powP this.proof.r) * (this.value powP this.proof.c) // 8.2
        val challenge = hashElements(qbar, jointPublicKey, this.message.pad, this.message.data, a, b, this.value) // 8.B
        return (challenge.toElementModQ(group) == this.proof.c)
    }

    // Verify with eq 64 and 65
    private fun DecryptionResults.detailedVerify() : Boolean {
        var ok = true
        for (partialDecryption in this.shares.values) {
            val guardian = guardians.find { it.guardianId == partialDecryption.guardianId} ?: throw IllegalStateException("*** guardian ${partialDecryption.guardianId} not found")
            val lagrange = lagrangeCoordinates[guardian.guardianId] ?: throw IllegalStateException("*** lagrange not found for ${guardian.guardianId}")
            val response = this.responses[guardian.guardianId] ?: throw IllegalStateException("*** response not found for ${guardian.guardianId}")
            val challenge = this.challenge!!.toElementModQ(group)

            val inner = innerFactor72(guardian.xCoordinate)
            val middle = guardian.publicKey() * (inner powP lagrange.lagrangeCoordinate)
            val ap = group.gPowP(response) * (middle powP challenge) // 64
            if (partialDecryption.a != ap) {
                println("ayes dont match for ${guardian.guardianId}")
                ok = false
            }

            val bp = (this.ciphertext.pad powP response) // 65
            if (partialDecryption.b != bp) {
                println("bees dont match for ${guardian.guardianId}")
                ok = false
            }
        }
        return ok
    }

    // the innermost factor of eq 72
    private fun innerFactor72(xcoord: Int) : ElementModP {
        val trusteeNames = lagrangeCoordinates.values.map { it.guardianId }.toSet()
        val missingGuardians = guardians.filter { !trusteeNames.contains(it.guardianId) }

        return with (group) { missingGuardians.map { calculateGexpPiAtL(xcoord, it.coefficientCommitments() ) }.multP() }
    }

    /**
     * Calculate g^Pi(ℓ) mod p = Product ((K_i,j)^ℓ^j) mod p, j = 0, quorum-1
     * Used in KeyCeremonyTrustee and DecryptingTrustee, public information.
     * use the one in ElectionPolynomial
     */
    fun calculateGexpPiAtL(
        xcoord: Int,  // l
        coefficientCommitments: List<ElementModP>  // the committments to Pi
    ): ElementModP {
        val group = compatibleContextOrFail(*coefficientCommitments.toTypedArray())
        val xcoordQ: ElementModQ = group.uIntToElementModQ(xcoord.toUInt())
        var result: ElementModP = group.ONE_MOD_P
        var xcoordPower: ElementModQ = group.ONE_MOD_Q // ℓ^j

        for (commitment in coefficientCommitments) {
            val term = commitment powP xcoordPower // (K_i,j)^ℓ^j
            result *= term
            xcoordPower *= xcoordQ
        }
        return result
    }

}