package com.alertalinkedin.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "keywords", indices = {@Index(value = "value", unique = true)})
public class KeywordEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String value;

    public KeywordEntity() {}

    @Ignore
    public KeywordEntity(String value) {
        this.value = value;
    }
}
