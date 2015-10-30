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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.launcher3.compat.UserHandleCompat;

import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represents a launchable icon on the workspaces and in folders.
 * ShortcutInfo代表的都是可以直接启动的Item,注意这是不同于FolderIcon的,
 * FolderIcon点击之后是直接进入到这个Folder内部，即这个Folder展开之后的
 * 内容了
 */
public class ShortcutInfo extends ItemInfo {

    private static final String TAG = "Launcher3_ShortcutInfo";

    // Shortcut是否是分类型的，即不同的Shortcut具有不同的类型
    // 当我们向Launcher当中的Workspace当中添加Shortcut时，会遇到
    // 两种情况，第一种就是当我们安装完程序之后，自动在桌面上添加
    // 一个ShortcutIcon，第二种就是当我们安装好一个程序之后，这个程序
    // 可以自己添加Shortcut(不是这个程序的图标，而是类似于有道笔记那样，或者为知笔记
    // 那样可以为每一篇文件添加桌面快捷方式这种)
    // 他们的创建方式是不同的，甚至在某些地方也是不同的，我们需要区分他们
    // 就目前来看，由于第三方程序创建自己的Shortcut的信息是来自于系统创建的，
    // 即第三方程序自己申请INSTALL_SHORTCUT权限之后然后进行程序安装的过程
    // 但是这个过程全部是由系统自己在进行操控的，与Launcher之间没有任何关系，所以我们不需要
    // 关心这些
    // 关键的问题就是所有的这些流程都不会走我们这个ShortcutInfo当中的任何一个构造方法
    // 所以我们可以在我们自己的构造方法当中添加自己的标记
    // Shortcut当中并没有标注这个App是来自于System还是来自于DOWNLOAD的app
    // 所以我们需要额外的添加上这个Flag
    public static final int DEFAULT = 0;

    /**
     * The shortcut was restored from a backup and it not ready to be used. This is automatically
     * set during backup/restore
     */
    public static final int FLAG_RESTORED_ICON = 1;

    /**
     * The icon was added as an auto-install app, and is not ready to be used. This flag can't
     * be present along with {@link #FLAG_RESTORED_ICON}, and is set during default layout
     * parsing.
     */
    public static final int FLAG_AUTOINTALL_ICON = 2;

    /**
     * The icon is being installed. If {@link FLAG_RESTORED_ICON} or {@link FLAG_AUTOINTALL_ICON}
     * is set, then the icon is either being installed or is in a broken state.
     */
    public static final int FLAG_INSTALL_SESSION_ACTIVE = 4;

    /**
     * Indicates that the widget restore has started.
     */
    public static final int FLAG_RESTORE_STARTED = 8;

    /**
     * The intent used to start the application.
     */
    Intent intent;

    /**
     * Indicates whether the icon comes from an application's resource (if false)
     * or from a custom Bitmap (if true.)
     */
    boolean customIcon;

    /**
     * Indicates whether we're using the default fallback icon instead of something from the
     * app.
     */
    boolean usingFallbackIcon;

    /**
     * If isShortcut=true and customIcon=false, this contains a reference to the
     * shortcut icon as an application's resource.
     */
    Intent.ShortcutIconResource iconResource;

    /**
     * The application icon.
     */
    private Bitmap mIcon;

    /**
     * Indicates that the icon is disabled due to safe mode restrictions.
     */
    public static final int FLAG_DISABLED_SAFEMODE = 1;

    /**
     * Indicates that the icon is disabled as the app is not available.
     */
    public static final int FLAG_DISABLED_NOT_AVAILABLE = 2;

    /**
     * Could be disabled, if the the app is installed but unavailable (eg. in safe mode or when
     * sd-card is not available).
     */
    int isDisabled = DEFAULT;

    int status;

    /**
     * The installation progress [0-100] of the package that this shortcut represents.
     */
    private int mInstallProgress;

    /**
     * Refer {@link AppInfo#firstInstallTime}.
     */
    long firstInstallTime;

    //TODO: 现在的情况是，如果我们无法获得AppInfo时，我们就无法完成flags变量的初始化过程
    /**
     * TODO move this to {@link status}
     */
    int flags = 0;

    /**
     * If this shortcut is a placeholder, then intent will be a market intent for the package, and
     * this will hold the original intent from the database.  Otherwise, null.
     * Refer {@link #FLAG_RESTORE_PENDING}, {@link #FLAG_INSTALL_PENDING}
     */
    Intent promisedIntent;

    ShortcutInfo() {
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;
    }

    public Intent getIntent() {
        return intent;
    }

