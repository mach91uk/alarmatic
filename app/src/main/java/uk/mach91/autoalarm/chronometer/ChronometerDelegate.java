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
 * Copyright 2018 Chris Allenby - content updated for Alarmation
 *
 */

package uk.mach91.autoalarm.chronometer;

import android.content.res.Resources;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.RelativeSizeSpan;
import android.util.Log;

import uk.mach91.autoalarm.R;

import java.util.Formatter;
import java.util.IllegalFormatException;
import java.util.Locale;

/**
 * Created by Phillip Hsu on 9/7/2016.
 *
 * A helper class for BaseChronometer that handles formatting the text.
 * Can also be used independent of Chronometer to format elapsed times and return the result
 * as a CharSequence.
 */
public final class ChronometerDelegate {
    private static final String TAG = "ChronometerDelegate";

    private static final RelativeSizeSpan SIZE_SPAN = new RelativeSizeSpan(0.5f);

    private long mBase;
    private long mNow; // the currently displayed time
    private boolean mLogged;
    private String mFormat;
    private Formatter mFormatter;
    private Locale mFormatterLocale;
    private Object[] mFormatterArgs = new Object[1];
    private StringBuilder mFormatBuilder;
    private StringBuilder mRecycle = new StringBuilder(8);
    private boolean mCountDown;
    private boolean mShowCentiseconds;
    private boolean mApplySizeSpanOnCentiseconds;

    public void init() {
        mBase = SystemClock.elapsedRealtime();
    }

    public void setCountDown(boolean countDown) {
        mCountDown = countDown;
    }

    public boolean isCountDown() {
        return mCountDown;
    }

    public void setShowCentiseconds(boolean showCentiseconds, boolean applySizeSpan) {
        mShowCentiseconds = showCentiseconds;
        mApplySizeSpanOnCentiseconds = applySizeSpan;
    }

    public boolean showsCentiseconds() {
        return mShowCentiseconds;
    }

    public void setBase(long base) {
        mBase = base;
    }

    public long getBase() {
        return mBase;
    }

    public void setFormat(String format) {
        mFormat = format;
        if (format != null && mFormatBuilder == null) {
            mFormatBuilder = new StringBuilder(format.length() * 2);
        }
    }

    public String getFormat() {
        return mFormat;
    }

    public CharSequence formatElapsedTime(long now, @Nullable Resources resources) {
        mNow = now;
        long seconds = mCountDown ? mBase - now : now - mBase;
        boolean negative = false;
        // Only modify how negative timers are displayed if we have a Resources.
        // Otherwise, if we invert the sign of seconds and we don't have a Resources,
        // the timer will be positive again which will confuse the user.
        if (seconds < 0 && resources != null) {
            seconds = -seconds;
            negative = true;
        }
        String text = DateUtils.formatElapsedTime(mRecycle, seconds / 1000);
        if (negative) {
            // The only way this can call through is if the previous null check on
            // `resources` passed.
            text = resources.getString(R.string.negative_duration, text);
        }

        Locale loc = Locale.getDefault();
        if (mFormat != null) {
            if (mFormatter == null || !loc.equals(mFormatterLocale)) {
                mFormatterLocale = loc;
                mFormatter = new Formatter(mFormatBuilder, loc);
            }
            mFormatBuilder.setLength(0);
            mFormatterArgs[0] = text;
            try {
                mFormatter.format(mFormat, mFormatterArgs);
                text = mFormatBuilder.toString();
            } catch (IllegalFormatException ex) {
                if (!mLogged) {
                    Log.w(TAG, "Illegal format string: " + mFormat);
                    mLogged = true;
                }
            }
        }
        if (mShowCentiseconds) {
            long centiseconds = (seconds % 1000) / 10;
            String centisecondsText = String.format(loc,
                    // TODO: Different locales use different decimal marks.
                    // The two most common are . and ,
                    // Consider removing the . and just let the size span
                    // represent this as fractional seconds?
                    // ...or figure out how to get the correct mark for the
                    // current locale.
                    // It looks like Google's Clock app strictly uses .
                    ".%02d", // The . before % is not a format specifier
                    centiseconds);
            if (mApplySizeSpanOnCentiseconds) {
                SpannableString span = new SpannableString(centisecondsText);
                span.setSpan(SIZE_SPAN, 0, centisecondsText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return TextUtils.concat(text, span);
            } else {
                return text.concat(centisecondsText);
            }
        }

        return text;
    }
}
