package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.unwrap
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.GroupContext

fun GroupContext.importDecryptedTallyOrBallot(tally: electionguard.protogen.DecryptedTallyOrBallot?):
        Result<DecryptedTallyOrBallot, String> {
    if (tally == null) {
        return Err("Null DecryptedTallyOrBallot")
    }

    if (tally.contests.isEmpty()) {
        return Err("No contests in DecryptedTallyOrBallot")
    }

    val (contests, errors) = tally.contests.map { this.importContest(it) }.partition()
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(DecryptedTallyOrBallot(tally.tallyId, contests.associateBy { it.contestId }))
}

private fun GroupContext.importContest(contest: electionguard.protogen.DecryptedContest):
        Result<DecryptedTallyOrBallot.Contest, String> {

    if (contest.selections.isEmpty()) {
        return Err("No selections in DecryptedContest")
    }

    val (selections, errors) = contest.selections.map { this.importSelection(it) }.partition()
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(DecryptedTallyOrBallot.Contest(
        contest.contestId,
        selections.associateBy { it.selectionId },
    null, // TODO
    ))
}

private fun GroupContext.importSelection(selection: electionguard.protogen.DecryptedSelection):
        Result<DecryptedTallyOrBallot.Selection, String> {
    val value = this.importElementModP(selection.value)
        .toResultOr { "DecryptedSelection ${selection.selectionId} value was malformed or missing" }
    val message = this.importCiphertext(selection.message)
        .toResultOr { "DecryptedSelection ${selection.selectionId} message was malformed or missing" }

    val errors = getAllErrors(value, message)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        DecryptedTallyOrBallot.Selection(
            selection.selectionId,
            selection.tally,
            value.unwrap(),
            message.unwrap(),
            this.importChaumPedersenProof(selection.proof)!!
        )
    )
}

/*
private fun GroupContext.importPartialDecryption(partial: electionguard.protogen.PartialDecryption):
        Result<PartialDecryption, String> {

    if (partial.proof == null && partial.recoveredParts == null) {
        return Err("PartialDecryption ${partial.selectionId} missing both proof and recoveredParts")
    }

    val share = this.importElementModP(partial.share)
        .toResultOr { "PartialDecryption ${partial.selectionId} share was malformed or missing" }

    val proof: GenericChaumPedersenProof? =
        if (partial.proof != null) this.importChaumPedersenProof(partial.proof) else null

    val recoveredParts = partial.recoveredParts
    val (parts, perrors) =
        recoveredParts?.fragments?.map { this.importRecoveredPartialDecryption(partial.selectionId, it) }?.partition() ?: Pair(null, emptyList())

    val errors = getAllErrors(share) + perrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        PartialDecryption(
            partial.selectionId,
            partial.guardianId,
            share.unwrap(),
            proof,
            parts,
        )
    )
}

private fun GroupContext.importRecoveredPartialDecryption(selectionId: String, parts: electionguard.protogen.RecoveredPartialDecryption):
        Result<RecoveredPartialDecryption, String> {
    val share = this.importElementModP(parts.share)
        .toResultOr { "RecoveredPartialDecryption $selectionId share was malformed or missing" }
    val recoveryKey = this.importElementModP(parts.recoveryKey)
        .toResultOr { "RecoveredPartialDecryption $selectionId recoveryKey was malformed or missing" }
    val proof = this.importChaumPedersenProof(parts.proof)
        .toResultOr { "RecoveredPartialDecryption $selectionId proof was malformed or missing" }

    val errors = getAllErrors(share, recoveryKey, proof)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        RecoveredPartialDecryption(
            parts.decryptingGuardianId,
            parts.missingGuardianId,
            share.unwrap(),
            recoveryKey.unwrap(),
            proof.unwrap(),
        )
    )
} */

//////////////////////////////////////////////////////////////////////////////////////////////

fun DecryptedTallyOrBallot.publishDecryptedTallyOrBallot(): electionguard.protogen.DecryptedTallyOrBallot {
    return electionguard.protogen
        .DecryptedTallyOrBallot(this.tallyId, this.contests.values.map { it.publishContest() })
}

private fun DecryptedTallyOrBallot.Contest.publishContest(): electionguard.protogen.DecryptedContest {
    return electionguard.protogen
        .DecryptedContest(this.contestId, this.selections.values.map { it.publishSelection() })
}

private fun DecryptedTallyOrBallot.Selection.publishSelection():
        electionguard.protogen.DecryptedSelection {
    return electionguard.protogen
        .DecryptedSelection(
            this.selectionId,
            this.tally,
            this.value.publishElementModP(),
            this.message.publishCiphertext(),
            this.proof.publishChaumPedersenProof()
        )
}

/*
private fun PartialDecryption.publishPartialDecryption():
        electionguard.protogen.PartialDecryption {
    // either proof or recovered_parts is non null
    val proofOrParts: electionguard.protogen.PartialDecryption.ProofOrParts<*>?
    if (this.proof != null) {
        proofOrParts =
            electionguard.protogen
                .PartialDecryption
                .ProofOrParts
                .Proof(this.proof.publishChaumPedersenProof())
    } else if (this.recoveredDecryptions.isNotEmpty()) {
        val pparts = this.recoveredDecryptions.map { it.publishMissingPartialDecryption() }
        proofOrParts =
            electionguard.protogen
                .PartialDecryption
                .ProofOrParts
                .RecoveredParts(electionguard.protogen.RecoveredParts(pparts))
    } else {
        throw IllegalStateException(
            "CiphertextDecryptionSelection must have proof or recoveredParts"
        )
    }
    return electionguard.protogen
        .PartialDecryption(
            this.selectionId,
            this.guardianId,
            this.share().publishElementModP(),
            proofOrParts,
        )
}

private fun RecoveredPartialDecryption.publishMissingPartialDecryption():
        electionguard.protogen.RecoveredPartialDecryption {
    return electionguard.protogen
        .RecoveredPartialDecryption(
            this.decryptingGuardianId,
            this.missingGuardianId,
            this.share.publishElementModP(),
            this.recoveryKey.publishElementModP(),
            this.proof.publishChaumPedersenProof(),
        )
}
 */