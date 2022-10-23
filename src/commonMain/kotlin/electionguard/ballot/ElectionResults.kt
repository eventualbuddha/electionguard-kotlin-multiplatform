package electionguard.ballot

import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.UInt256

data class TallyResult(
    val group: GroupContext,
    val electionInitialized: ElectionInitialized,
    val encryptedTally: EncryptedTally,
    val ballotIds: List<String>,
    val tallyIds: List<String>,
    val metadata: Map<String, String> = emptyMap(),
) {
    fun jointPublicKey(): ElGamalPublicKey {
        return ElGamalPublicKey(electionInitialized.jointPublicKey)
    }
    fun cryptoExtendedBaseHash(): UInt256 {
        return electionInitialized.cryptoExtendedBaseHash
    }
    fun numberOfGuardians(): Int {
        return electionInitialized.config.numberOfGuardians
    }
    fun quorum(): Int {
        return electionInitialized.config.quorum
    }
}

data class DecryptionResult(
    val tallyResult: TallyResult,
    val decryptedTally: DecryptedTallyOrBallot,
    val lagrangeCoordinates: List<LagrangeCoordinate>,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(lagrangeCoordinates.isNotEmpty())
        require(lagrangeCoordinates.size >= tallyResult.quorum())
    }
    fun numberOfGuardians(): Int {
        return tallyResult.numberOfGuardians()
    }
    fun quorum(): Int {
        return tallyResult.quorum()
    }
}

data class LagrangeCoordinate(
    var guardianId: String,
    var xCoordinate: Int,
    var lagrangeCoefficient: ElementModQ, // wℓ
) {
    init {
        require(guardianId.isNotEmpty())
        require(xCoordinate > 0)
    }
}