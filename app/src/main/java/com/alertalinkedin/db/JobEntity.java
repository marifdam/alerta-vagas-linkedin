package com.alertalinkedin.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "seen_jobs")
public class JobEntity {
    @PrimaryKey
    @NonNull
    public String id;

    public String title;
    public String company;
    public String location;
    public String url;
    public String keyword;
    public long foundAt;

    public JobEntity(@NonNull String id, String title, String company,
                     String location, String url, String keyword) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.url = url;
        this.keyword = keyword;
        this.foundAt = System.currentTimeMillis();
    }
}
