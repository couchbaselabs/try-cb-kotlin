package com.couchbase.repositories

import com.couchbase.model.User
import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed
import org.springframework.data.couchbase.core.query.Query
import org.springframework.data.couchbase.core.query.ViewIndexed
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository

@N1qlPrimaryIndexed
@ViewIndexed(designDoc = "user")
interface UserRepository : CouchbasePagingAndSortingRepository<User, String> {

    fun findByName(name: String): List<User>

    @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} and ANY preference IN " + " preferences SATISFIES preference.name = $1 END")
    fun findUsersByPreferenceName(name: String): List<User>

    @Query("#{#n1ql.selectEntity} where #{#n1ql.filter} and meta().id = $1 and ARRAY_CONTAINS(securityRoles, $2)")
    fun hasRole(userId: String, role: String): User
}
