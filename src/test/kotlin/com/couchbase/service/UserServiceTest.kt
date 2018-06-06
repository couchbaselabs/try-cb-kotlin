package com.couchbase.service

import com.couchbase.model.Address
import com.couchbase.model.Preference
import com.couchbase.model.User
import com.couchbase.repositories.UserRepository
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest
class UserServiceTest {

    val USER_1 = "user::1"

    @Autowired
    lateinit var userService: UserService;

    @Autowired
    lateinit var userRepository: UserRepository;

    @Before
    fun deleteAll() = userRepository.deleteAll()

    @Test
    fun testSave() {
        val user = User(USER_1, "someUser", Address(), emptyList(), emptyList())
        userService.save(user)

        val savedUser = userService.findById(USER_1)
        assertThat<Any>(savedUser.id, equalTo<Any>(user.id))
    }

    @Test
    fun testSelectByPreference() {

        val preferences = Arrays.asList<Preference>(Preference("targetName", "targetValue"))
        val otherPreferences = Arrays.asList<Preference>(Preference("someOtherName", "someOtherValue"))

        userService.save(User(USER_1, "user1", Address(), preferences, emptyList()))
        userService.save(User("user::2", "user2", Address(), preferences, emptyList()))
        userService.save(User("user::3", "user3", Address(), otherPreferences, emptyList()))

        val users = userService.findUsersByPreferenceName("targetName")

        assertThat(users, hasSize<Any>(2))
    }

    @Test
    fun testHasRole() {
        userService.save(User(USER_1, "user1", Address(), emptyList(), Arrays.asList<String>("admin", "manager")))

        assertTrue(userService.hasRole(USER_1, "admin"))
        assertFalse(userService.hasRole(USER_1, "user"))
    }

    @Test
    fun testComposedAddress() {
        val address1 = Address("street1", "1", "0000", "santo andre", "br")
        val address2 = Address("street1", "2", "0000", "santo andre", "br")
        val address3 = Address("street2", "12", "1111", "munich", "de")

        userService.save(User(USER_1, "user1", address1, emptyList(), emptyList()))
        userService.save(User("user::2", "user2", address2, emptyList(), emptyList()))
        userService.save(User("user::3", "user3", address3, emptyList(), emptyList()))

        var users = userService.findUserByAddress(streetName = "street1")
        assertThat(users, hasSize<Any>(2))

        users = userService.findUserByAddress(streetName = "street1", number=  "1")
        assertThat(users, hasSize<Any>(1))

        users = userService.findUserByAddress(country = "de")
        assertThat(users, hasSize<Any>(1))
    }

}