@file:DependsOn("libs/client-0.2.1.jar")
@file:Import("scripting-engine.main.kts")

import org.tsdl.client.api.builder.Range.IntervalType
import org.tsdl.client.api.builder.TsdlQueryBuilder.`as`
import org.tsdl.client.impl.builder.ConstantEventSpecificationImpl
import org.tsdl.client.impl.builder.EventConnectiveSpecificationImpl.and
import org.tsdl.client.impl.builder.EventSpecificationImpl.event
import org.tsdl.client.impl.builder.IncreaseEventSpecificationImpl
import org.tsdl.client.impl.builder.RangeImpl.for_
import org.tsdl.client.impl.builder.RangeImpl.within
import org.tsdl.client.impl.builder.SelectSpecificationImpl.precedes
import org.tsdl.client.impl.builder.YieldSpecificationImpl.allPeriods
import org.tsdl.infrastructure.common.TsdlTimeUnit
import org.tsdl.infrastructure.model.TsdlPeriodSet
import java.io.File
import java.time.temporal.ChronoUnit

val queryEndpoint = "http://localhost:8080/query"
val storageName = "csv"
val serviceConfiguration = mapOf(
    "targetFile" to File("./evaluation-logs/13-time-tolernace-for-event-sequences.log").canonicalPath
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
            .events(
                event(
                    and(ConstantEventSpecificationImpl.constant(10.0, 5.0)), `as`("constantEvent")
                ),
                event(
                    and(IncreaseEventSpecificationImpl.increase(5.25, 25.5, 600.0)),
                    `for`(2L, 5L, TsdlTimeUnit.WEEKS, IntervalType.OPEN_START),
                    `as`("increaseEvent")
                )
            )
            .selection(
                precedes(
                    "constantEvent",
                    "increaseEvent",
                    within(30L, 100L, TsdlTimeUnit.DAYS, IntervalType.CLOSED)
                )
            )
            .yield(allPeriods())
    }

    println("UC13: Time Tolerance for Event Sequences\n")
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
    println("-------------------------------")
}

fun `for`(lowerBound: Long, upperBound: Long, unit: TsdlTimeUnit, type: IntervalType) =
    for_(lowerBound, upperBound, unit, type)

fun `for`(lowerBound: Long, unit: TsdlTimeUnit, type: IntervalType) =
    for_(lowerBound, type, unit)
