package app.hisaab.di

import android.content.Context
import androidx.room.Room
import app.hisaab.data.HisaabDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): HisaabDb =
        Room.databaseBuilder(ctx, HisaabDb::class.java, "hisaab.db")
            .fallbackToDestructiveMigration().build()
}
