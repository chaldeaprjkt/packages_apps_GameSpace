package io.chaldeaprjkt.gamespace.utils.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.utils.GameModeUtils
import io.chaldeaprjkt.gamespace.utils.ScreenUtils


@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServiceViewEntryPoint {
    fun appSettings(): AppSettings
    fun systemSettings(): SystemSettings
    fun screenUtils(): ScreenUtils
    fun gameModeUtils(): GameModeUtils
}
