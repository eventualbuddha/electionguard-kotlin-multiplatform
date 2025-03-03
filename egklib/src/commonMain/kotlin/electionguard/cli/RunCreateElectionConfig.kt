package electionguard.cli

import electionguard.ballot.makeElectionConfig
import electionguard.ballot.protocolVersion
import electionguard.core.productionGroup
import electionguard.publish.makePublisher
import electionguard.publish.readAndCheckManifestBytes
import io.ktor.utils.io.core.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required

/** Run Create Election Configuration CLI. */
fun createConfig(args: Array<String>) {
    val parser = ArgParser("RunCreateElectionConfig")
    val electionManifest by parser.option(
        ArgType.String,
        shortName = "manifest",
        description = "Manifest file or directory (json or protobuf)"
    ).required()
    val nguardians by parser.option(
        ArgType.Int,
        shortName = "nguardians",
        description = "number of guardians"
    ).required()
    val quorum by parser.option(
        ArgType.Int,
        shortName = "quorum",
        description = "quorum size"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output ElectionInitialized record"
    ).required()
    val createdBy by parser.option(
        ArgType.String,
        shortName = "createdBy",
        description = "who created"
    ).default("RunCreateElectionConfigurationy")
    val device by parser.option(
        ArgType.String,
        shortName = "device",
        description = "device information"
    ).default("device")
    val chainCodes by parser.option(
        ArgType.Boolean,
        shortName = "chainCodes",
        description = "chain conformation codes"
    ).default(false)
    parser.parse(args)

    println("RunCreateElectionConfig starting\n" +
            "   manifest= $electionManifest\n" +
            "   nguardians= $nguardians\n" +
            "   quorum= $quorum\n" +
            "   output = $outputDir\n" +
            "   createdBy = $createdBy\n" +
            "   device = $device"
    )

    val group = productionGroup()

    val (isJson, _, manifestBytes) = readAndCheckManifestBytes(group, electionManifest)

    // As input, either specify the input directory that contains electionConfig.protobuf file,
    // OR the election manifest, nguardians and quorum.
    val config =
        makeElectionConfig(protocolVersion,
            group.constants,
            nguardians,
            quorum,
            manifestBytes,
            chainCodes,
            device.toByteArray(),
            mapOf(
                Pair("CreatedBy", createdBy),
            ),
        )

    val publisher = makePublisher(outputDir, true, isJson)
    publisher.writeElectionConfig(config)

    println("RunCreateElectionConfig success, outputType = ${ if (isJson) "JSON" else "PROTO" }")
}
