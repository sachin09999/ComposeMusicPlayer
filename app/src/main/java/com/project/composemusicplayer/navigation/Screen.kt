package com.project.composemusicplayer.navigation

sealed class Screen(val route: String) {
    object Home: Screen("home")
    object SongList : Screen("song_list")
    object FullScreenPlayer: Screen("full_screen_player")
}