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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * TODO: ------------------ ALERT -------------------------------------------
 * TODO: 防止Workspace当中创建重复的Shortcut分为两种情况，第一种就是
 * TODO: 在LauncherModel初始化的时候直接读取到的重复的Shortcut，然后就创建了
 * TODO: 重复的Shortcut(关于这部分问题的解决方案就是通过LauncherModel类的部分改进)；
 * TODO: 第二种情况就是我们在Launcher运行的过程当中主动创建一个Shortcut，也就是
 * TODO: 通过InstallShortcutReceiver方法来进行的(这种情况下主要是侦测到了新程序的
 * TODO: 安装，或者干脆普通的App申请了INSTALL_SHORTCUT的权限，然后触发了这个事件,例如今
 * TODO: 日头条在程序首次安装时，会提示用户是否在桌面添加快捷方式)，
 * TODO: 结果就安装了一个新的Shortcut，但是这个过程当中InstallShortcutReceiver并没有
 * TODO: 施加关于Shortcut是否已经重复存在的检查,但是EUI就会检测，在这个过程当中会弹出一个
 * TODO: Toast告诉用户相关的Shortcut已经存在了
 *
 *
 */
public class InstallShortcutReceiver extends BroadcastReceiver {

    private static final String TAG = "InstallShortcutReceiver";

    private static final boolean DBG = true;

    private static final String ACTION_INSTALL_SHORTCUT =
            "com.android.launcher.action.INSTALL_SHORTCUT";

    private static final String LAUNCH_INTENT_KEY = "intent.launch";
    private static final String NAME_KEY = "name";
    private static final String ICON_KEY = "icon";
    private static final String ICON_RESOURCE_NAME_KEY = "iconResource";
    private static final String ICON_RESOURCE_PACKAGE_NAME_KEY = "iconResourcePackage";

    private static final String APP_SHORTCUT_TYPE_KEY = "isAppShortcut";
    private static final String USER_HANDLE_KEY = "userHandle";

    // The set of shortcuts that are pending install
    private static final String APPS_PENDING_INSTALL = "apps_to_install";

    public static final int NEW_SHORTCUT_BOUNCE_DURATION = 450;
    public static final int NEW_SHORTCUT_STAGGER_DELAY = 85;

    private static final Object sLock = new Object();

    // 将我们要添加的ShortcutInfo保存到SharedPreference进行持久化
    private static void addToInstallQueue(
            SharedPreferences sharedPrefs, PendingInstallShortcutInfo info) {
        Log.d(TAG, " perform the true adding process from here ... ");
        synchronized (sLock) {
            String encoded = info.encodeToString();
            if (encoded != null) {
                Set<String> strings = sharedPrefs.getStringSet(APPS_PENDING_INSTALL, null);
                if (strings == null) {
                    strings = new HashSet<>(1);
                } else {
                    strings = new HashSet<>(strings);
                }
                strings.add(encoded);
                sharedPrefs.edit().putStringSet(APPS_PENDING_INSTALL, strings).commit();
            }
        }
    }

    public static void removeFromInstallQueue(Context context, ArrayList<String> packageNames,
                                              UserHandleCompat user) {
        if (packageNames.isEmpty()) {
            return;
        }
        String spKey = LauncherAppState.getSharedPreferencesKey();
        SharedPreferences sp = context.getSharedPreferences(spKey, Context.MODE_PRIVATE);
        synchronized (sLock) {
            Set<String> strings = sp.getStringSet(APPS_PENDING_INSTALL, null);
            if (DBG) {
                Log.d(TAG, "APPS_PENDING_INSTALL: " + strings
                        + ", removing packages: " + packageNames);
            }
            if (strings != null) {
                Set<String> newStrings = new HashSet<>(strings);
                Iterator<String> newStringsIter = newStrings.iterator();
                while (newStringsIter.hasNext()) {
                    String encoded = newStringsIter.next();
                    PendingInstallShortcutInfo info = decode(encoded, context);
                    if (info == null || (packageNames.contains(info.getTargetPackage())
                            && user.equals(info.user))) {
                        newStringsIter.remove();
                    }
                }
                sp.edit().putStringSet(APPS_PENDING_INSTALL, newStrings).commit();
            }
        }
    }

