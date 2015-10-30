/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;


import android.view.View;

public class CheckLongPressHelper {
    private View mView;
    private boolean mHasPerformedLongPress;
    private CheckForLongPress mPendingCheckForLongPress;

    class CheckForLongPress implements Runnable {
        public void run() {
            if ((mView.getParent() != null) && mView.hasWindowFocus()
                    && !mHasPerformedLongPress) {
                /**
                 * Call this view's OnLongClickListener, if it is defined. Invokes the context menu if the
                 * OnLongClickListener did not consume the event.
                 *
                 * View.performLongClick()方法返回true如果有receiver consumed long click的点击事件，否则就返回false
                 */
                if (mView.performLongClick()) {
                    mView.setPressed(false);
                    mHasPerformedLongPress = true;
                }
            }
        }
    }

    public CheckLongPressHelper(View v) {
        mView = v;
    }

    public void postCheckForLongPress() {
        mHasPerformedLongPress = false;

        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mView.postDelayed(mPendingCheckForLongPress,
                LauncherAppState.getInstance().getLongPressTimeout());
    }

    public void cancelLongPress() {
        mHasPerformedLongPress = false;
        if (mPendingCheckForLongPress != null) {
            // 每一个View本身都是可以处理很多的Callbacks的，如果一个View
            // 接受了不止一个Callback(这些callback都是Runnable的instance)
            // View就会把这些runnable放入到一个Queue当中，那么我们可以再View
            // 还没有处理这个callback之前就把这个callback从Queue当中移除掉
            mView.removeCallbacks(mPendingCheckForLongPress);
            mPendingCheckForLongPress = null;
        }
    }

    public boolean hasPerformedLongPress() {
        return mHasPerformedLongPress;
    }
}
