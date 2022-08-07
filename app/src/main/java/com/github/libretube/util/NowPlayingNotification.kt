package com.github.libretube.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.session.MediaSessionCompat
import com.github.libretube.BACKGROUND_CHANNEL_ID
import com.github.libretube.PLAYER_NOTIFICATION_ID
import com.github.libretube.activities.MainActivity
import com.github.libretube.obj.Streams
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import java.net.URL

class NowPlayingNotification(
    private val context: Context,
    private val player: ExoPlayer
) {
    private var streams: Streams? = null

    /**
     * The [PlayerNotificationManager] to load the [mediaSession] content on it.
     */
    private var playerNotification: PlayerNotificationManager? = null

    private fun setStreams(streams: Streams) {
        this.streams = streams
    }

    /**
     * The [DescriptionAdapter] is used to show title, uploaderName and thumbnail of the video in the notification
     * Basic example [here](https://github.com/AnthonyMarkD/AudioPlayerSampleTest)
     */
    inner class DescriptionAdapter() :
        PlayerNotificationManager.MediaDescriptionAdapter {
        /**
         * sets the title of the notification
         */
        override fun getCurrentContentTitle(player: Player): CharSequence {
            // return controller.metadata.description.title.toString()
            return streams?.title!!
        }

        /**
         * overrides the action when clicking the notification
         */
        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            //  return controller.sessionActivity
            /**
             *  starts a new MainActivity Intent when the player notification is clicked
             *  it doesn't start a completely new MainActivity because the MainActivity's launchMode
             *  is set to "singleTop" in the AndroidManifest (important!!!)
             *  that's the only way to launch back into the previous activity (e.g. the player view
             */
            val intent = Intent(context, MainActivity::class.java)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        /**
         * the description of the notification (below the title)
         */
        override fun getCurrentContentText(player: Player): CharSequence? {
            // return controller.metadata.description.subtitle.toString()
            return streams?.uploader
        }

        /**
         * return the icon/thumbnail of the video
         */
        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            lateinit var bitmap: Bitmap

            /**
             * running on a new thread to prevent a NetworkMainThreadException
             */
            val thread = Thread {
                try {
                    /**
                     * try to GET the thumbnail from the URL
                     */
                    val inputStream = URL(streams?.thumbnailUrl).openStream()
                    bitmap = BitmapFactory.decodeStream(inputStream)
                } catch (ex: java.lang.Exception) {
                    ex.printStackTrace()
                }
            }
            thread.start()
            thread.join()
            /**
             * returns the scaled bitmap if it got fetched successfully
             */
            return try {
                val resizedBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    bitmap.width,
                    bitmap.width,
                    false
                )
                resizedBitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Initializes the [playerNotification] attached to the [player] and shows it.
     */
    fun initializePlayerNotification(
        mediaSession: MediaSessionCompat,
        streams: Streams
    ) {
        this.streams = streams
        playerNotification = PlayerNotificationManager
            .Builder(context, PLAYER_NOTIFICATION_ID, BACKGROUND_CHANNEL_ID)
            // set the description of the notification
            .setMediaDescriptionAdapter(
                DescriptionAdapter()
            )
            .build()
        playerNotification?.apply {
            setPlayer(player)
            setUseNextAction(false)
            setUsePreviousAction(false)
            setUseStopAction(true)
            setColorized(true)
            setMediaSessionToken(mediaSession.sessionToken)
        }
    }
}
