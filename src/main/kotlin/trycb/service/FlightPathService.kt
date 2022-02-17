/*
 * MIT License
 *
 * Copyright (c) 2022 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

@file:OptIn(VolatileCouchbaseApi::class)

package trycb.service

import com.couchbase.client.core.error.QueryException
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.annotations.VolatileCouchbaseApi
import com.couchbase.client.kotlin.query.QueryParameters
import com.couchbase.client.kotlin.query.QueryResult
import com.couchbase.client.kotlin.query.execute
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.stereotype.Service
import trycb.model.JsonMap
import trycb.model.NarratedResponse
import trycb.util.toMap
import java.time.LocalDate
import java.util.*
import kotlin.math.ceil
import kotlin.random.Random

private val log = LoggerFactory.getLogger(FlightPathService::class.java)

private fun param(name: String) = "\$$name"

@Service
class FlightPathService(private val cluster: Cluster) {
    suspend fun findAll(
        bucket: String,
        from: String,
        to: String,
        leave: LocalDate,
    ): NarratedResponse<List<JsonMap>> {
        val unionQuery = """    
            SELECT faa as fromAirport
            FROM `$bucket`.inventory.airport
            WHERE airportname = ${param("from")}
            UNION
            SELECT faa as toAirport
            FROM `$bucket`.inventory.airport
            WHERE airportname = ${param("to")}
            """.trimIndent().replace("\n", " ")

        logQuery(unionQuery)

        val result: QueryResult = try {
            cluster.query(
                statement = unionQuery,
                parameters = QueryParameters.named("from" to from, "to" to to)
            ).execute()

        } catch (e: QueryException) {
            log.warn("Query failed with exception: $e")
            throw DataRetrievalFailureException("Query failed", e);
        }
        val rows: List<JsonMap> = result.rows.map { it.contentAs<JsonMap>() }

        println(rows)

        var fromAirport: String? = null
        var toAirport: String? = null
        for (row in rows) {
            if (row.containsKey("fromAirport")) {
                fromAirport = row["fromAirport"] as String
            }
            if (row.containsKey("toAirport")) {
                toAirport = row["toAirport"] as String
            }
        }
        val joinQuery = """
            SELECT a.name, s.flight, s.utc, r.sourceairport, r.destinationairport, r.equipment
            FROM `$bucket`.inventory.route AS r
            UNNEST r.schedule AS s
            JOIN `$bucket`.inventory.airline AS a ON KEYS r.airlineid
            WHERE r.sourceairport = ? and r.destinationairport = ?
            AND s.day = ?
            ORDER BY a.name ASC
            """.trimIndent().replace("\n", " ")

        logQuery(joinQuery)

        val leaveDay = leave.dayOfWeek.value - 1

        val otherResult: QueryResult = try {
            cluster.query(
                statement = joinQuery,
                parameters = QueryParameters.positional(listOf(
                    fromAirport, toAirport, leaveDay
                ))
            ).execute()

        } catch (e: QueryException) {
            log.warn("Query failed with exception: $e")
            throw DataRetrievalFailureException("Query error", e)
        }

        val resultRows: List<ObjectNode> = otherResult.rows.map { it.contentAs<ObjectNode>() }

        val data: MutableList<JsonMap> = LinkedList()
        for (row in resultRows) {
            val flightTime = Random.Default.nextInt(8000)
            row.put("flighttime", flightTime)
            row.put("price", ceil(flightTime.toDouble() / 8 * 100) / 100)
            data.add(row.toMap())
        }
        val queryType = "N1QL query - scoped to inventory: "
        return NarratedResponse(data, queryType, unionQuery, joinQuery)
    }

    private fun logQuery(query: String) =
        log.info("Executing Query: {}", query)
}
