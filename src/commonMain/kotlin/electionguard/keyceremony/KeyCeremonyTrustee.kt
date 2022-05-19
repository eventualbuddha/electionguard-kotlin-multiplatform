package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext

class KeyCeremonyTrustee(
    val group: GroupContext,
    val id: String,
    val xCoordinate: UInt,
    val quorum: Int,
) {
    val polynomial: ElectionPolynomial

    // All of the guardians' public keys (including this one), keyed by guardian id.
    val guardianPublicKeys: MutableMap<String, PublicKeys> = mutableMapOf()

    // This guardian's share of other guardians' secret keys, keyed by designated guardian id.
    val mySecretKeyShares: MutableMap<String, SecretKeyShare> = mutableMapOf()

    // Other guardians' shares of this guardian's secret key, keyed by generating guardian id.
    val guardianSecretKeyShares: MutableMap<String, SecretKeyShare> = mutableMapOf()

    init {
        polynomial = group.generatePolynomial(id, quorum)

        // allGuardianPublicKeys include itself.
        guardianPublicKeys[id] = this.sharePublicKeys()
    }

    fun electionPublicKey(): ElementModP = polynomial.coefficientCommitments[0]

    fun sharePublicKeys(): PublicKeys {
        return PublicKeys(
            id,
            xCoordinate,
            polynomial.coefficientCommitments,
            polynomial.coefficientProofs,
        )
    }

    /** Receive publicKeys from another guardian. Return error message or empty string on success.  */
    // LOOK how do we know it came from the real guardian?
    fun receivePublicKeys(publicKeys: PublicKeys): Result<PublicKeys, String> {
        if (publicKeys.guardianXCoordinate < 1U) {
            return Err("${publicKeys.guardianId}: guardianXCoordinate must be >= 1")
        }
        if (publicKeys.coefficientCommitments.size != quorum) {
            return Err("${publicKeys.guardianId}: must have quorum ($quorum) coefficientCommitments")
        }
        if (publicKeys.coefficientProofs.size != quorum) {
            return Err("${publicKeys.guardianId}: must have quorum ($quorum) coefficientProofs")
        }
        if (!publicKeys.isValid()) {
            return Err("${publicKeys.guardianId}: has invalid proof")
        }

        guardianPublicKeys[publicKeys.guardianId] = publicKeys
        return Ok(publicKeys)
    }

    // LOOK this needs to be encrypted, see p10 spec 1.0
    fun sendSecretKeyShare(otherGuardian: String): Result<SecretKeyShare, String> {
        if (mySecretKeyShares.containsKey(otherGuardian)) {
            return Ok(mySecretKeyShares[otherGuardian]!!)
        }
        val result = generateSecretKeyShare(otherGuardian)
        if (result is Ok) {
            mySecretKeyShares[otherGuardian] = result.unwrap()
        }
        return result
    }

    private fun generateSecretKeyShare(otherGuardian: String): Result<SecretKeyShare, String> {
        val other = guardianPublicKeys[otherGuardian]
        if (other == null) {
            return Err("Trustee '$id', does not have public key for '$otherGuardian'")
        }

        // Compute my polynomial's y value at the other's x coordinate.
        val value: ElementModQ = polynomial.valueAt(group, other.guardianXCoordinate)
        return Ok(SecretKeyShare(
            id,
            otherGuardian,
            other.guardianXCoordinate,
            value,
        ))
    }

    fun receiveSecretKeyShare(result: Result<SecretKeyShare, String>): Result<SecretKeyShare, String> {
        if (result is Ok) {
            val backup = result.unwrap()
            guardianSecretKeyShares[backup.generatingGuardianId] = backup
        }
        return result
    }

}