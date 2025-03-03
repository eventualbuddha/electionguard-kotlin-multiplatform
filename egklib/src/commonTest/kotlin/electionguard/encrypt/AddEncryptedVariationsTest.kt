package electionguard.encrypt

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.input.RandomBallotProvider
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class AddEncryptedVariationsTest {
    val group = productionGroup()
    val input = "src/commonTest/data/workflow/allAvailableJson"
    val outputDirProto = "testOut/encrypt/AddEncryptedUnorderedTest"

    val nballots = 3

    @Test
    fun testJustOneDevice() {
        val outputDir = "$outputDirProto/testJustOne"
        val device = "device0"

        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit,
            device,
            electionRecord.config().configBaux0,
            false,
            outputDir,
            "${outputDir}/invalidDir",
            true,
            false,
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        val cballots = mutableListOf<CiphertextBallot>()
        repeat(nballots) {
            val ballot = ballotProvider.makeBallot()
            val result = encryptor.encrypt(ballot)
            assertTrue(result is Ok)
            cballots.add(result.unwrap())
        }
        cballots.shuffle()
        cballots.forEach { encryptor.submit(it.ballotId, EncryptedBallot.BallotState.CAST) }
        encryptor.close()

        checkOutput(group, outputDir, nballots, false)
    }

    @Test
    fun testMultipleDevices() {
        val outputDir = "$outputDirProto/testMultipleDevices"

        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        repeat(3) { it ->
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit,
                "device$it",
                electionRecord.config().configBaux0,
                false,
                outputDir,
                "$outputDir/invalidDir",
                true,
                false,
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            val cballots = mutableListOf<CiphertextBallot>()
            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot)
                assertTrue(result is Ok)
                cballots.add(result.unwrap())
            }
            cballots.shuffle()
            cballots.forEach { encryptor.submit(it.ballotId, EncryptedBallot.BallotState.CAST) }
            encryptor.close()
        }

        checkOutput(group, outputDir, 3 * nballots, false)
    }

    @Test
    fun testOneWithChain() {
        val outputDir = "$outputDirProto/testOneWithChain"
        val device = "device0"

        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit,
            device,
            electionRecord.config().configBaux0,
            true,
            outputDir,
            "${outputDir}/invalidDir",
            true,
            false,
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        val cballots = mutableListOf<CiphertextBallot>()
        repeat(nballots) {
            val ballot = ballotProvider.makeBallot()
            val result = encryptor.encrypt(ballot)
            assertTrue(result is Ok)
            cballots.add(result.unwrap())
        }
        cballots.shuffle()
        cballots.forEach { encryptor.submit(it.ballotId, EncryptedBallot.BallotState.CAST) }
        encryptor.close()

        checkOutput(group, outputDir, nballots, true)
    }

    @Test
    fun testMultipleDevicesChaining() {
        val outputDir = "$outputDirProto/testMultipleDevicesChaining"

        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        repeat(3) { it ->
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit,
                "device$it",
                electionRecord.config().configBaux0,
                true,
                outputDir,
                "$outputDir/invalidDir",
                true,
                false,
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            val cballots = mutableListOf<CiphertextBallot>()
            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot)
                assertTrue(result is Ok)
                cballots.add(result.unwrap())
            }
            cballots.shuffle()
            cballots.forEach { encryptor.submit(it.ballotId, EncryptedBallot.BallotState.CAST) }
            encryptor.close()
        }

        checkOutput(group, outputDir, 3 * nballots, true)
    }

    @Test
    fun testCallMultipleTimes() {
        val outputDir = "$outputDirProto/testCallMultipleTimes"
        testMultipleCalls(outputDir, true, true, false)
        testMultipleCalls(outputDir, false, true, false)
        testMultipleCalls(outputDir, true, false, false)
        testMultipleCalls(outputDir, false, false, false)
        testMultipleCalls(outputDir, true, true, true)
        testMultipleCalls(outputDir, false, true, true)
        testMultipleCalls(outputDir, true, false, true)
        testMultipleCalls(outputDir, false, false, true)
    }

    fun testMultipleCalls(outputDir: String, shuffle: Boolean, skip: Boolean, chained: Boolean) {
        val device = "deviceM"

        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        println("shuffle=$shuffle skip=$skip chained=$chained")
        repeat(3) {
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit,
                device,
                electionRecord.config().configBaux0,
                chained,
                outputDir,
                "${outputDir}/invalidDir",
                true,
                false,
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            val cballots = mutableListOf<CiphertextBallot>()
            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot)
                assertTrue(result is Ok)
                cballots.add(result.unwrap())
            }
            if (shuffle) {
                cballots.shuffle()
            }

            cballots.forEach {
                val random = Random.nextInt(10)
                val state = if (random < 6) EncryptedBallot.BallotState.SPOILED else EncryptedBallot.BallotState.CAST
                val skip = skip && random < 2 // skip 2 in 10
                println(" skip $skip state $state")
                if (!skip) {
                    encryptor.submit(it.ballotId, state)
                }
            }
            encryptor.close()
        }

        checkOutput(group, outputDir, 3 * nballots, chained)
    }
}