    private static ArrayList<PendingInstallShortcutInfo> getAndClearInstallQueue(
            SharedPreferences sharedPrefs, Context context) {
        synchronized (sLock) {
            // 果然存储小量的数据，我们一般还是采用SharedPreference来进行存储，
            // 关键是这些数据本身是对顺序不敏感的，所以我们可以直接用Set<String>来进行保存
            // 在其他地方我们还可以看到在SharedPreference当中使用Json来直接保存我们需要存储的结构化数据
            // 因为Json可以代表复杂的结构化数据，然后我们可以直接把这个JsonObject转换成String，
            // 然后直接存储到SharedPreference当中，然后读取的时候直接逆向解析以下就可以了
            Set<String> strings = sharedPrefs.getStringSet(APPS_PENDING_INSTALL, null);

            if (strings == null) {
                return new ArrayList<>();
            }
            ArrayList<PendingInstallShortcutInfo> infos =
                    new ArrayList<>();
            for (String encoded : strings) {
                PendingInstallShortcutInfo info = decode(encoded, context);
                if (info != null) {
                    infos.add(info);
                }
            }
            // 然后我们在SharedPreference当中直接存储一个空的HashSet<>，这个过程就相当于将
            // 我们的请求队列清空了, 因为我们是通过将Json Object装换成String保存了起来，所以就OK了
            // TODO: 另外我们需要注意的就是SharedPreference.commit()真的比SharedPreference.apply()耗费
            // TODO: 更多的时间和资源，所以深入调研一下下面的commit()方法再替换成apply()方法是否会影响到
            // TODO: 具体的逻辑处理过程
            sharedPrefs.edit().putStringSet(APPS_PENDING_INSTALL, new HashSet<String>()).commit();
            return infos;
        }
    }

    // Determines whether to defer installing shortcuts immediately until
    // processAllPendingInstalls() is called.
    private static boolean mUseInstallQueue = false;

    // 当程序在安装完之后主动要求创建一个快捷方式时，下面的onReceive就会被触发
    public void onReceive(Context context, Intent data) {
        Log.d(TAG, " in InstallShortcutReceiver --> we have received the action : " + data.getAction().toString());
        if (!ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }

        // TODO: 在下面我们打出的Log当中，显示的data.toUri(0)当中有一条信息，就是:
        // TODO: #Intent;action=com.android.launcher.action.INSTALL_SHORTCUT;
        // TODO: launchFlags=0x10;
        // TODO: component=com.android.launcher3/.InstallShortcutReceiver;
        // TODO: B.duplicate=false;
        // TODO: S.android.intent.extra.shortcut.NAME=%E4%BB%8A%E6%97%A5%E5%A4%B4%E6%9D%A1;
        // TODO: end
        // TODO: 以上就是全部数据了，我们可以看到B.duplicate=false的字段，如果这个字段为true的话，获取
        // TODO: 以下的过程就不会发生了
        if (DBG) Log.d(TAG, "Got INSTALL_SHORTCUT: " + data.toUri(0));
        PendingInstallShortcutInfo info = new PendingInstallShortcutInfo(data, context);

        queuePendingShortcutInfo(info, context);
    }

    static void queueInstallShortcut(LauncherActivityInfoCompat info, Context context) {
        queuePendingShortcutInfo(new PendingInstallShortcutInfo(info, context), context);
    }

    private static void queuePendingShortcutInfo(PendingInstallShortcutInfo info, Context context) {
        Log.d(TAG, " add this pending shortcut info into the queue ... ");
        // Queue the item up for adding if launcher has not loaded properly yet
        LauncherAppState.setApplicationContext(context.getApplicationContext());
        LauncherAppState app = LauncherAppState.getInstance();
        // 下面检查Launcher是否启动的方法跟简单，就是判断LauncherModel的getCallback()方法的返回值
        // 因为LauncherModel.getCallback()返回的callback实例就是Launcher，而这个Callback初始化
        // 就是在Launcher类的onCreate()方法当中进行
        boolean launcherNotLoaded = app.getModel().getCallback() == null;

        String spKey = LauncherAppState.getSharedPreferencesKey();
        SharedPreferences sp = context.getSharedPreferences(spKey, Context.MODE_PRIVATE);
        // 将要添加的ShortcutInfo添加到SharedPreference当中
        addToInstallQueue(sp, info);
        if (!mUseInstallQueue && !launcherNotLoaded) {
            flushInstallQueue(context);
        }
    }

