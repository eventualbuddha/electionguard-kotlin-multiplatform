package electionguard.protoconvert

import com.github.michaelbull.result.*
import electionguard.ballot.DecryptionResult
import electionguard.ballot.EncryptedBallotChain
import electionguard.ballot.TallyResult
import pbandk.ByteArr

fun electionguard.protogen.EncryptedBallotChain.import(): Result<EncryptedBallotChain, String> {

    return Ok(EncryptedBallotChain(
        this.encryptingDevice,
        this.baux0.array,
        this.ballotIds,
        importUInt256(this.lastConfirmationCode)!!,
        this.chaining,
        importUInt256(this.closingHash),
        this.metadata.associate { it.key to it.value }
    ))
}

////////////////////////////////////////////////////////

fun EncryptedBallotChain.publishProto() =
    electionguard.protogen.EncryptedBallotChain(
        this.encryptingDevice,
        ByteArr(this.baux0),
        this.ballotIds,
        this.lastConfirmationCode.publishProto(),
        this.chaining,
        this.closingHash?.publishProto(),
        this.metadata.entries.map { electionguard.protogen.EncryptedBallotChain.MetadataEntry(it.key, it.value) }
    )

fun TallyResult.publishProto() =
    electionguard.protogen.TallyResult(
        this.electionInitialized.publishProto(),
        this.encryptedTally.publishProto(),
        this.tallyIds,
        this.metadata.entries.map { electionguard.protogen.TallyResult.MetadataEntry(it.key, it.value) }
    )

fun DecryptionResult.publishProto() =
    electionguard.protogen.DecryptionResult(
        this.tallyResult.publishProto(),
        this.decryptedTally.publishProto(),
        this.metadata.entries.map { electionguard.protogen.DecryptionResult.MetadataEntry(it.key, it.value) }
    )