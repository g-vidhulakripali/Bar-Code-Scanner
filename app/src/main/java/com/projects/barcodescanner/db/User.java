// Create a new Java class: com.projects.barcodescanner.db.User
package com.projects.barcodescanner.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = {"username"}, unique = true)})
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "username")
    public String username;

    @ColumnInfo(name = "password_hash")
    public String passwordHash;

    @ColumnInfo(name = "role")
    public String role;

    // Constructors, getters, setters can be added if needed
}