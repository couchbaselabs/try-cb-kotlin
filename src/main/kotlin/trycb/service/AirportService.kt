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
import org.slf4j.LoggerFactory
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.stereotype.Component
import trycb.model.JsonMap
import trycb.model.NarratedResponse


private val log = LoggerFactory.getLogger(AirportService::class.java)

@Component
class AirportService(
    private val cluster: Cluster,
) {

    /**
     * Find all airports.
     */
    suspend fun findAll(bucket: String, params: String): NarratedResponse<List<JsonMap>> {
        var params = params
        val builder = StringBuilder()
        builder.append("SELECT airportname FROM `").append(bucket).append("`.inventory.airport WHERE ")
        val sameCase =
            params == params.uppercase() || params == params.lowercase()
        params = if (params.length == 3 && sameCase) {
            builder.append("faa = \$val")
            params.uppercase()
        } else if (params.length == 4 && sameCase) {
            builder.append("icao = \$val")
            params.uppercase()
        } else {
            // The airport name should start with the parameter value.
            builder.append("POSITION(LOWER(airportname), \$val) = 0")
            params.lowercase()
        }
        val query = builder.toString()
        logQuery(query)

        val result: QueryResult = try {
            cluster.query(
                statement = query,
                parameters = QueryParameters.named(mapOf("val" to params))
            ).execute()

        } catch (e: QueryException) {
            log.warn("Query failed with exception", e)
            throw DataRetrievalFailureException("Query error", e)
        }

        val data = result.rows.map { row -> row.contentAs<Map<String, Any?>>() }

        val queryType = "N1QL query - scoped to inventory: "
        return NarratedResponse(data, queryType, query)
    }

    /**
     * Helper method to log the executing query.
     */
    private fun logQuery(query: String) {
        log.info("Executing Query: {}", query)
    }

}
