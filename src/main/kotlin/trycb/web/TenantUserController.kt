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

package trycb.web

import com.couchbase.client.kotlin.kv.Expiry
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.server.ResponseStatusException
import trycb.model.JsonMap
import trycb.model.NarratedResponse
import trycb.service.LoginResponse
import trycb.service.TenantUserService
import trycb.service.TokenService
import trycb.util.jsonMapper
import kotlin.time.Duration.Companion.seconds


private val log = LoggerFactory.getLogger(TenantUserController::class.java)

@RestController
@RequestMapping("/api/tenants")
class TenantUserController(
    private val tenantUserService: TenantUserService,
    private val jwtService: TokenService,
    @Value("\${storage.expiry:0}") expirySeconds: Int = 0,
) {
    private val expiry =
        if (expirySeconds == 0) Expiry.none()
        else Expiry.of(expirySeconds.seconds)

    @RequestMapping(
        value = ["/{tenant}/user/login"],
        method = [RequestMethod.POST],
    )
    suspend fun login(
        @PathVariable("tenant") tenant: String,
        @RequestBody loginInfo: Map<String, String>,
    ): NarratedResponse<LoginResponse> {
        val user = loginInfo["user"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "missing 'user'")
        val password = loginInfo["password"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "missing 'password'")

        try {
            return tenantUserService.login(tenant, user, password)
        } catch (e: AuthenticationException) {
            log.error("Authentication failed with exception", e)
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, e.message, e)
        }
    }

    @RequestMapping(
        value = ["/{tenant}/user/signup"],
        method = [RequestMethod.POST]
    )
    suspend fun createLogin(
        @PathVariable("tenant") tenant: String,
        @RequestBody loginInfo: Map<String, String>,
    ): ResponseEntity<NarratedResponse<LoginResponse>> {
        val username = loginInfo["user"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "missing 'user'")
        val password = loginInfo["password"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "missing 'password'")

        return try {
            val result = tenantUserService.createLogin(
                tenant = tenant,
                username = username,
                password = password,
                expiry = expiry,
            )

            ResponseEntity.status(HttpStatus.CREATED).body(result)
        } catch (e: AuthenticationServiceException) {
            log.error("Authentication failed with exception", e)
            throw ResponseStatusException(HttpStatus.CONFLICT, e.message, e)
        }
    }

    private fun checkAuthorization(tenant: String, username: String, authorizationHeader: String?) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer Authentication must be used")
        }

        try {
            // TODO include tenant in token, right?
            jwtService.verifyAuthenticationHeader(authorization = authorizationHeader, expectedUsername = username)
        } catch (e: BadCredentialsException) {
            log.error("auth check failed", e)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Bad credentials.")
        }
    }

    @RequestMapping(
        value = ["/{tenant}/user/{username}/flights"],
        method = [RequestMethod.PUT],
    )
    suspend fun book(
        @PathVariable("tenant") tenant: String,
        @PathVariable("username") username: String,
        @RequestBody json: String,
        @RequestHeader("Authorization") authentication: String?,
    ): NarratedResponse<JsonMap> {
        // checking auth on a per-method like this is not recommended in a production application
        checkAuthorization(tenant = tenant, username = username, authorizationHeader = authentication)

        val jsonData = kotlin.runCatching { jsonMapper.readTree(json) as ObjectNode }
            .getOrElse { throw ResponseStatusException(HttpStatus.BAD_REQUEST, it.message) }

        return tenantUserService.registerFlightForUser(tenant, username, jsonData.get("flights") as ArrayNode)
    }

    @RequestMapping(
        value = ["/{tenant}/user/{username}/flights"],
        method = [RequestMethod.GET],
    )
    suspend fun booked(
        @PathVariable("tenant") tenant: String,
        @PathVariable("username") username: String,
        @RequestHeader("Authorization") authentication: String?,
    ): NarratedResponse<List<JsonMap>> {
        // checking auth on a per-method like this is not recommended in a production application
        checkAuthorization(tenant = tenant, username = username, authorizationHeader = authentication)
        return tenantUserService.getFlightsForUser(tenant, username)
    }
}
