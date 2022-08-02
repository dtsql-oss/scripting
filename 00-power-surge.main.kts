@file:DependsOn("libs/client-0.2.1.jar")
@file:Import("scripting-engine.main.kts")

import org.tsdl.client.api.builder.EventFunctionSpecification.not
import org.tsdl.client.api.builder.Range.IntervalType.OPEN_END
import org.tsdl.client.impl.builder.EchoSpecificationImpl.echo
import org.tsdl.client.impl.builder.EventConnectiveSpecificationImpl.and
import org.tsdl.client.impl.builder.EventSpecificationImpl.event
import org.tsdl.client.impl.builder.RangeImpl.for_
import org.tsdl.client.impl.builder.SelectSpecificationImpl.follows
import org.tsdl.client.impl.builder.TemporalFilterSpecificationImpl.before
import org.tsdl.client.impl.builder.ThresholdFilterSpecificationImpl.gt
import org.tsdl.client.impl.builder.ThresholdFilterSpecificationImpl.lt
import org.tsdl.client.impl.builder.ValueSampleSpecificationImpl.average
import org.tsdl.client.impl.builder.ValueSampleSpecificationImpl.integral
import org.tsdl.client.impl.builder.YieldSpecificationImpl.*
import org.tsdl.infrastructure.common.TsdlTimeUnit
import org.tsdl.infrastructure.model.*
import java.io.File


println(
    """
      ########################################
      ######### POWER SURGE DETECTOR #########
      ########################################
      
      This sample analysis script achieves three tasks:
      (1) Filters out out irrelevant data points; only data points with values of at least 40 and before December 15th 2022 (8 o'clock) are to be considered.
      (2) Detects periods where the recorded power surges from below the average of filtered data points to above the average for at least 15, but less than 200 min.
      (3) Finally, it calculates the energy consumption during the periods that have been detected in step (2).  
    """.trimIndent()
)

val queryEndpoint = "http://localhost:8080/query"
val storageName = "csv"
val serviceConfiguration = emptyMap<String, Any>()
val lookupConfiguration = mapOf(
    "filePath" to File("./data/series2.csv").canonicalPath,
    "skipHeaders" to 0,
    "fieldSeparator" to ';'
)
val transformationConfiguration = mapOf(
    "valueColumn" to 1,
    "timeColumn" to 0,
    "timeFormat" to "yyyy-MM-dd HH:mm:ss.SSS"
)

invokeAnalysisSession { engine, client ->
    println("\nStarting analysis.")

    print("\n(1) Filtering out irrelevant data...")
    val (_, filteredTimeSeriesCache) = engine.query<TsdlDataPoints>(
        client, queryEndpoint, storageName, serviceConfiguration, lookupConfiguration, transformationConfiguration,
        engine.tsdl {
            it
                .filter(
                    and(
                        not(lt(40.0)), before("2022-12-15T08:00:00Z")
                    )
                )
                .yield(dataPoints())
        }.trimIndent()
    )
    println("done.")

    print("\n(2) Detecting periods capturing power surges...")
    val (surgePeriods, _) = engine.query<TsdlPeriodSet>(
        client, filteredTimeSeriesCache, queryEndpoint,
        engine.tsdl {
            it
                .valueSample(average("globalAverage", echo("2")))
                .events(
                    event(and(lt("globalAverage")), "low"),
                    event(and(gt("globalAverage")), for_(15L, 200L, TsdlTimeUnit.MINUTES, OPEN_END), "high")
                )
                .selection(follows("high", "low"))
                .yield(allPeriods())
        }
    )
    println(
        "done.\n  ${
            surgePeriods.periods()
                .joinToString(separator = "\n  ") { "[${it.index()}]: start: ${it.start()}, end: ${it.end()}" }
        }"
    )
    println(" ...log messages:\n  ${engine.joinLogMessagesToString(surgePeriods.logs())}")

    print("\n(3) Calculating energy consumption during detected periods...")
    val (energyConsumption, _) = engine.query<MultipleScalarResult>(
        client, filteredTimeSeriesCache, queryEndpoint,
        engine.tsdl { builder ->
            builder
                .valueSamples(
                    surgePeriods.periods().mapIndexed { i, p -> integral("powerIntegral${i + 1}", p.start(), p.end()) })
                .yield(samples((1..surgePeriods.totalPeriods()).map { "powerIntegral$it" }))
        }
    )
    print(
        "done.\n  ${
            energyConsumption.values().mapIndexed { index, value -> "[$index]: $value" }.joinToString("\n  ")
        }"
    )
}
