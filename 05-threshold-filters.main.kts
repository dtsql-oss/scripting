@file:DependsOn("libs/client-0.2.1.jar")
@file:Import("scripting-engine.main.kts")

import org.tsdl.client.api.builder.EventFunctionSpecification.not
import org.tsdl.client.impl.builder.EventConnectiveSpecificationImpl.and
import org.tsdl.client.impl.builder.ThresholdFilterSpecificationImpl.gt
import org.tsdl.client.impl.builder.ValueSampleSpecificationImpl.average
import org.tsdl.client.impl.builder.YieldSpecificationImpl.dataPoints
import org.tsdl.infrastructure.model.TsdlDataPoints
import java.io.File

val queryEndpoint = "http://localhost:8080/query"
val storageName = "csv"
val serviceConfiguration = mapOf(
    "targetFile" to File("./evaluation-logs/05-threshold-filters.log").canonicalPath
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
            .valueSamples(average("globalAvg"))
            .filter(and(gt("globalAvg"), not(gt(875.75))))
            .yield(dataPoints())
    }

    println("UC5: Threshold Filters\n")
    println(query)

    val (filteredData, _) = engine.query<TsdlDataPoints>(
        client,
        queryEndpoint,
        storageName,
        serviceConfiguration,
        lookupConfiguration,
        transformationConfiguration,
        query
    )

    println("\n" + filteredData.items().size)
    println("-------------------------------")
}
