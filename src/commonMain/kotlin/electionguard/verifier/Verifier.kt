package electionguard.verifier

import com.github.michaelbull.result.*
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.*
import electionguard.publish.ElectionRecord

// since there's no verification spec 2.0, this is approximate
class Verifier(val record: ElectionRecord, val nthreads: Int = 11) {
    val group: GroupContext
    val manifest: Manifest
    val jointPublicKey: ElGamalPublicKey
    val cryptoExtendedBaseHash: ElementModQ

    init {
        if (record.stage() < ElectionRecord.Stage.INIT) {
            throw IllegalStateException("election record stage = ${record.stage()}, not initialized\n")
        }
        group = productionGroup()
        jointPublicKey = ElGamalPublicKey(record.jointPublicKey()!!)
        cryptoExtendedBaseHash = record.cryptoExtendedBaseHash()!!.toElementModQ(group)
        manifest = record.manifest()
    }

    fun verify(showTime : Boolean = false): Boolean {
        println("Verify election record in = ${record.topdir()}\n")
        val starting23 = getSystemTimeInMillis()

        if (record.stage() < ElectionRecord.Stage.INIT) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            return false
        }

        val guardiansOk = verifyGuardianPublicKey()
        println(" 2. verifyGuardianPublicKeys= $guardiansOk")

        val publicKeyOk = verifyElectionPublicKey(record.cryptoBaseHash()!!)
        println(" 3. verifyElectionPublicKey= $publicKeyOk")