    static void enableInstallQueue() {
        mUseInstallQueue = true;
    }

    static void disableAndFlushInstallQueue(Context context) {
        mUseInstallQueue = false;
        flushInstallQueue(context);
    }

    /**
     * 将所有需要安装的Shortcut一起安装了
     *
     * @param context
     */
    static void flushInstallQueue(Context context) {
        Log.d(TAG, " start flush the install queue, and the detailed process is to put this ShortcutInfo into the SharedPreference... ");
        // 可以参考之前我们将所有安装的Shortcut是如何保存在SharedPreference当中的
        String spKey = LauncherAppState.getSharedPreferencesKey();
        SharedPreferences sp = context.getSharedPreferences(spKey, Context.MODE_PRIVATE);
        ArrayList<PendingInstallShortcutInfo> installQueue = getAndClearInstallQueue(sp, context);

        if (!installQueue.isEmpty()) {
            Log.d(TAG, " the installQueue are not empty ... ");
            Iterator<PendingInstallShortcutInfo> iter = installQueue.iterator();
            ArrayList<ItemInfo> addShortcuts = new ArrayList<>();
            while (iter.hasNext()) {
                final PendingInstallShortcutInfo pendingInfo = iter.next();
                final Intent intent = pendingInfo.launchIntent;

                if (LauncherAppState.isDisableAllApps() && !isValidShortcutLaunchIntent(intent)) {
                    if (DBG) Log.d(TAG, "Ignoring shortcut with launchIntent:" + intent);
                    continue;
                }

                // If the intent specifies a package, make sure the package exists
                String packageName = pendingInfo.getTargetPackage();
                if (!TextUtils.isEmpty(packageName)) {
                    UserHandleCompat myUserHandle = UserHandleCompat.myUserHandle();
                    if (!LauncherModel.isValidPackage(context, packageName, myUserHandle)) {
                        if (DBG) Log.d(TAG, "Ignoring shortcut for absent package:" + intent);
                        continue;
                    }
                }

                // TODO: 下面这个判断Shortcut的逻辑有些问题，不是我们需要的逻辑，我们需要的逻辑是
                // TODO: Launcher3当中所有的App都是存在存在于Workspace当中的，即如果这个App存在于
                // TODO: Workspace当中，那么我们就不能再次添加重复的Shortcut
                // TODO: 但是目前的判断逻辑不是这样的
                Log.d(TAG, " the shortcut we need to evaluate are --> label : " + pendingInfo.label + "\n intent : " + intent + "\n user : " + pendingInfo.user);
                final boolean exists = LauncherModel.shortcutExists(context, pendingInfo.label,
                        intent, pendingInfo.user);

                Log.d(TAG, " does this shortcut exists ? " + exists);
                if (!exists) {
                    Log.d(TAG, " this shortcut does not exists, so we add this shortcut into the addShortcuts queue, " +
                            " and the queue can handle it...");
                    // Generate a shortcut info to add into the model
                    addShortcuts.add(pendingInfo.getShortcutInfo());
                    Log.d(TAG, " the shortcut info we need to add are : " + pendingInfo.getShortcutInfo().toString());
                }
            }

            // Add the new apps to the model and bind them
            if (!addShortcuts.isEmpty()) {
                LauncherAppState app = LauncherAppState.getInstance();
                app.getModel().addAndBindAddedWorkspaceApps(context, addShortcuts);
            }
        }
    }

