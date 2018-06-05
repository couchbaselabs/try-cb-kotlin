package com.couchbase.model

class Address() {

    constructor(streetName: String,
                houseNumber: String,
                postalCode: String,
                city: String,
                country: String):this() {

        this.streetName = streetName;
        this.houseNumber = houseNumber;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
    }

    var streetName: String? = null

    var houseNumber: String? = null

    var postalCode: String? = null

    var city: String? = null

    var country: String? = null
}