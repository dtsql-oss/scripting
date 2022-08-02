@file:DependsOn("libs/client-0.2.1.jar")

import org.tsdl.client.api.TsdlClient
import org.tsdl.client.api.builder.TsdlQueryBuilder
import org.tsdl.client.impl.csv.CsvSerializingQueryClientSpecification
import org.tsdl.client.impl.csv.CsvSerializingTsdlClient
import org.tsdl.client.impl.csv.CsvSerializingTsdlClientResult
import org.tsdl.client.util.TsdlClientIoException
import org.tsdl.client.util.TsdlClientServiceException
import org.tsdl.infrastructure.dto.QueryDto
import org.tsdl.infrastructure.dto.StorageDto
import org.tsdl.infrastructure.model.QueryResult
import org.tsdl.infrastructure.model.TsdlLogEvent
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.deleteIfExists

val cacheFiles = mutableListOf<String>()

fun invokeAnalysisSession(
    timeout: Long = 15,
    unit: TimeUnit = TimeUnit.MINUTES,
    analysis: (Scripting_engine_main, TsdlClient) -> Unit
) {
    if (cacheFiles.isNotEmpty()) {
        throw TsdlScriptingException("An analysis session is already running. Invoking another one is only valid as soon as it is finished.")
    }

    try {
        val client: TsdlClient = CsvSerializingTsdlClient(timeout, unit)
        analysis(this, client)
    } finally {
        cacheFiles.forEach { Path.of(it).deleteIfExists() }
        cacheFiles.clear()
    }
}

inline fun <reified T : QueryResult> query(
    client: TsdlClient,
    queryEndpoint: String,
    storageName: String,
    serviceConfiguration: Map<String, Any>,
    lookupConfiguration: Map<String, Any>,
    transformationConfiguration: Map<String, Any>,
    tsdlQuery: String,
): TsdlScriptingResult<T> {
    val queryDto: QueryDto = QueryDto.builder()
        .tsdlQuery(tsdlQuery)
        .storage(
            StorageDto.builder()
                .name(storageName)
                .serviceConfiguration(serviceConfiguration)
                .lookupConfiguration(lookupConfiguration)
                .transformationConfiguration(transformationConfiguration)
                .build()
        )
        .build()

    val querySpecification = CsvSerializingQueryClientSpecification(queryDto, queryEndpoint)
    return safeQuery { client.query(querySpecification) as CsvSerializingTsdlClientResult }
}

inline fun <reified T : QueryResult> query(
    client: TsdlClient,
    cacheFile: String,
    queryEndpoint: String,
    tsdlQuery: String
): TsdlScriptingResult<T> =
    safeQuery { client.query(cacheFile, queryEndpoint, tsdlQuery) as CsvSerializingTsdlClientResult }

inline fun <reified T : QueryResult> safeQuery(body: () -> CsvSerializingTsdlClientResult): TsdlScriptingResult<T> {
    return try {
        val result = body()
        cacheFiles.add(result.resultCacheFilePath)
        when (val queryResult = result.queryResult) {
            is T -> TsdlScriptingResult(queryResult, result.resultCacheFilePath)
            else -> throw IllegalArgumentException("Requested result type '${T::class.simpleName}' is incompatible with actual result type '${queryResult.javaClass.simpleName}'.")
        }
    } catch (e: TsdlClientServiceException) {
        if (e.errorTrace() != null) {
            val errorTrace = e.errorTrace().entries.joinToString(separator = "\n") { "[${it.key}]: ${it.value}" }
            throw TsdlScriptingException("Query execution failed during processing or evaluation:\n$errorTrace")
        } else {
            throw TsdlScriptingException("Query execution failed due to an unknown HTTP error. Details:\n${e.errorBody()}")
        }
    } catch (e: TsdlClientIoException) {
        throw TsdlScriptingException(
            """
        Query execution failed due to an I/O error. Details:
        [0]: ${e.message}
        ${if (e.cause != null) "[1]: ${e.cause!!.javaClass.simpleName}: ${e.cause!!.message}" else ""}
      """.trimIndent()
        )
    } catch (e: Exception) {
        throw TsdlScriptingException("Query execution failed. Details:\n${e.javaClass.simpleName}: ${e.message}")
    }
}

fun joinLogMessagesToString(logs: List<TsdlLogEvent>, separator: String = "\n  ") =
    logs
        .map { "${it.dateTime()}: ${it.message()}" }
        .withIndex()
        .joinToString(separator) { (index, value) -> "[$index]: $value" }

fun tsdl(builder: (TsdlQueryBuilder) -> TsdlQueryBuilder): String = builder(TsdlQueryBuilder.instance()).build()

data class TsdlScriptingResult<out T : QueryResult>(val queryResult: T, val cachePath: String)

class TsdlScriptingException(message: String) : Exception(message)
