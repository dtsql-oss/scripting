@file:DependsOn("libs/client-0.2.1.jar")
@file:Import("scripting-engine.main.kts")

import org.tsdl.client.api.builder.EventFunctionSpecification.not
import org.tsdl.client.api.builder.TsdlQueryBuilder.`as`
import org.tsdl.client.impl.builder.DeviationFilterSpecificationImpl.aroundRelative
import org.tsdl.client.impl.builder.EchoSpecificationImpl.echo
import org.tsdl.client.impl.builder.EventConnectiveSpecificationImpl.and
import org.tsdl.client.impl.builder.EventSpecificationImpl.event
import org.tsdl.client.impl.builder.TemporalFilterSpecificationImpl.after
import org.tsdl.client.impl.builder.TemporalFilterSpecificationImpl.before
import org.tsdl.client.impl.builder.ValueSampleSpecificationImpl.average
import org.tsdl.client.impl.builder.YieldSpecificationImpl.longestPeriod
import org.tsdl.infrastructure.model.TsdlPeriod
import java.io.File
import java.time.temporal.ChronoUnit

val queryEndpoint = "http://localhost:8080/query"
val storageName = "csv"
val serviceConfiguration = mapOf(
    "targetFile" to File("./evaluation-logs/08-deviation-events.log").canonicalPath
)
val lookupConfiguration = mapOf(
    "filePath" to File(args[0]).canonicalPath,
    "skipHeaders" to 1,
    "fieldSeparator" to ','
)
val transformationConfiguration = mapOf(
    "valueColumn" to 1,
    "timeColumn" to 0,
    "timeFormat" to "yyyy-MM-dd'T'HH:mm:ssXXX"
)

invokeAnalysisSession { engine, client ->
    val query = engine.tsdl {
        it
            .valueSamples(average("localAvg", "2018-06-04T10:00:00Z", "2019-01-22T10:00:00Z", echo("4")))
            .filter(and(not(before("2018-06-04T10:00:00Z")), not(after("2019-01-22T10:00:00Z"))))
            .events(event(and(aroundRelative("localAvg", 2.5)), `as`("aroundEvent")))
            .yield(longestPeriod())
    }

    println("UC8: Deviation Events\n")
    println(query)

    val (period, _) = engine.query<TsdlPeriod>(
        client,
        queryEndpoint,
        storageName,
        serviceConfiguration,
        lookupConfiguration,
        transformationConfiguration,
        query
    )

    println(
        "\n${period.index()}: ${period.start()}--${period.end()} (${
            ChronoUnit.DAYS.between(
                period.start(),
                period.end()
            )
        } days)"
    )
    println("\nlog messages:\n  ${engine.joinLogMessagesToString(period.logs())}")
    println("-------------------------------")
}
