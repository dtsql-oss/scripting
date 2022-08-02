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
    "targetFile" to File("./evaluation-logs/01-global-aggregates.log").canonicalPath
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
                average("myAvg"),
                count("myCount"),
                maximum("myMax"),
                minimum("myMin"),
                standardDeviation("myStdDev"),
                sum("mySum"),
            )
            .yield(
                samples("myAvg", "myStdDev", "myMin", "myMax", "mySum", "myCount")
            )
    }

    println("UC1: Global Aggregates\n")
    println(query)

    val (globalAggregates, _) = engine.query<MultipleScalarResult>(
        client,
        queryEndpoint,
        storageName,
        serviceConfiguration,
        lookupConfiguration,
        transformationConfiguration,
        query
    )

    println("\n" + globalAggregates.values().map { TsdlUtil.formatNumber(it) })
    println("-------------------------------")
}
