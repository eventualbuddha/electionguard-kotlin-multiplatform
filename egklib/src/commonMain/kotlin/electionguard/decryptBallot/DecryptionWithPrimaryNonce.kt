package electionguard.decryptBallot

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.decryptWithNonceToContestData
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.Nonces
import electionguard.core.UInt256
import electionguard.core.decryptWithNonce
import electionguard.core.get
import electionguard.core.hashElements
import electionguard.core.take
import electionguard.core.toElementModQ

/** Decryption of a EncryptedBallot using the master nonce. */
class DecryptionWithPrimaryNonce(val group : GroupContext, val manifest: Manifest, val publicKey: ElGamalPublicKey) {

    fun EncryptedBallot.decrypt(primaryNonce: ElementModQ): Result<PlaintextBallot, String> {
        val ballotNonce: UInt256 = hashElements(manifest.manifestHash, this.ballotId, primaryNonce)

        val (plaintextContests, cerrors) = this.contests.map {
            val mcontest = manifest.contests.find { tcontest -> it.contestId == tcontest.contestId}
            if (mcontest == null) {
                Err("Cant find contest ${it.contestId} in manifest")
            } else {
                decryptContestWithPrimaryNonce(mcontest, ballotNonce, it)
            }
        }.partition()

        if (cerrors.isNotEmpty()) {
            return Err(cerrors.joinToString("\n"))
        }
        return Ok(PlaintextBallot(
            this.ballotId,
            this.ballotStyleId,
            plaintextContests,
            null
        ))
    }

    private fun decryptContestWithPrimaryNonce(
        mcontest: Manifest.ContestDescription,
        ballotNonce: UInt256,
        contest: EncryptedBallot.Contest
    ): Result<PlaintextBallot.Contest, String> {
        val contestDescriptionHash = mcontest.contestHash
        val contestDescriptionHashQ = contestDescriptionHash.toElementModQ(group)
        val (contestNonce, _, contestDataNonce) = Nonces(contestDescriptionHashQ, ballotNonce).take(3)

        val dSelections = mutableListOf<PlaintextBallot.Selection>()
        val errors = mutableListOf<String>()
        for (selection in contest.selections) {
            val mselection = mcontest.selections.find { it.selectionId == selection.selectionId }
            if (mselection == null) {
                errors.add(" Cant find selection ${selection.selectionId} in contest ${mcontest.contestId}")
                continue
            }
            val dSelection = decryptSelectionWithPrimaryNonce(mselection, contestNonce, selection)
            if (dSelection == null) {
                errors.add(" decryption with master nonce failed for contest: ${contest.contestId} selection: ${selection.selectionId}")
            } else {
                dSelections.add(dSelection)
            }
        }
        if (errors.isNotEmpty()) {
            return Err(errors.joinToString("\n"))
        }

        // contest data
        val contestDataResult = contest.contestData.decryptWithNonceToContestData(publicKey, contestDataNonce)
        if (contestDataResult is Err) {
            return contestDataResult
        }
        val contestData = contestDataResult.unwrap()

        // on overvote, modify selections to use original votes
        val useSelections = if (contestData.status == ContestDataStatus.over_vote) {
            // set the selections to the original
            dSelections.map { dselection ->
                if (contestData.overvotes.find { it == dselection.sequenceOrder } == null) dselection
                else dselection.copy(vote = 1)
            }
        } else {
            dSelections
        }

        return Ok(PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            useSelections,
            contestData.writeIns,
        ))
    }

    private fun decryptSelectionWithPrimaryNonce(
        mselection: Manifest.SelectionDescription,
        contestNonce: ElementModQ,
        selection: EncryptedBallot.Selection
    ): PlaintextBallot.Selection? {
        val nonceSequence = Nonces(mselection.selectionHash.toElementModQ(group), contestNonce)
        val selectionNonce: ElementModQ = nonceSequence[1]

        val decodedVote: Int? = selection.ciphertext.decryptWithNonce(publicKey, selectionNonce)
        return decodedVote?.let {
            PlaintextBallot.Selection(
                selection.selectionId,
                selection.sequenceOrder,
                decodedVote,
            )
        }
    }
}