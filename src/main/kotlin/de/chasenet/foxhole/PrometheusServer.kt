package de.chasenet.foxhole

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

fun startServer() {
    println("Starting server")
    val server = HttpServer.create(InetSocketAddress(8080), 0)
//    server.createContext("/prometheus") { httpExchange ->
//        val response = registry.scrape()
//        httpExchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
//        httpExchange.responseBody.write(response.toByteArray())
//        httpExchange.close()
//    }
    server.createContext("/health") { it.apply {
        sendResponseHeaders(200, 0)
        responseBody.close()
    } }
    Thread(server::start).start()
}