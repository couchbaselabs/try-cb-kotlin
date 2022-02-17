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

package trycb.util

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono


/**
 * Sets very permissive Cross-Origin Resource Sharing (CORS) headers.
 *
 * Probably not suitable for use in a production app.
 */
//@Component
//class CorsFilter : Filter {
//    override fun doFilter(
//        request: ServletRequest,
//        response: ServletResponse,
//        chain: FilterChain,
//    ) {
//        with(response as HttpServletResponse) {
//            setHeader("Access-Control-Allow-Origin", "*")
//            setHeader("Access-Control-Allow-Credentials", "true")
//            setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE")
//            setHeader("Access-Control-Allow-Headers",
//                "Origin, X-Requested-With, Content-Type, Accept, Authorization")
//        }
//        chain.doFilter(request, response)
//    }
//}

/**
 * Sets very permissive Cross-Origin Resource Sharing (CORS) headers.
 *
 * Probably not suitable for use in a production app.
 */
@Component
class CorsFilter : WebFilter {
    override fun filter(
        serverWebExchange: ServerWebExchange,
        webFilterChain: WebFilterChain,
    ): Mono<Void> {
        with(serverWebExchange.response.headers) {
            add("Access-Control-Allow-Origin", "*")
            add("Access-Control-Allow-Credentials", "true")
            add("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE")
            add("Access-Control-Allow-Headers",
                "Origin, X-Requested-With, Content-Type, Accept, Authorization")
        }
        return webFilterChain.filter(serverWebExchange)
    }
}

