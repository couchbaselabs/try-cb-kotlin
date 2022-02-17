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

package trycb.service

import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.Scope
import com.couchbase.client.kotlin.kv.LookupInSpec
import com.couchbase.client.kotlin.search.DisjunctionQuery
import com.couchbase.client.kotlin.search.SearchQuery
import com.couchbase.client.kotlin.search.SearchQuery.Companion.matchPhrase
import com.couchbase.client.kotlin.search.SearchResult
import com.couchbase.client.kotlin.search.execute
import org.slf4j.LoggerFactory
import org.springframework.dao.TransientDataAccessResourceException
import org.springframework.stereotype.Service
import trycb.model.HotelSummary
import trycb.model.NarratedResponse

fun matchPhraseInAnyOf(phrase: String, fields: List<String>): DisjunctionQuery =
    SearchQuery.disjunction(fields.map { matchPhrase(phrase, field = it) })

private val log = LoggerFactory.getLogger(HotelService::class.java)

@Service
class HotelService(
    val cluster: Cluster,
    val scope: Scope,
) {
    /**
     * Search for a hotel in a particular location.
     */
    suspend fun findHotels(
        location: String = "*",
        description: String = "*",
    ): NarratedResponse<List<HotelSummary>> {
        val conjuncts = mutableListOf(
            SearchQuery.term("hotel", field = "type")
        )

        if (location.isNotEmpty() && location != "*") {
            conjuncts.add(
                matchPhraseInAnyOf(
                    phrase = location,
                    fields = listOf("country", "city", "state", "address"),
                )
            )
        }

        if (description.isNotEmpty() && description != "*") {
            conjuncts.add(
                matchPhraseInAnyOf(
                    phrase = description,
                    fields = listOf("description", "name")
                )
            )
        }

        val query = SearchQuery.conjunction(conjuncts)
        log.info("Executing FTS Query: {}", query)

        val result: SearchResult = cluster.searchQuery(
            indexName = "hotels-index",
            query = query,
            limit = 100,
        ).execute()

        val queryType =
            "FTS search - scoped to: inventory.hotel within fields country, city, state, address, name, description"
        return NarratedResponse(extractResultOrThrow(result), queryType)
    }

    private val hotelSummarySpec = object : LookupInSpec() {
        val name = get("name")
        val description = get("description")
        val address = get("address")
        val city = get("city")
        val state = get("state")
        val country = get("country")
    }

    /**
     * Extract a FTS result or throw if there is an issue.
     */
    private suspend fun extractResultOrThrow(searchResult: SearchResult): List<HotelSummary> {
        with(searchResult.metadata) {
            if (metrics.failedPartitions > 0) {
                log.warn("Query returned with errors: {}", errors)
                throw TransientDataAccessResourceException("Query error: $errors")
            }
        }

        val hotelCollection = scope.collection("hotel")

        return searchResult.rows.mapNotNull { row -> getHotelSummaryOrNull(hotelCollection, row.id) }
    }

    private suspend fun getHotelSummaryOrNull(collection: Collection, hotelId: String): HotelSummary? {
        try {
            // Get just the fields defined by hotelSummarySpec.

            // An alternative to `lookupIn` would be:
            // hotelCollection.get(row.id, project = listOf(
            //     "name", "description", "address", "city", "state", "country"))

            collection.lookupIn(hotelId, hotelSummarySpec) {
                // Inside the "lookupIn" lambda, we can access
                // the content of the LookupInSpec's fields.
                with(hotelSummarySpec) {
                    val fullAddress = listOfNotNull(
                        address.contentAs<String?>(),
                        city.contentAs<String?>(),
                        state.contentAs<String?>(),
                        country.contentAs<String?>(),
                    ).joinToString()

                    return HotelSummary(
                        name = name.contentAs<String>(),
                        description = description.contentAs<String>(),
                        address = fullAddress,
                    )
                }
            }
        } catch (_: DocumentNotFoundException) {
            // Document from search results doesn't exist anymore.
            return null
        }
    }

}
