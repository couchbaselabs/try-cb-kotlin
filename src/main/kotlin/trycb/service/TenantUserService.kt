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
import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Scope
import com.couchbase.client.kotlin.kv.Expiry
import com.couchbase.client.kotlin.kv.GetResult
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import trycb.model.JsonMap
import trycb.model.NarratedResponse
import trycb.util.jsonMapper
import trycb.util.toMap
import java.util.*


private const val USERS_COLLECTION_NAME = "users"
private const val BOOKINGS_COLLECTION_NAME = "bookings"

data class LoginResponse(val token: String)

@Service
class TenantUserService(
    private val jwtService: TokenService,
    private val bucket: Bucket,
) {
    /**
     * Try to log the given tenant user in.
     */
    suspend fun login(
        tenant: String,
        username: String,
        password: String,
    ): NarratedResponse<LoginResponse> {
        val scope = bucket.scope(tenant)
        val collection = scope.collection(USERS_COLLECTION_NAME)
        val doc: GetResult = try {
            collection.get(username)
        } catch (ex: DocumentNotFoundException) {
            throw AuthenticationCredentialsNotFoundException("Bad Username or Password")
        }
        val storedHash = doc.contentAs<ObjectNode>()
            .path("password")
            .textValue()

        // todo is this really the right exception to throw?
        if (!BCrypt.checkpw(password, storedHash))
            throw AuthenticationCredentialsNotFoundException("Bad Username or Password")

        return NarratedResponse(
            LoginResponse(jwtService.buildToken(username)),
            "KV get - scoped to ${scope.name}.$USERS_COLLECTION_NAME: for password field in document $username",
        )
    }

    /**
     * Create a tenant user.
     */
    suspend fun createLogin(
        tenant: String,
        username: String,
        password: String,
        expiry: Expiry,
    ): NarratedResponse<LoginResponse> {
        val scope = bucket.scope(tenant)
        val collection = scope.collection(USERS_COLLECTION_NAME)

        try {
            collection.insert(
                id = username,
                content = mapOf(
                    "type" to "user",
                    "name" to username,
                    "password" to BCrypt.hashpw(password, BCrypt.gensalt()),
                ),
                expiry = expiry,
            )

            return NarratedResponse(
                LoginResponse(jwtService.buildToken(username)),
                "KV insert - scoped to ${scope.name}.$USERS_COLLECTION_NAME: document $username"
            )

        } catch (e: Exception) {
            e.printStackTrace()
            throw AuthenticationServiceException("There was an error creating account")
        }
    }

    /*
     * Register a flight (or flights) for the given tenant user.
     */
    suspend fun registerFlightForUser(
        tenant: String,
        username: String,
        newFlights: ArrayNode,
    ): NarratedResponse<JsonMap> {
        val userDataFetch: GetResult
        val scope: Scope = bucket.scope(tenant)
        val usersCollection = scope.collection(USERS_COLLECTION_NAME)
        val bookingsCollection = scope.collection(BOOKINGS_COLLECTION_NAME)

        userDataFetch = try {
            usersCollection.get(username)
        } catch (ex: DocumentNotFoundException) {
            throw IllegalStateException(ex) // todo better exception?
        }
        val userData = userDataFetch.contentAs<ObjectNode>()

        val added = mutableListOf<JsonMap>()
        val allBookedFlights = userData.get("flights") as? ArrayNode ?: jsonMapper.createArrayNode()

        for (newFlight in newFlights) {
            val t = checkFlight(newFlight)
            t.put("bookedon", "try-cb-kotlin")
            val flightId = UUID.randomUUID().toString()

            bookingsCollection.insert(flightId, t)

            allBookedFlights.add(flightId)
            added.add(t.toMap())
        }
        userData.replace("flights", allBookedFlights)

        // todo replace with CAS!!!!!
        usersCollection.upsert(username, userData)


        return NarratedResponse(
            mapOf("added" to added),
            "KV update - scoped to ${scope.name}.$USERS_COLLECTION_NAME: for bookings field in document $username",
        )
    }

    suspend fun getFlightsForUser(
        tenant: String,
        username: String,
    ): NarratedResponse<List<JsonMap>> {
        val userDoc: GetResult
        val scope: Scope = bucket.scope(tenant)
        val usersCollection = scope.collection(USERS_COLLECTION_NAME)
        val bookingsCollection = scope.collection(BOOKINGS_COLLECTION_NAME)
        userDoc = try {
            usersCollection.get(username)
        } catch (ex: DocumentNotFoundException) {
            return NarratedResponse(emptyList())
        }

        val userData = userDoc.contentAs<ObjectNode>()
        val flights = userData.get("flights") as? ArrayNode ?: return NarratedResponse(emptyList())

        // The "flights" array contains flight ids. Convert them to actual objects.
        val results = mutableListOf<JsonMap>()
        for (i in 0 until flights.size()) {
            val flightId: String = flights.get(i).textValue()
            val res: GetResult = try {
                bookingsCollection.get(flightId)
            } catch (ex: DocumentNotFoundException) {
                throw RuntimeException("Unable to retrieve flight id $flightId")
            }

            results.add(res.contentAs<JsonMap>())
        }

        val queryType =
            "KV get - scoped to ${scope.name}.$USERS_COLLECTION_NAME: for ${results.size} bookings in document $username"
        return NarratedResponse(results, queryType)
    }

    private fun checkFlight(flight: JsonNode): ObjectNode {
        require(flight is ObjectNode) { "Each flight must be a JSON Object" }
        require(flight.has("name") && flight.has("date") && flight.has("sourceairport") && flight.has("destinationairport")) {
            "Malformed flight inside flights payload $flight"
        }
        return flight
    }
}
