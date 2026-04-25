package com.layzbug.app.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? = date?.toString()
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE walks ADD COLUMN distanceKm REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE walks ADD COLUMN minutes INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE walks ADD COLUMN isManual INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(entities = [WalkEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walkDao(): WalkDao
}