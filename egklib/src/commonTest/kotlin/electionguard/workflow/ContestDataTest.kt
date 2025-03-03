package electionguard.workflow

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.*
import electionguard.encrypt.Encryptor
import electionguard.encrypt.cast
import electionguard.input.BallotInputBuilder
import electionguard.publish.Publisher
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import io.ktor.utils.io.core.*
import pbandk.decodeFromByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ContestDataTest {
    val input = "src/commonTest/data/workflow/allAvailableProto"
    val output = "testOut/contestData/testEncryptionWithWriteIn"
    val context = productionGroup()
    val keypair = elGamalKeyPairFromRandom(context)

    @Test
    fun testEncryptDecryptWithWriteIn() {
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!

        val builder = BallotInputBuilder(electionRecord.manifest(), "ContestDataTest").setStyle("ballotStyle")
        val ballot = builder
            .addContest(0)
            .addSelection(0, vote = 1)
            .done()
            .addContest(1)
            .addWriteIn("Sekula-Gibbs")
            .done()
            .addContest(2)
            .addWriteIn("SekulaGibbs")
            .addSelection(0, vote = 1)
            .done()
            .addContest(3)
            .addWriteIn("SekulaGibbs")
            .addWriteIn("Sekula-Gibbs")
            .addSelection(0, vote = 1)
            .addSelection(1, vote = 1)
            .done()
            .addContest(4)
            .addWriteIn("01234567890123456789012345678901234567890123456789012345678901234567890123456789")
            .done()
            .addContest(5)
            .addWriteIn("01234567890123456789012345678901234567890123456789012345678901234567890123456789")
            .addSelection(0, vote = 1)
            .done()
            .addContest(6)
            .addSelection(0, vote = 1)
            .addSelection(1, vote = 1)
            .addSelection(2, vote = 1)
            .addSelection(3, vote = 1)
            .done()
            .build()

        val encryptor = Encryptor(context, electionRecord.manifest(), keypair.publicKey, electionInit.extendedBaseHash, "device")
        val eballot = encryptor.encrypt(ballot, ByteArray(0))

        eballot.contests.forEachIndexed { idx, it ->
            assertNotNull(it.contestData)
            assertEquals(64, it.contestData.c1.size)

            val baRT = it.contestData.decryptWithSecretKey(
                keypair.publicKey,
                electionInit.extendedBaseHash,
                it.contestId,
                keypair.secretKey
            )
            assertNotNull(baRT)
            val protoRoundtrip = electionguard.protogen.ContestData.decodeFromByteArray(baRT)
            val contestDataResult = importContestData(protoRoundtrip)
            assertTrue(contestDataResult is Ok)
            val contestDataRoundtrip = contestDataResult.unwrap()

            if (idx < ballot.contests.size) {
                println(" ballot contest $idx = ${ballot.contests[idx]}")
            }
            println(" contestDataRoundtrip = $contestDataRoundtrip \n")
            when (idx) {
                0 -> {
                    assertEquals(ContestDataStatus.normal, contestDataRoundtrip.status)
                    assertEquals(emptyList(), contestDataRoundtrip.writeIns)
                }
                1 -> {
                    assertEquals(ContestDataStatus.normal, contestDataRoundtrip.status)
                    assertEquals(listOf("Sekula-Gibbs"), contestDataRoundtrip.writeIns)
                }
                2 -> {
                    assertEquals(ContestDataStatus.over_vote, contestDataRoundtrip.status)
                    assertEquals(listOf("SekulaGibbs"), contestDataRoundtrip.writeIns)
                    assertEquals(listOf(8), contestDataRoundtrip.overvotes)
                }
                3 -> {
                    assertEquals(ContestDataStatus.over_vote, contestDataRoundtrip.status)
                    assertEquals(listOf("SekulaGibbs", "Sekula-Gibbs"), contestDataRoundtrip.writeIns)
                    assertEquals(listOf(12, 13), contestDataRoundtrip.overvotes)
                }
                4 -> {
                    assertEquals(ContestDataStatus.normal, contestDataRoundtrip.status)
                    assertEquals(listOf("012345678901234567890123456789*"), contestDataRoundtrip.writeIns)
                    assertEquals(emptyList(), contestDataRoundtrip.overvotes)
                }
                5 -> {
                    assertEquals(ContestDataStatus.over_vote, contestDataRoundtrip.status)
                    assertEquals(listOf("012345678901234567890123456789*"), contestDataRoundtrip.writeIns)
                    assertEquals(listOf(20), contestDataRoundtrip.overvotes)
                }
                6 -> {
                    assertEquals(ContestDataStatus.over_vote, contestDataRoundtrip.status)
                    assertEquals(emptyList(), contestDataRoundtrip.writeIns)
                    assertEquals(listOf(24, 25, 26, 27), contestDataRoundtrip.overvotes)
                }
                else -> {
                    assertEquals(ContestDataStatus.null_vote, contestDataRoundtrip.status)
                    assertEquals(emptyList(), contestDataRoundtrip.writeIns)
                }
            }
        }

        val publisherProto = makePublisher(output, true, false)
        publisherProto.writeElectionInitialized(electionInit)
        publisherProto.encryptedBallotSink("testWriteEncryptions").use { sink ->
            sink.writeEncryptedBallot(eballot.cast())
        }

        val publisherJson = makePublisher(output + "Json", true, true)
        publisherJson.writeElectionInitialized(electionInit)
        publisherJson.encryptedBallotSink("testWriteEncryptions").use { sink ->
            sink.writeEncryptedBallot(eballot.cast())
        }
    }
}