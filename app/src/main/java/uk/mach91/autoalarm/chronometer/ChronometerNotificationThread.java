/*
 * Copyright 2017 Phillip Hsu
 *
 * This file is part of ClockPlus.
 *
 * ClockPlus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ClockPlus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ClockPlus.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2018 Mach91 - content updated for Alarmatic
 *
 */

package uk.mach91.autoalarm.chronometer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Phillip Hsu on 9/10/2016.
 *
 * A thread that updates a chronometer-based notification. While notifications
 * have built-in support for using a chronometer, it lacks pause/resume functionality
 * and the ability to choose between count up or count down.
 */
public class ChronometerNotificationThread extends HandlerThread {
    private static final String TAG = "ChronomNotifThread";

    private static final int MSG_WHAT = 2;

    private final ChronometerDelegate mDelegate;
    private final NotificationManager mNotificationManager;
    private final Notification.Builder mNoteBuilder;
    private final Resources mResources;
    private final String mNoteTag;
    private final int mNoteId;

    private Handler mHandler;

    /**
     * @param delegate Configured by the client service, including whether to be counting down or not.
     * @param builder A preconfigured Builder from the client service whose content
     *                text will be updated and eventually built from.
     * @param resources Required only if the ChronometerDelegate is configured to count down.
*                  Used to retrieve a String resource if/when the countdown reaches negative.
*                  TODO: Will the notification be cancelled fast enough before the countdown
     * @param noteTag An optional tag for posting notifications.
     */
    public ChronometerNotificationThread(@NonNull ChronometerDelegate delegate,
                                         @NonNull NotificationManager manager,
                                         @NonNull Notification.Builder builder,
                                         @Nullable Resources resources,
                                         @Nullable String noteTag,
                                         int noteId) {
        super(TAG);
        mDelegate = delegate;
        mNotificationManager = manager;
        mNoteBuilder = builder;
        mResources = resources;
        mNoteTag = noteTag;
        mNoteId = noteId;
    }

    // There won't be a memory leak since our handler is using a looper that is not
    // associated with the main thread. The full Lint warning confirmed this.
    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        // This is called after the looper has completed initializing, but before
        // it starts looping through its message queue. Right now, there is no
        // message queue, so this is the place to create it.
        // By default, the constructor associates this handler with this thread's looper.
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                updateNotification(true);
                sendMessageDelayed(Message.obtain(this, MSG_WHAT), 1000);
            }
        };
        // Once the handler is initialized, we may immediately begin our work.
        mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_WHAT), 1000);
    }

    /**
     * @param updateText whether the new notification should update its chronometer.
     *                   Use {@code false} if you are updating everything else about the notification,
     *                   e.g. you just want to refresh the actions due to a start/pause state change.
     */
    public void updateNotification(boolean updateText) {
        if (updateText) {
            CharSequence text = mDelegate.formatElapsedTime(SystemClock.elapsedRealtime(), mResources);
            mNoteBuilder.setContentText(text);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = "channel-05";
            String channelName = "Timer";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
            mChannel.setSound(null,null);
            mNotificationManager.createNotificationChannel(mChannel);
            mNoteBuilder.setChannelId(channelId);
        }

        mNotificationManager.notify(mNoteTag, mNoteId, mNoteBuilder.build());
    }

    @Override
    public boolean quit() {
        // TODO: I think we can call removeCallbacksAndMessages(null)
        // to remove ALL messages.
        mHandler.removeMessages(MSG_WHAT);
        return super.quit();
    }
}