    /**
     * Returns true if the intent is a valid launch intent for a shortcut.
     * (需要考虑到创建Shortcut不仅仅是一个程序的MainActivity，还有可能是这个程序自己也可以创建
     * Shortcut，而自己创建的Shortcut所对应的Intent就不仅仅是MainActivity这个Activity了，所以
     * 我们要在这里验证Intent的合法性)
     * This is used to identify shortcuts which are different from the ones exposed by the
     * applications' manifest file.
     *
     * <p/>
     * When DISABLE_ALL_APPS is true, shortcuts exposed via the app's manifest should never be
     * duplicated or removed(unless the app is un-installed).
     *
     * @param launchIntent The intent that will be launched when the shortcut is clicked.
     */
    static boolean isValidShortcutLaunchIntent(Intent launchIntent) {
        if (launchIntent != null
                && Intent.ACTION_MAIN.equals(launchIntent.getAction())
                && launchIntent.getComponent() != null
                && launchIntent.getCategories() != null
                && launchIntent.getCategories().size() == 1
                && launchIntent.hasCategory(Intent.CATEGORY_LAUNCHER)
                && launchIntent.getExtras() == null
                && TextUtils.isEmpty(launchIntent.getDataString())) {
            return false;
        }
        return true;
    }

    /**
     * Ensures that we have a valid, non-null name.  If the provided name is null, we will return
     * the application name instead.
     */
    private static CharSequence ensureValidName(Context context, Intent intent, CharSequence name) {
        if (name == null) {
            try {
                PackageManager pm = context.getPackageManager();
                ActivityInfo info = pm.getActivityInfo(intent.getComponent(), 0);
                name = info.loadLabel(pm).toString();
            } catch (PackageManager.NameNotFoundException nnfe) {
                return "";
            }
        }
        // 如果name不为空，那么此时这个name的值就被赋予了这个程序的名字
        // 在调用ensureValidName()这个方法的地方所传的参数当中的第二个参数
        // 就是这个程序的名字
        return name;
    }

    private static class PendingInstallShortcutInfo {

        final LauncherActivityInfoCompat activityInfo;

        final Intent data;
        final Context mContext;
        final Intent launchIntent;
        final String label;
        final UserHandleCompat user;

        /**
         * Initializes a PendingInstallShortcutInfo received from a different app.
         */
        public PendingInstallShortcutInfo(Intent data, Context context) {
            this.data = data;
            mContext = context;

            launchIntent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            label = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
            user = UserHandleCompat.myUserHandle();
            activityInfo = null;
        }

        /**
         * Initializes a PendingInstallShortcutInfo to represent a launcher target.(即可启动的Shortcut)
         */
        public PendingInstallShortcutInfo(LauncherActivityInfoCompat info, Context context) {
            this.data = null;
            mContext = context;
            activityInfo = info;
            user = info.getUser();

            launchIntent = AppInfo.makeLaunchIntent(context, info, user);
            label = info.getLabel().toString();
        }

        public String encodeToString() {
            if (activityInfo != null) {
                try {
                    // If it a launcher target, we only need component name, and user to
                    // recreate this.
                    return new JSONStringer()
                            .object()
                            .key(LAUNCH_INTENT_KEY).value(launchIntent.toUri(0))
                            .key(APP_SHORTCUT_TYPE_KEY).value(true)
                            .key(USER_HANDLE_KEY).value(UserManagerCompat.getInstance(mContext)
                                    .getSerialNumberForUser(user))
                            .endObject().toString();
                } catch (JSONException e) {
                    Log.d(TAG, "Exception when adding shortcut: " + e);
                    return null;
                }
            }

            if (launchIntent.getAction() == null) {
                launchIntent.setAction(Intent.ACTION_VIEW);
            } else if (launchIntent.getAction().equals(Intent.ACTION_MAIN) &&
                    launchIntent.getCategories() != null &&
                    launchIntent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
                launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            }

            // This name is only used for comparisons and notifications, so fall back to activity
            // name if not supplied
            String name = ensureValidName(mContext, launchIntent, label).toString();
            Bitmap icon = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
            Intent.ShortcutIconResource iconResource =
                    data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);

