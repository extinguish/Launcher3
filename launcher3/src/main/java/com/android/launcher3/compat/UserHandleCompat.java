/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.launcher3.compat;

import android.content.Intent;
import android.os.Build;
import android.os.UserHandle;

import com.android.launcher3.Utilities;

// 用于处理Android 5.0当中新引入的Multi-User模式，
// 在这种模式下面，我们同时还要处理多用户模式下面的
// Launcher处理过程
public class UserHandleCompat {
    // the UserHandle are the android.os internal implementation of
    // which are the representation of a user on the device
    private UserHandle mUser;

    private UserHandleCompat(UserHandle user) {
        mUser = user;
    }

    private UserHandleCompat() {
    }

    public static UserHandleCompat myUserHandle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Android 当中从4.2之后开始引入了多用户模式，不过只是在Tablet当汇总
            // 可以使用。
            // 真正的在所有的平台上都引入Multi-User Mode是在4.4之后了，
            // 因为在4.4之前没有解决的一个问题就是多用户模式下的TelephonyManager
            // 的问题应该如何处理，而Tablet上面是没有这个烦恼的
            // 参考<Android Security Internals> 来对多用户模式下的
            // Telephony处理过程
            return new UserHandleCompat(android.os.Process.myUserHandle());
        } else {
            // 如果是Android4.2之后，那么我们直接返回一个空的UserHandle就可以了
            return new UserHandleCompat();
        }
    }

    static UserHandleCompat fromUser(UserHandle user) {
        if (user == null) {
            return null;
        } else {
            return new UserHandleCompat(user);
        }
    }

    UserHandle getUser() {
        return mUser;
    }

    @Override
    public String toString() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return mUser.toString();
        } else {
            return "";
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof UserHandleCompat)) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return mUser.equals(((UserHandleCompat) other).mUser);
        } else {
            return true;
        }
    }

    @Override
    public int hashCode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return mUser.hashCode();
        } else {
            return 0;
        }
    }

    /**
     * Adds {@link UserHandle} to the intent in for L or above.
     * Pre-L the launcher doesn't support showing apps for multiple
     * profiles so this is a no-op.
     */
    public void addToIntent(Intent intent, String name) {
        if (Utilities.isLmpOrAbove() && mUser != null) {
            intent.putExtra(name, mUser);
        }
    }
}
