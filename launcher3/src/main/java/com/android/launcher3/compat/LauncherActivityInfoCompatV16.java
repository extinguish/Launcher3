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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;


public class LauncherActivityInfoCompatV16 extends LauncherActivityInfoCompat {
    private ActivityInfo mActivityInfo;
    private ComponentName mComponentName;
    private PackageManager mPm;

    LauncherActivityInfoCompatV16(Context context, ResolveInfo info) {
        super();
        this.mActivityInfo = info.activityInfo;
        mComponentName = new ComponentName(mActivityInfo.packageName, mActivityInfo.name);
        mPm = context.getPackageManager();
    }

    public ComponentName getComponentName() {
        return mComponentName;
    }

    public UserHandleCompat getUser() {
        return UserHandleCompat.myUserHandle();
    }

    public CharSequence getLabel() {
        return mActivityInfo.loadLabel(mPm);
    }

    public Drawable getIcon(int density) {
        Drawable d = null;
        if (mActivityInfo.getIconResource() != 0) {
            Resources resources;
            try {
                // 首先当然就是尝试获取这个App所设置的Icon
                resources = mPm.getResourcesForApplication(mActivityInfo.packageName);
            } catch (PackageManager.NameNotFoundException e) {
                resources = null;
            }
            if (resources != null) {
                try {
                    // 如果我们获取程序自己的Icon失败的话，那么我们就尝试获取这个程序的Activity的
                    // 自己定义的Icon
                    d = resources.getDrawableForDensity(mActivityInfo.getIconResource(), density);
                } catch (Resources.NotFoundException e) {
                    // Return default icon below.
                }
            }
        }
        if (d == null) {
            // 靠，如果这个程序自己的Activity都没有Icon，那么我们只好自己给这个程序赋予一个我们自己定义的Icon
            // 而通常这个Icon是被我们用于系统App的Icon
            /**
             * Resources.getSystem()函数的作用就是：
             * Return a global shared Resources object that provides access to only
             * system resources (no application resources), and is not configured for
             * the current screen (can not use dimension units, does not change based
             * on orientation, etc).
             */
            Resources resources = Resources.getSystem();
            d = resources.getDrawableForDensity(android.R.mipmap.sym_def_app_icon, density);
        }
        return d;
    }

    public ApplicationInfo getApplicationInfo() {
        return mActivityInfo.applicationInfo;
    }

    public long getFirstInstallTime() {
        try {
            PackageInfo info = mPm.getPackageInfo(mActivityInfo.packageName, 0);
            return info != null ? info.firstInstallTime : 0;
        } catch (NameNotFoundException e) {
            return 0;
        }
    }

    public String getName() {
        return mActivityInfo.name;
    }

    public Drawable getBadgedIcon(int density) {
        return getIcon(density);
    }
}