            // Only encode the parameters which are supported by the API.
            try {
                JSONStringer json = new JSONStringer()
                        .object()
                        .key(LAUNCH_INTENT_KEY).value(launchIntent.toUri(0))
                        .key(NAME_KEY).value(name);
                if (icon != null) {
                    byte[] iconByteArray = ItemInfo.flattenBitmap(icon);
                    json = json.key(ICON_KEY).value(
                            Base64.encodeToString(
                                    iconByteArray, 0, iconByteArray.length, Base64.DEFAULT));
                }
                if (iconResource != null) {
                    json = json.key(ICON_RESOURCE_NAME_KEY).value(iconResource.resourceName);
                    json = json.key(ICON_RESOURCE_PACKAGE_NAME_KEY)
                            .value(iconResource.packageName);
                }
                return json.endObject().toString();
            } catch (JSONException e) {
                Log.d(TAG, "Exception when adding shortcut: " + e);
            }
            return null;
        }

        public ShortcutInfo getShortcutInfo() {
            if (activityInfo != null) {
                final ShortcutInfo info = new ShortcutInfo();
                info.user = user;
                info.title = label;
                info.contentDescription = label;
                info.customIcon = false;
                info.intent = launchIntent;
                info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
                info.flags = AppInfo.initFlags(activityInfo);
                info.firstInstallTime = activityInfo.getFirstInstallTime();
                return info;
            } else {
                return LauncherAppState.getInstance().getModel().infoFromShortcutIntent(mContext, data);
            }
        }

        public String getTargetPackage() {
            String packageName = launchIntent.getPackage();
            if (packageName == null) {
                packageName = launchIntent.getComponent() == null ? null :
                        launchIntent.getComponent().getPackageName();
            }
            return packageName;
        }
    }

    private static PendingInstallShortcutInfo decode(String encoded, Context context) {
        if (TextUtils.isEmpty(encoded)) Log.d(TAG, " the encoded pendingInstallShortcutInfo string are : " + encoded);

        try {
            JSONObject object = (JSONObject) new JSONTokener(encoded).nextValue();
            Intent launcherIntent = Intent.parseUri(object.getString(LAUNCH_INTENT_KEY), 0);

            if (object.optBoolean(APP_SHORTCUT_TYPE_KEY)) {
                // The is an internal launcher target shortcut.
                UserHandleCompat user = UserManagerCompat.getInstance(context)
                        .getUserForSerialNumber(object.getLong(USER_HANDLE_KEY));
                if (user == null) {
                    return null;
                }

                LauncherActivityInfoCompat info = LauncherAppsCompat.getInstance(context)
                        .resolveActivity(launcherIntent, user);
                return info == null ? null : new PendingInstallShortcutInfo(info, context);
            }

            Intent data = new Intent();
            data.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launcherIntent);
            data.putExtra(Intent.EXTRA_SHORTCUT_NAME, object.getString(NAME_KEY));

            String iconBase64 = object.optString(ICON_KEY);
            String iconResourceName = object.optString(ICON_RESOURCE_NAME_KEY);
            String iconResourcePackageName = object.optString(ICON_RESOURCE_PACKAGE_NAME_KEY);
            if (iconBase64 != null && !iconBase64.isEmpty()) {
                byte[] iconArray = Base64.decode(iconBase64, Base64.DEFAULT);
                Bitmap b = BitmapFactory.decodeByteArray(iconArray, 0, iconArray.length);
                data.putExtra(Intent.EXTRA_SHORTCUT_ICON, b);
            } else if (iconResourceName != null && !iconResourceName.isEmpty()) {
                Intent.ShortcutIconResource iconResource =
                        new Intent.ShortcutIconResource();
                iconResource.resourceName = iconResourceName;
                iconResource.packageName = iconResourcePackageName;
                data.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
            }

            return new PendingInstallShortcutInfo(data, context);
        } catch (JSONException e) {
            Log.d(TAG, "Exception reading shortcut to add: " + e);
        } catch (URISyntaxException e) {
            Log.d(TAG, "Exception reading shortcut to add: " + e);
        }
        return null;
    }
}
