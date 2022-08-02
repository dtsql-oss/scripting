@file:DependsOn("libs/client-0.2.1.jar")
@file:Import("scripting-engine.main.kts")

import org.tsdl.client.api.builder.EventFunctionSpecification.not
import org.tsdl.client.api.builder.Range
import org.tsdl.client.api.builder.TsdlQueryBuilder.`as`
import org.tsdl.client.impl.builder.ConstantEventSpecificationImpl.constant
import org.tsdl.client.impl.builder.EchoSpecificationImpl.echo
import org.tsdl.client.impl.builder.EventConnectiveSpecificationImpl.and
import org.tsdl.client.impl.builder.EventSpecificationImpl.event
import org.tsdl.client.impl.builder.RangeImpl.for_
import org.tsdl.client.impl.builder.RangeImpl.within
import org.tsdl.client.impl.builder.SelectSpecificationImpl.follows
import org.tsdl.client.impl.builder.SelectSpecificationImpl.precedes
import org.tsdl.client.impl.builder.TemporalFilterSpecificationImpl.after
import org.tsdl.client.impl.builder.ThresholdFilterSpecificationImpl.gt
import org.tsdl.client.impl.builder.ThresholdFilterSpecificationImpl.lt
import org.tsdl.client.impl.builder.ValueSampleSpecificationImpl.average
import org.tsdl.client.impl.builder.YieldSpecificationImpl.allPeriods
import org.tsdl.infrastructure.common.TsdlTimeUnit
import org.tsdl.infrastructure.model.TsdlPeriodSet
import java.io.File
import java.time.temporal.ChronoUnit

val queryEndpoint = "http://localhost:8080/query"
val storageName = "csv"
val serviceConfiguration = mapOf(
    "targetFile" to File("./evaluation-logs/14-nary-event-sequences.log").canonicalPath
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
            .valueSamples(average("globalAvg", echo("4")))
            .filter(and(not(after("2019-08-29T10:00:00Z"))))
            .events(
                event(and(constant(10.0, 5.0)), `as`("constantEvent")),
                event(and(lt("globalAvg")), `for`(20L, TsdlTimeUnit.DAYS, Range.IntervalType.CLOSED), `as`("low")),
                event(and(gt("globalAvg")), `for`(20L, TsdlTimeUnit.DAYS, Range.IntervalType.CLOSED), `as`("high"))
            )
            .selection(
                precedes(
                    "constantEvent",
                    follows("low", "high"),
                    within(0, 2, TsdlTimeUnit.DAYS, Range.IntervalType.CLOSED)
                )
            )
            .yield(allPeriods())
    }

    println("UC14: N-Ary Event Sequences\n")
    println(query)

    val (periods, _) = engine.query<TsdlPeriodSet>(
        client,
        queryEndpoint,
        storageName,
        serviceConfiguration,
        lookupConfiguration,
        transformationConfiguration,
        query
    )

    println(
        "\n" + periods.periods().joinToString(separator = "\n") {
            "${it.index()}: ${it.start()}--${it.end()} (${
                ChronoUnit.DAYS.between(
                    it.start(),
                    it.end()
                )
            } days)"
        })
    println("\nlog messages:\n  ${engine.joinLogMessagesToString(periods.logs())}")
    println("-------------------------------")
}

fun `for`(lowerBound: Long, upperBound: Long, unit: TsdlTimeUnit, type: Range.IntervalType) =
    for_(lowerBound, upperBound, unit, type)

fun `for`(lowerBound: Long, unit: TsdlTimeUnit, type: Range.IntervalType) =
    for_(lowerBound, type, unit)
