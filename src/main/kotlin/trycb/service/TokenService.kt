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

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.util.Base64Utils
import trycb.util.jsonMapper

@Service
class TokenService(
    @Value("\${jwt.secret}") val secret: String,
    @Value("\${jwt.enabled}") val useJwt: Boolean,
) {

    /**
     * @throws IllegalStateException when the Authorization header couldn't be verified or didn't match the expected
     * username.
     */
    fun verifyAuthenticationHeader(authorization: String, expectedUsername: String) {
        val token = authorization.replaceFirst("Bearer ".toRegex(), "")
        val tokenName: String = if (useJwt) verifyJwt(token) else verifySimple(token)
        if (expectedUsername != tokenName) {
            throw BadCredentialsException("Token and username don't match")
        }
    }

    private fun verifyJwt(token: String): String {
        return try {
            Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .body["user", String::class.java]
        } catch (e: JwtException) {
            throw BadCredentialsException("Could not verify JWT token", e)
        }
    }

    private fun verifySimple(token: String): String {
        return try {
            String(Base64Utils.decodeFromString(token))
        } catch (e: Exception) {
            throw BadCredentialsException("Could not verify simple token", e)
        }
    }

    fun buildToken(username: String): String =
        if (useJwt) buildJwtToken(username) else buildSimpleToken(username)

    private fun buildJwtToken(username: String): String =
        Jwts.builder().signWith(SignatureAlgorithm.HS512, secret)
            .setPayload(jsonMapper.writeValueAsString(mapOf("user" to username)))
            .compact()

    private fun buildSimpleToken(username: String): String =
        Base64Utils.encodeToString(username.toByteArray())
}

