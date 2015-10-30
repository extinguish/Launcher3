package com.android.launcher3;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Central list of files the Launcher writes to the application data directory.
 *
 * To add a new Launcher file, create a String constant referring to the filename, and add it to
 * ALL_FILES, as shown below.
 */
public class LauncherFiles {

    private static final String XML = ".xml";

    public static final String DEFAULT_WALLPAPER_THUMBNAIL = "default_thumb2.jpg";
    public static final String DEFAULT_WALLPAPER_THUMBNAIL_OLD = "default_thumb.jpg";
    public static final String LAUNCHER_DB = "launcher.db";
    public static final String LAUNCHER_PREFERENCES = "launcher.preferences";
    public static final String LAUNCHES_LOG = "launches.log";
    public static final String SHARED_PREFERENCES_KEY = "com.android.launcher3.prefs";
    public static final String STATS_LOG = "stats.log";
    public static final String WALLPAPER_CROP_PREFERENCES_KEY =
            WallpaperCropActivity.class.getName();
    public static final String WALLPAPER_IMAGES_DB = "saved_wallpaper_images.db";
    public static final String WIDGET_PREVIEWS_DB = "widgetpreviews.db";

    // 对于Collections.unmodifiableList()得到的List来说，这个List就是不在支持所有跟add,remove()等
    // 会改变List当中的内容的操作
    // 而Collections当中的unmodifiableList()会代理对所封装的List的所有的操作
    // 我们在程序的初始创建这个List，就是为了防止在程序运行以后就不会再有相关的文件被创建
    // 所以注意Collections.unmodifiableList的使用
    public static final List<String> ALL_FILES = Collections.unmodifiableList(Arrays.asList(
            DEFAULT_WALLPAPER_THUMBNAIL,
            DEFAULT_WALLPAPER_THUMBNAIL_OLD,
            LAUNCHER_DB,
            LAUNCHER_PREFERENCES,
            LAUNCHES_LOG,
            SHARED_PREFERENCES_KEY + XML,
            STATS_LOG,
            WALLPAPER_CROP_PREFERENCES_KEY + XML,
            WALLPAPER_IMAGES_DB,
            WIDGET_PREVIEWS_DB));
}
