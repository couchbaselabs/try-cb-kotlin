package com.couchbase.model

import java.util.ArrayList

class Preference() {

    constructor(name: String,
                value: String): this(){
        this.name = name;
        this.value = value;
    }

    var name: String? = null

    var value: String? = null
}