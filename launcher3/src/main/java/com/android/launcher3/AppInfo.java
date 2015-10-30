/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.compat.UserHandleCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Represents an app in AllAppsView.
 * AppsCustomizePagedView当中存放的基本item也是ItemInfo的子类
 * 在WorkSpace当中存放的ShortcutInfo也是ItemInfo的子类
 * 这也是他们之间的共通的地方
 *
 */
public class AppInfo extends ItemInfo {
    // 目前的情况是我们可以从一个AppInfo来得到ShortcutInfo，但是反过来却不可以，
    // 因为ShortcutInfo包含的信息总的来说是比AppInfo要少的，
    // 所以在Launcher3的实现当中，我们需要注意这点，我们需要在程序安装当中添加这个信息
    private static final String TAG = "Launcher3_AppInfo";

    /**
     * The intent used to start the application.
     */
    Intent intent;

    /**
     * A bitmap version of the application icon.
     */
    Bitmap iconBitmap;

    /**
     * The time at which the app was first installed.
     * 我们可以利用这个字段来进行AppsCustomizePagedView上面
     * 所有的App的排序整理工作，因为其中一个整理的原则就是按照程序
     * 安装的时间进行排序，而fistInstallTime就是我们排序的依据了
     *
     */
    long firstInstallTime;

    ComponentName componentName;

    static final int DOWNLOADED_FLAG = 1;
    static final int UPDATED_SYSTEM_APP_FLAG = 2;

    // 我们可以通过这个Flag来决定这个应用是否可以从系统当中卸载
    // 例如当我们从AppsCustomizePagedView当中长按一个Icon的结果有4种
    // 1，这个Icon拖动到DeleteZone，然后被删除；2. 这个Icon被拖动到Workspace
    // 当中，然后就相当于添加了这个App的shortCut；或者拖动到一个Folder当中3.我们
    // 在中途放弃了这个操作，那么我们也就不会创建这个App的Shortcut了；4.我们进行
    // 了非法的操作，那么操作直接失败,例如我们删除的app的flag当中包含了FLAG_SYSTEM
    // 那么我们是不能直接卸载这个App的
    int flags = 0;

    AppInfo() {
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;
    }

    public Intent getIntent() {
        return intent;
    }

    protected Intent getRestoredIntent() {
        return null;
    }

    /**
     * Must not hold the Context.
     */
    public AppInfo(Context context, LauncherActivityInfoCompat info, UserHandleCompat user,
            IconCache iconCache, HashMap<Object, CharSequence> labelCache) {
        this.componentName = info.getComponentName();
        this.container = ItemInfo.NO_ID;

        flags = initFlags(info);
        firstInstallTime = info.getFirstInstallTime();
        iconCache.getTitleAndIcon(this, info, labelCache);
        intent = makeLaunchIntent(context, info, user);
        this.user = user;
    }

    // 没当我们安装好程序时，都会读取这个新安装的程序的LauncherActivityInfoCompat信息
    // 然后我们再从这个程序当中读取相关的Flag信息,而我们需要的就是这个Flag信息
    public static int initFlags(LauncherActivityInfoCompat info) {
        // 在这里我们可以看到一个程序的Flag是直接通过PackageManager来获取的
        // 所以我们也可以利用这一个特性来直接在ShortcutInfo当中指定
        int appFlags = info.getApplicationInfo().flags;
        int flags = 0;
        if ((appFlags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
            // 我们需要利用这里指定的Flag来确定这个程序是否可以直接从Launcher当中卸载，
            // 如果Flag是DOWNLOAD_FLAG,那么我们就可以直接在Launcher当中直接卸载了
            // 否则Launcher给我们的选项当中就没有删除这个选项，而是查看这个App的信息
            flags |= DOWNLOADED_FLAG;

            if ((appFlags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                flags |= UPDATED_SYSTEM_APP_FLAG;
            }
        }
        return flags;
    }

    public AppInfo(AppInfo info) {
        super(info);
        componentName = info.componentName;
        title = info.title.toString();
        intent = new Intent(info.intent);
        flags = info.flags;
        firstInstallTime = info.firstInstallTime;
        iconBitmap = info.iconBitmap;
    }

    @Override
    public String toString() {
        return "ApplicationInfo(title=" + title.toString() + " id=" + this.id
                + " type=" + this.itemType + " container=" + this.container
                + " screen=" + screenId + " cellX=" + cellX + " cellY=" + cellY
                + " spanX=" + spanX + " spanY=" + spanY + " dropPos=" + Arrays.toString(dropPos)
                + " user=" + user + ")";
    }

    public static void dumpApplicationInfoList(String tag, String label, ArrayList<AppInfo> list) {
        Log.d(tag, label + " size=" + list.size());
        for (AppInfo info: list) {
            Log.d(tag, "   title=\"" + info.title + "\" iconBitmap="
                    + info.iconBitmap + " firstInstallTime="
                    + info.firstInstallTime);
        }
    }

    public ShortcutInfo makeShortcut() {
        Log.d(TAG, " we are making the shortCut for : " + this.title);
        return new ShortcutInfo(this);
    }

    public static Intent makeLaunchIntent(Context context, LauncherActivityInfoCompat info,
            UserHandleCompat user) {
        long serialNumber = UserManagerCompat.getInstance(context).getSerialNumberForUser(user);
        return new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(info.getComponentName())
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            .putExtra(EXTRA_PROFILE, serialNumber);
    }
}
