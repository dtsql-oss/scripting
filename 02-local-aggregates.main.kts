@file:DependsOn("libs/client-0.2.1.jar")
@file:Import("scripting-engine.main.kts")

import org.tsdl.client.impl.builder.ValueSampleSpecificationImpl.*
import org.tsdl.client.impl.builder.YieldSpecificationImpl.samples
import org.tsdl.infrastructure.common.TsdlUtil
import org.tsdl.infrastructure.model.MultipleScalarResult
import java.io.File

val queryEndpoint = "http://localhost:8080/query"
val storageName = "csv"
val serviceConfiguration = mapOf(
    "targetFile" to File("./evaluation-logs/02-local-aggregates.log").canonicalPath
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
            .valueSamples(
                average("myLocalAvg", "2017-09-09T10:00:00Z", null),
                count("myLocalCount", null, "2019-09-04T10:00:00Z"),
                maximum("myLocalMax", "2017-09-09T10:00:00Z", "2019-09-04T10:00:00Z"),
                minimum("myLocalMin", "2017-09-09T10:00:00Z", "2019-09-04T10:00:00Z"),
                standardDeviation("myLocalStdDev", "2017-09-09T10:00:00Z", "2019-09-04T10:00:00Z"),
                sum("myLocalSum", "2017-09-09T10:00:00Z", "2019-09-04T10:00:00Z"),
            )
            .yield(
                samples("myLocalAvg", "myLocalStdDev", "myLocalMin", "myLocalMax", "myLocalSum", "myLocalCount")
            )
    }

    println("UC2: Local Aggregates\n")
    println(query)

    val (localAggregates, _) = engine.query<MultipleScalarResult>(
        client,
        queryEndpoint,
        storageName,
        serviceConfiguration,
        lookupConfiguration,
        transformationConfiguration,
        query
    )

    println("\n" + localAggregates.values().map { TsdlUtil.formatNumber(it) })
    println("-------------------------------")
}
