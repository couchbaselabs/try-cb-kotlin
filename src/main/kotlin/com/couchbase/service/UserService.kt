package com.couchbase.service

import com.couchbase.client.java.document.json.JsonObject
import com.couchbase.client.java.query.N1qlParams
import com.couchbase.client.java.query.N1qlQuery
import com.couchbase.client.java.query.consistency.ScanConsistency
import com.couchbase.model.User
import com.couchbase.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.validation.Valid

@Service
class UserService {

    @Autowired
    lateinit var userRepository: UserRepository;

    fun findByName(name: String): List<User> = userRepository.findByName(name)

    fun findById(userId: String) = userRepository.findOne(userId)

    fun save(@Valid user: User) = userRepository.save(user)

    fun findUsersByPreferenceName(name: String): List<User> = userRepository.findUsersByPreferenceName(name)

    fun hasRole(userId: String, role: String): Boolean {
        return userRepository.hasRole(userId, role) != null
    }

    /**
     * Example of ad hoc queries
     */
    fun findUserByAddress(streetName: String?, number: String?, postalCode: String?,
                          city: String?, country: String?): List<User> {

        var query = "SELECT meta(b).id as id, b.* FROM " + getBucketName() + " b WHERE  b._class = '" + User::class.java.getName() + "' "

        if (!streetName.isNullOrBlank()) query += " and b.address.streetName = '$streetName' "

        if (!number.isNullOrBlank()) query += " and b.address.houseNumber = '$number' "

        if (!postalCode.isNullOrBlank()) query += " and b.address.postalCode = '$postalCode' "

        if (!city.isNullOrBlank()) query += " and b.address.city = '$city' "

        if (!country.isNullOrBlank()) query += " and b.address.country = '$country' "

        val params = N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS).adhoc(true)
        val paramQuery = N1qlQuery.parameterized(query, JsonObject.create(), params)
        return userRepository.getCouchbaseOperations().findByN1QLProjection(paramQuery, User::class.java)
    }

    fun getBucketName() = userRepository.getCouchbaseOperations().getCouchbaseBucket().bucketManager().info().name()
}