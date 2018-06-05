package com.couchbase.model

import com.couchbase.client.java.repository.annotation.Field
import org.springframework.data.annotation.Id
import javax.validation.constraints.NotNull

class User(): BasicEntity() {

    constructor(id: String,
                name: String,
                address: Address,
                preferences: List<Preference>,
                securityRoles: List<String>): this(){

        this.id = id;
        this.name = name;
        this.address = address;
        this.preferences = preferences;
        this.securityRoles = securityRoles;
    }

    @Id
    var id: String? = null

    @NotNull
    var name: String? = null

    var address: Address? = null

    var preferences: List<Preference> = emptyList()

    var securityRoles: List<String> = emptyList()


}