    ShortcutInfo(Intent intent, CharSequence title, CharSequence contentDescription,
            Bitmap icon, UserHandleCompat user) {
        this();
        Log.d(TAG, " create the shortcutInfo with all of the DETAILED PARAMETER ... ");
        this.intent = intent;
        this.title = title;
        this.contentDescription = contentDescription;
        mIcon = icon;
        this.user = user;
    }

    public ShortcutInfo(Context context, ShortcutInfo info) {
        super(info);
        Log.d(TAG, " create shortcut info with the CONTEXT & SHORTCUT INFO PARAMETER ... ");
        title = info.title.toString();
        intent = new Intent(info.intent);
        if (info.iconResource != null) {
            iconResource = new Intent.ShortcutIconResource();
            iconResource.packageName = info.iconResource.packageName;
            iconResource.resourceName = info.iconResource.resourceName;
        }
        mIcon = info.mIcon; // TODO: should make a copy here.  maybe we don't need this ctor at all
        customIcon = info.customIcon;
        flags = info.flags;
        firstInstallTime = info.firstInstallTime;
        user = info.user;
        status = info.status;
    }

    // 注意这里我们创建的Shortcut并没有涉及到screenId的值的设置
    /** TODO: Remove this.  It's only called by ApplicationInfo.makeShortcut. */
    // 但是我们每次当我们安装新的程序之后，都会调用到这里的步骤，即
    // LauncherModel检测到有新的程序安装之后就会调用这个构造方法来创建新的快捷方式在桌面上
    public ShortcutInfo(AppInfo info) {
        super(info);
        Log.d(TAG, " create the shortcutInfo with APPINFO PARAMETER ... ");
        title = info.title.toString();
        intent = new Intent(info.intent);
        customIcon = false;
        flags = info.flags;
        firstInstallTime = info.firstInstallTime;
        Log.d(TAG, " ----> here we create the shortcut Info by the ApplicationInfo.makeShortcut method \n"
                + ", and the detailed information are : " + toString());
    }

    public void setIcon(Bitmap b) {
        mIcon = b;
    }

    public Bitmap getIcon(IconCache iconCache) {
        if (mIcon == null) {
            updateIcon(iconCache);
        }
        return mIcon;
    }

    public void updateIcon(IconCache iconCache) {
        mIcon = iconCache.getIcon(promisedIntent != null ? promisedIntent : intent, user);
        usingFallbackIcon = iconCache.isDefaultIcon(mIcon, user);
    }

    @Override
    void onAddToDatabase(Context context, ContentValues values) {
        super.onAddToDatabase(context, values);

        String titleStr = title != null ? title.toString() : null;
        values.put(LauncherSettings.BaseLauncherColumns.TITLE, titleStr);

        String uri = promisedIntent != null ? promisedIntent.toUri(0)
                : (intent != null ? intent.toUri(0) : null);
        values.put(LauncherSettings.BaseLauncherColumns.INTENT, uri);

        if (customIcon) {
            values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE,
                    LauncherSettings.BaseLauncherColumns.ICON_TYPE_BITMAP);
            writeBitmap(values, mIcon);
        } else {
            if (!usingFallbackIcon) {
                writeBitmap(values, mIcon);
            }
            values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE,
                    LauncherSettings.BaseLauncherColumns.ICON_TYPE_RESOURCE);
            if (iconResource != null) {
                values.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE,
                        iconResource.packageName);
                values.put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE,
                        iconResource.resourceName);
            }
        }
    }

    @Override
    public String toString() {
        return "ShortcutInfo(title=" + title + "intent=" + intent + "id=" + this.id
                + " type=" + this.itemType + " container=" + this.container + " screen=" + screenId
                + " cellX=" + cellX + " cellY=" + cellY + " spanX=" + spanX + " spanY=" + spanY
                + " dropPos=" + Arrays.toString(dropPos) + " user=" + user + ")"
                + " SOME ADDITION INFORMATION --> " ;
    }

    public static void dumpShortcutInfoList(String tag, String label,
            ArrayList<ShortcutInfo> list) {
        Log.d(tag, label + " size=" + list.size());
        for (ShortcutInfo info: list) {
            Log.d(tag, "   title=\"" + info.title + " icon=" + info.mIcon
                    + " customIcon=" + info.customIcon);
        }
    }

    public ComponentName getTargetComponent() {
        return promisedIntent != null ? promisedIntent.getComponent() : intent.getComponent();
    }

    public boolean hasStatusFlag(int flag) {
        return (status & flag) != 0;
    }


    public final boolean isPromise() {
        return hasStatusFlag(FLAG_RESTORED_ICON | FLAG_AUTOINTALL_ICON);
    }

    public int getInstallProgress() {
        return mInstallProgress;
    }

    public void setInstallProgress(int progress) {
        mInstallProgress = progress;
        status |= FLAG_INSTALL_SESSION_ACTIVE;
    }
}