        if (record.stage() < ElectionRecord.Stage.ENCRYPTED) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            return true
        }
        val took = getSystemTimeInMillis() - starting23
        if (showTime) println("   verify 2,3 took $took millisecs")

        // encryption and vote limits 4, 5
        val verifyBallots = VerifyEncryptedBallots(group, manifest, jointPublicKey, cryptoExtendedBaseHash, nthreads)
        // Note we are validating all ballots, not just CAST
        val ballotStats = verifyBallots.verify(record.encryptedBallots { true }, showTime)
        println(" 4,5,6. verifySelectionEncryptions, contestVoteLimits $ballotStats")

        if (record.stage() < ElectionRecord.Stage.TALLIED) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            return true
        }

        // tally accumulation
        val verifyAggregation = VerifyAggregation(group, verifyBallots.aggregator)
        val encryptedTally = record.encryptedTally()!!
        val aggResult = verifyAggregation.verify(encryptedTally, showTime)
        println(" 7. verifyAggregation $aggResult")

        if (record.stage() < ElectionRecord.Stage.DECRYPTED) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            return true
        }

        // decryption
        val decryptedTally = record.decryptedTally()!!
        val verifyTally = VerifyDecryptedTally(group, manifest, jointPublicKey, cryptoExtendedBaseHash, record.guardians())
        val tallyStats = verifyTally.verifyDecryptedTally(decryptedTally, showTime)
        println(" 8,9,11. verifyDecryptedTally $tallyStats")

        // box 10
        val decryptingGuardians = record.decryptingGuardians()
        if (decryptingGuardians.size == record.numberOfGuardians()) {
            println(" 10. Correctness of Replacement Partial Decryptions not needed since there are no missing guardians")
        } else {
            val pdvStats = VerifyRecoveredShares(group, record).verify(showTime)
            println(" 10. Correctness of Replacement Partial Decryptions $pdvStats")
        }

        //  LOOK we think spoiled ballots are the same as tallies. Need to look harder at sections 12-19
        val spoiledStats =
            verifyTally.verifySpoiledBallotTallies(record.spoiledBallotTallies(), nthreads, showTime)
        println(" 12. verifySpoiledBallotTallies $spoiledStats")

        val allOk = (guardiansOk is Ok) && (publicKeyOk is Ok) && (aggResult is Ok) && ballotStats.allOk() && tallyStats.allOk && spoiledStats.allOk()
        println("verify allOK = $allOk")
        return allOk
    }

    // Verification Box 2
    private fun verifyGuardianPublicKey(): Result<Boolean, String> {
        val checkProofs: MutableList<Result<Boolean, String>> = mutableListOf()
        for (guardian in this.record.guardians()) {
            guardian.coefficientProofs.forEachIndexed { index, proof ->
                if (!proof.isValid()) {
                    checkProofs.add(Err("  2.A Guardian ${guardian.guardianId} has invalid proof for coefficient $index"))
                } else {
                    checkProofs.add(Ok(true))
                }
            }
        }
        return checkProofs.merge()
    }

    // Verification Box 3
    private fun verifyElectionPublicKey(cryptoBaseHash: UInt256): Result<Boolean, String> {
        val jointPublicKeyComputed = this.record.guardians().map { it.publicKey() }.reduce { a, b -> a * b }
        val errors = mutableListOf<Result<Boolean, String>>()
        if (!jointPublicKey.equals(jointPublicKeyComputed)) {
            errors.add(Err("  3.A jointPublicKey K does not equal computed K = Prod(K_i)"))
        }

        val commitments = mutableListOf<ElementModP>()
        this.record.guardians().forEach { commitments.addAll(it.coefficientCommitments()) }
        val commitmentsHash = hashElements(commitments)
        // spec 1.52, eq 17 and 3.B
        val computedQbar: UInt256 = hashElements(cryptoBaseHash, jointPublicKeyComputed, commitmentsHash)
        if (!cryptoExtendedBaseHash.equals(computedQbar.toElementModQ(group))) {
            errors.add(Err("  3.B qbar does not match computed = H(Q, K, Prod(K_ij))"))
        }

        return errors.merge()
    }

    fun verifyEncryptedBallots(): StatsAccum {
        val verifyBallots = VerifyEncryptedBallots(group, manifest, jointPublicKey, cryptoExtendedBaseHash, nthreads)
        return verifyBallots.verify(record.encryptedBallots { true })
    }

    fun verifyEncryptedBallots(ballots: Iterable<EncryptedBallot>): StatsAccum {
        val verifyBallots = VerifyEncryptedBallots(group, manifest, jointPublicKey, cryptoExtendedBaseHash, nthreads)
        return verifyBallots.verify(ballots)
    }

    fun verifyDecryptedTally(tally: DecryptedTallyOrBallot): Stats {
        val verifyTally = VerifyDecryptedTally(group, manifest, jointPublicKey, cryptoExtendedBaseHash, record.guardians())
        return verifyTally.verifyDecryptedTally(tally)
    }

    fun verifyRecoveredShares(): Result<Boolean, String> {
        val verifier = VerifyRecoveredShares(group, record)
        return verifier.verify()
    }

    fun verifySpoiledBallotTallies(): StatsAccum {
        val verifyTally = VerifyDecryptedTally(group, manifest, jointPublicKey, cryptoExtendedBaseHash, record.guardians())
        return verifyTally.verifySpoiledBallotTallies(record.spoiledBallotTallies(), nthreads)
    }

}

class Stats(
    val forWho: String,
    val allOk: Boolean,
    val ncontests: Int,
    val nselections: Int,
    val errors: List<String>,
    val nshares: Int = 0,
) {
    override fun toString(): String {
        return "$forWho allOk=$allOk, ncontests=$ncontests, nselections=$nselections, nshares=$nshares, errors=${errors.joinToString("\n")}"
    }
}

class StatsAccum {
    var n: Int = 0
    val errors = mutableListOf<String>()

    private var ncontests: Int = 0
    private var nselections: Int = 0
    private var nshares: Int = 0

    fun add(stat: Stats) {
        n++
        ncontests += stat.ncontests
        nselections += stat.nselections
        nshares += stat.nshares
        if (stat.errors.isNotEmpty()) {
            errors.addAll(stat.errors)
        }
    }

    fun add(errs: List<String>) {
        if (errs.isNotEmpty()) {
            errors.addAll(errs)
        }
    }

    fun allOk() = errors.isEmpty()

    override fun toString(): String {
        return "allOk=${allOk()}, n=$n, ncontests=$ncontests, nselections=$nselections, nshares=$nshares, errors=${errors.joinToString("\n")}"
    }
}


