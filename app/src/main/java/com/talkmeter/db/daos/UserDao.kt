package com.talkmeter.db.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.talkmeter.db.User

@Dao
interface UserDao {
    @Query("SELECT * FROM user")
    fun getAll(): LiveData<List<User>>

    @Query("SELECT * FROM user WHERE u_category = 1")
    fun loadLeftUsers(): LiveData<List<User>>

    @Query("SELECT * FROM user WHERE u_category = 2")
    fun loadRightUsers(): LiveData<List<User>>

    @Query("SELECT * FROM user WHERE spoke_time > 0")
    fun loadTimedUsers(): LiveData<List<User>>

    @Insert
    fun insertAll(vararg user: User)

    @Update
    fun updateUserById(vararg user: User)

    @Delete
    fun delete(user: User)

    @Query("DELETE FROM user")
    fun clear()

    @Query("UPDATE user SET spoke_time = spoke_time + 1 WHERE uid IN (SELECT uid FROM user WHERE u_selected=1)")
    fun updateTimers()
}