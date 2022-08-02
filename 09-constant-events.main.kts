@file:DependsOn("libs/client-0.2.1.jar")
@file:Import("scripting-engine.main.kts")

import org.tsdl.client.api.builder.TsdlQueryBuilder.`as`
import org.tsdl.client.impl.builder.ConstantEventSpecificationImpl.constant
import org.tsdl.client.impl.builder.EventConnectiveSpecificationImpl.and
import org.tsdl.client.impl.builder.EventSpecificationImpl.event
import org.tsdl.client.impl.builder.YieldSpecificationImpl.allPeriods
import org.tsdl.infrastructure.model.TsdlPeriodSet
import java.io.File
import java.time.temporal.ChronoUnit

val queryEndpoint = "http://localhost:8080/query"
val storageName = "csv"
val serviceConfiguration = mapOf(
    "targetFile" to File("./evaluation-logs/09-constant-events.log").canonicalPath
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
            .events(event(and(constant(10.0, 5.0)), `as`("constantEvent")))
            .yield(allPeriods())
    }

    println("UC9: Constant Events\n")
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
