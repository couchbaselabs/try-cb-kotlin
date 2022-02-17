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
package trycb.config

import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.filter.CorsFilter

/**
 * Trace incoming HTTP requests using Spring Boot CommonsRequestLoggingFilter.
 * This allows us to see incoming request logs when running the application.
 */
@Configuration
class Request {
//    @Bean
//    fun logFilter(): CommonsRequestLoggingFilter = CommonsRequestLoggingFilter().apply {
//        setIncludeQueryString(true)
//        setIncludePayload(true)
//        setMaxPayloadLength(10000)
//        setIncludeHeaders(false)
//        setAfterMessagePrefix("REQUEST: ")
//    }

//    @Bean  fun errorAttributes() : ErrorAttributes {
//        return DefaultErrorAttributes().
//    }
////
//    fun getErrorAttributes(request: ServerRequest, includeStackTrace: Boolean): Map<String, Any> {
//        val ex = getError(request)
//
//        if (ex is TryCbException) {
//            return mapOf(attributes)
//        }
//
//        val attributes = LinkedHashMap<String, Any>()
//        attributes["status"] = HttpStatus.BAD_REQUEST.value()
//        attributes["message"] = "bad"
//        return attributes
//    }
//}

//    @Bean
//    fun corsFilter(): CorsWebFilter {
//        val config = CorsConfiguration().apply {
//            addAllowedOrigin("*")
//
//            allowCredentials = true
//
//            listOf("POST", "PUT", "GET", "OPTIONS", "DELETE")
//                .forEach { addAllowedMethod(it) }
//
//            listOf("Origin", "X-Requested-With", "Content-Type", "Accept", "Authorization")
//                .forEach { addAllowedHeader(it) }
//        }
//
//        val source = UrlBasedCorsConfigurationSource()
//        source.registerCorsConfiguration("/**", config)
//
//        return CorsWebFilter(source)
//    }
//

}

