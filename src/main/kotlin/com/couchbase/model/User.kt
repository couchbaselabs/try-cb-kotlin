package com.couchbase.model

import com.couchbase.client.java.repository.annotation.Field
import org.springframework.data.annotation.Id
import org.springframework.data.couchbase.core.mapping.Document
import javax.validation.constraints.NotNull

@Document
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

    @Field
    var address: Address? = null

    @Field
    var preferences: List<Preference> = emptyList()

    @Field
    var securityRoles: List<String> = emptyList()
}