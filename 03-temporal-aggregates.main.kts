@file:DependsOn("libs/client-0.2.1.jar")
@file:Import("scripting-engine.main.kts")

import org.tsdl.client.impl.builder.QueryPeriodImpl.period
import org.tsdl.client.impl.builder.TemporalSampleSpecificationImpl.*
import org.tsdl.client.impl.builder.YieldSpecificationImpl.samples
import org.tsdl.infrastructure.common.TsdlTimeUnit
import org.tsdl.infrastructure.common.TsdlUtil
import org.tsdl.infrastructure.model.MultipleScalarResult
import java.io.File

val queryEndpoint = "http://localhost:8080/query"
val storageName = "csv"
val serviceConfiguration = mapOf(
    "targetFile" to File("./evaluation-logs/03-temporal-aggregates.log").canonicalPath
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
            .temporalSamples(
                averageTemporal(
                    "myTemporalAvg", TsdlTimeUnit.MINUTES,
                    period("2017-09-09T10:00:00Z", "2019-09-04T10:00:00Z"),
                    period("2018-06-11T10:00:00Z", "2019-01-08T10:00:00Z"),
                    period("2019-06-27T10:00:00Z", "2019-09-12T10:00:00Z")
                ),
                countTemporal(
                    "myTemporalCount",
                    period("2017-09-09T10:00:00Z", "2019-09-04T10:00:00Z"),
                    period("2018-06-11T10:00:00Z", "2019-01-08T10:00:00Z"),
                    period("2019-06-27T10:00:00Z", "2019-09-12T10:00:00Z")
                ),
                maximumTemporal(
                    "myTemporalMax", TsdlTimeUnit.HOURS,
                    period("2017-09-09T10:00:00Z", "2019-09-04T10:00:00Z"),
                    period("2018-06-11T10:00:00Z", "2019-01-08T10:00:00Z"),
                    period("2019-06-27T10:00:00Z", "2019-09-12T10:00:00Z")
                ),
                minimumTemporal(
                    "myTemporalMin", TsdlTimeUnit.SECONDS,
                    period("2017-09-09T10:00:00Z", "2019-09-04T10:00:00Z"),
                    period("2018-06-11T10:00:00Z", "2019-01-08T10:00:00Z"),
                    period("2019-06-27T10:00:00Z", "2019-09-12T10:00:00Z")
                ),
                standardDeviationTemporal(
                    "myTemporalStdDev", TsdlTimeUnit.DAYS,
                    period("2017-09-09T10:00:00Z", "2019-09-04T10:00:00Z"),
                    period("2018-06-11T10:00:00Z", "2019-01-08T10:00:00Z"),
                    period("2019-06-27T10:00:00Z", "2019-09-12T10:00:00Z")
                ),
                sumTemporal(
                    "myTemporalSum", TsdlTimeUnit.WEEKS,
                    period("2017-09-09T10:00:00Z", "2019-09-04T10:00:00Z"),
                    period("2018-06-11T10:00:00Z", "2019-01-08T10:00:00Z"),
                    period("2019-06-27T10:00:00Z", "2019-09-12T10:00:00Z")
                )
            )
            .yield(
                samples(
                    "myTemporalAvg",
                    "myTemporalStdDev",
                    "myTemporalMin",
                    "myTemporalMax",
                    "myTemporalSum",
                    "myTemporalCount"
                )
            )
    }

    println("UC3: Temporal Aggregates\n")
    println(query)

    val (temporalAggregates, _) = engine.query<MultipleScalarResult>(
        client,
        queryEndpoint,
        storageName,
        serviceConfiguration,
        lookupConfiguration,
        transformationConfiguration,
        query
    )

    println("\n" + temporalAggregates.values().map { TsdlUtil.formatNumber(it) })
    println("-------------------------------")
}
