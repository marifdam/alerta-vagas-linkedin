package com.alertalinkedin.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface JobDao {
    @Query("SELECT EXISTS(SELECT 1 FROM seen_jobs WHERE id = :jobId)")
    boolean exists(String jobId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(JobEntity job);

    @Query("DELETE FROM seen_jobs WHERE foundAt < :timestamp")
    void deleteOlderThan(long timestamp);
}
