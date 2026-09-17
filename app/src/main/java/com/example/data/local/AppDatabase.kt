package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BadgeDao
import com.example.data.local.dao.CommentDao
import com.example.data.local.dao.FollowDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.PostDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.BadgeEntity
import com.example.data.local.entity.CommentEntity
import com.example.data.local.entity.FollowEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.PostEntity
import com.example.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        FollowEntity::class,
        NotificationEntity::class,
        BadgeEntity::class,
        CommentEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun followDao(): FollowDao
    abstract fun notificationDao(): NotificationDao
    abstract fun badgeDao(): BadgeDao
    abstract fun commentDao(): CommentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ilmnet_academic.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
