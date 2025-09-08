package com.projects.barcodescanner.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDao {
    @Insert
    void insert(User user);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User findByUsername(String username);
}