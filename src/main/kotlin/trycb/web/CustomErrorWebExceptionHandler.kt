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

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RequestPredicates.all
import org.springframework.web.reactive.function.server.RequestPredicates.path
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.result.view.ViewResolver
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import kotlin.streams.toList

@Configuration
class ExceptionHandlerBeans {
    // CustomErrorWebExceptionHandler wants this
    @Bean
    fun resources(): WebProperties.Resources = WebProperties.Resources()
}

/**
 * So much boilerplate just to configure the global error response... ugh!
 */
@Order(-2) // Must be fast if you want to beat the default handler
@Component
class CustomErrorWebExceptionHandler(
    errorAttributes: ErrorAttributes,
    resourceProperties: WebProperties.Resources,
    applicationContext: ApplicationContext,
    serverCodecConfigurer: ServerCodecConfigurer,
    viewResolvers: ObjectProvider<ViewResolver>,
) : AbstractErrorWebExceptionHandler(errorAttributes, resourceProperties, applicationContext) {

    init {
        super.setMessageWriters(serverCodecConfigurer.writers)
        super.setMessageReaders(serverCodecConfigurer.readers)
        super.setViewResolvers(viewResolvers.orderedStream().toList())
    }

    override fun getRoutingFunction(errorAttributes: ErrorAttributes): RouterFunction<ServerResponse> {
        return route(all()) { serverRequest: ServerRequest -> renderErrorResponse(serverRequest) }
    }

    private fun renderErrorResponse(serverRequest: ServerRequest): Mono<ServerResponse> {
        val throwable = serverRequest
            .attribute("org.springframework.boot.web.reactive.error.DefaultErrorAttributes.ERROR")
            .orElseThrow { IllegalStateException("Missing exception attribute in ServerWebExchange") } as Throwable

        if (throwable is ResponseStatusException) {
            return renderErrorResponse(throwable.status, throwable.reason ?: throwable.message)
        }

        return renderErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error")
    }

    private fun renderErrorResponse(status: HttpStatus, message: String): Mono<ServerResponse> =
        ServerResponse.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(mapOf("message" to message)), Map::class.java)
}
