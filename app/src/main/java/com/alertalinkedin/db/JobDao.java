package com.alertalinkedin.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface JobDao {
    @Query("SELECT COUNT(*) FROM seen_jobs WHERE id = :jobId")
    int exists(String jobId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(JobEntity job);
}
