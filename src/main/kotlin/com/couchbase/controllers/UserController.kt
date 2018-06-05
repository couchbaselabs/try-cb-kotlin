package com.couchbase.controllers

import com.couchbase.model.User
import com.couchbase.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import javax.websocket.server.PathParam

import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST

@RestController
@RequestMapping("/api/user")
class UserController {

    @Autowired
    lateinit var userService: UserService


    @GetMapping(value = "/{id}")
    fun findById(@PathParam("id") id: String) = userService.findById(id)


    @GetMapping(value = "/preference")
    fun findPreference(@RequestParam("name") name: String): List<User> {
        return userService.findUsersByPreferenceName(name)
    }

    @GetMapping(value = "/find")
    fun findUserByName(@RequestParam("name") name: String): List<User> {
        return userService.findByName(name)
    }

    @PostMapping(value = "/save")
    fun findUserByName(@RequestBody user: User) = userService.save(user)

    @GetMapping(value = "/findByAddress")
    fun findByAddress(@RequestParam("streetName", defaultValue = "") streetName: String,
                      @RequestParam("number", defaultValue = "") number: String,
                      @RequestParam("postalCode", defaultValue = "") postalCode: String,
                      @RequestParam("city", defaultValue = "") city: String,
                      @RequestParam("country", defaultValue = "") country: String): List<User> {
        return userService.findUserByAddress(streetName, number, postalCode, city, country);
    }

}