package com.alertalinkedin.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface KeywordDao {
    @Query("SELECT * FROM keywords ORDER BY id DESC")
    LiveData<List<KeywordEntity>> getAll();

    @Query("SELECT * FROM keywords ORDER BY id DESC")
    List<KeywordEntity> getAllSync();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(KeywordEntity keyword);

    @Delete
    void delete(KeywordEntity keyword);
}
