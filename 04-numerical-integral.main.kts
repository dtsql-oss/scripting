@file:DependsOn("libs/client-0.2.1.jar")
@file:Import("scripting-engine.main.kts")

import org.tsdl.client.impl.builder.ValueSampleSpecificationImpl.integral
import org.tsdl.client.impl.builder.YieldSpecificationImpl.samples
import org.tsdl.infrastructure.common.TsdlUtil
import org.tsdl.infrastructure.model.MultipleScalarResult
import java.io.File

val queryEndpoint = "http://localhost:8080/query"
val storageName = "csv"
val serviceConfiguration = mapOf(
    "targetFile" to File("./evaluation-logs/04-numerical-integral.log").canonicalPath
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
                integral("myGlobalIntegral"),
                integral("myLocalIntegral", "2017-09-08T10:00:00Z", "2017-10-12T10:00:00Z"),
            )
            .yield(samples("myGlobalIntegral", "myLocalIntegral"))
    }

    println("UC4: Numerical Integral\n")
    println(query)

    val (integrals, _) = engine.query<MultipleScalarResult>(
        client,
        queryEndpoint,
        storageName,
        serviceConfiguration,
        lookupConfiguration,
        transformationConfiguration,
        query
    )

    println("\n" + integrals.values().map { TsdlUtil.formatNumber(it) })
    println("-------------------------------")
}
