/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * 在EUI当中，当我们从Hotseat当中移除一个Icon时，那么身下的Icon就会自动调整自己在Hotseat
 * 当中的位置，即四个图标和三个图标的相对位置和距离会自动调整的安排,
 * 然后Hotseat当中最多可以存放5个图标，当然了Hotseat当中还可以存放文件夹，即如果存放的图标的数目
 * 超过5个之后，我们再向Hotseat当中拖放图标，就只能形成Folder了
 * TODO: 所以我们要开发自己的Launcher的话，以上就是我们的目标
 *
 * The hotseat layout controller,
 * The AllAppsButton should never changed or removed from the
 * Hotseat column
 *
 */
// 可以看到Hotseat本身也是一个CellLayout的实现
public class Hotseat extends FrameLayout {

    private static final String TAG = "Launcher3_Hotseat";

    private CellLayout mContent;

    private Launcher mLauncher;

    private int mAllAppsButtonRank;

    private boolean mTransposeLayoutWithOrientation;
    private boolean mIsLandscape;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Resources r = context.getResources();

        mTransposeLayoutWithOrientation =
                r.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);
        mIsLandscape = context.getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
    }

    // hook the Hotseat into the Launcher implementation,
    // as the Hotseat need some custom function that provided by the Launcher
    // internal implementation
    public void setup(Launcher launcher) {
        mLauncher = launcher;
    }

    CellLayout getLayout() {
        return mContent;
    }

    /**
     * Registers the specified listener on the cell layout of the hotseat.
     */
    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        // we do not handle the OnLongClickListener in the HotSeat directly,
        // but delegate the real OnLongClickListener to the CellLayout,
        // and the CellLayout will responsible to handle all the
        // OnClick and OnLongClick events
        mContent.setOnLongClickListener(l);
    }
  
    private boolean hasVerticalHotseat() {
        // If the current screen orientation is in Landscape mode,
        // we should make the Hotseat layout vertically in the left side
        // of the current screen
        return (mIsLandscape && mTransposeLayoutWithOrientation);
    }

    /* Get the orientation invariant order of the item in the hotseat for persistence. */
    int getOrderInHotseat(int x, int y) {
        return hasVerticalHotseat() ? (mContent.getCountY() - y - 1) : x;
    }
    /* Get the orientation specific coordinates given an invariant order in the hotseat. */
    int getCellXFromOrder(int rank) {
        return hasVerticalHotseat() ? 0 : rank;
    }
    int getCellYFromOrder(int rank) {
        return hasVerticalHotseat() ? (mContent.getCountY() - (rank + 1)) : 0;
    }

    public boolean isAllAppsButtonRank(int rank) {
        if (LauncherAppState.isDisableAllApps()) {
            return false;
        } else {
            return rank == mAllAppsButtonRank;
        }
    }

    /** This returns the coordinates of an app in a given cell, relative to the DragLayer */
    Rect getCellCoordinates(int cellX, int cellY) {
        Rect coords = new Rect();
        mContent.cellToRect(cellX, cellY, 1, 1, coords);
        int[] hotseatInParent = new int[2];
        Utilities.getDescendantCoordRelativeToParent(this, mLauncher.getDragLayer(),
                hotseatInParent, false);
        coords.offset(hotseatInParent[0], hotseatInParent[1]);

        // 得到Cell的坐标位置
        // Center the icon
        int cWidth = mContent.getShortcutsAndWidgets().getCellContentWidth();
        int cHeight = mContent.getShortcutsAndWidgets().getCellContentHeight();

        int cellPaddingX = (int) Math.max(0, ((coords.width() - cWidth) / 2f));
        int cellPaddingY = (int) Math.max(0, ((coords.height() - cHeight) / 2f));
        coords.offset(cellPaddingX, cellPaddingY);

        return coords;
    }

    /**
     * Finalize inflating a view from XML.  This is called as the last phase
     * of inflation, after all child views have been added.
     *
     */
    @Override
    protected void onFinishInflate() {

        /**
         * Even if the subclass overrides onFinishInflate, they should always be
         * sure to call the super method, so that we get called.
         */
        // 我们可以看到在FrameLayout当中这个方法当中并没有提供具体的实现，
        // 但是这个方法在FrameLayout当中相当于一个生命周期的回调，我们
        // 必须调用，因为之后这样，才能保证我们在子类，即hotseat当中提供的
        // 实现才能得到调用
        super.onFinishInflate();
        // 读取app的状态
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();

        mAllAppsButtonRank = grid.hotseatAllAppsRank;
        mContent = (CellLayout) findViewById(R.id.layout);
        if (grid.isLandscape && !grid.isLargeTablet()) {
            // 当我们是位于Landscape时，我们的Launcher的Hotseat的大小
            mContent.setGridSize(
                    1, // x coordinate
                    (int) grid.numHotseatIcons // y coordinate
            );
        } else {
            // 当我们是位于Portrait模式时，我们的Launcher的Hotseat的大小
            mContent.setGridSize(
                    (int) grid.numHotseatIcons,  // y coordinate
                    1 // x coordinate
            );
        }

        // 设置下面的这个isHotseat标记的作用很简单，就是用于标记以后是否在每一个Shortcut下面添加Title字段
        // 是的话就添加，否则就不添加
        mContent.setIsHotseat(true);

        resetLayout();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void resetLayout() {
        // 在重新布局之前需要先将所有位于Hotseat当中的Icon删除掉
        // 然后再进行重新加载
        mContent.removeAllViewsInLayout();

        // 在传统的Docker还有的时候，每当我们重新加载整个布局时，可以看到Hotseat当中所有的
        // 之前加载的图标都不在了，但是这个AllAppsButton确一直都在
        if (!LauncherAppState.isDisableAllApps()) {
            // 以下的整个过程就是重新加载AllAppsButton的过程
            // 也就是说如果我们需要进行resetLayout的过程的话，我们需要的仅仅是
            // 重新设置和加载AllAppsButton的过程
            //
            // Add the Apps button
            Context context = getContext();

            LayoutInflater inflater = LayoutInflater.from(context);
            // AllAppsButton就是一个TextView,即TextView直接设置textViewDrawable属性来加载这个
            // AllAppsButton来实现
            TextView allAppsButton = (TextView)
                    inflater.inflate(R.layout.all_apps_button, mContent, false);

            Drawable dockerDrawable;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 在5.0之后，系统对于drawable的获取需要强制添加一个theme属性，
                // 其实这个方法很强大，以下是来自StackOverflow上面的哥们对这个方法的解释:
                /**
                 * As of API 22, you should use the getDrawable(int, Theme) method
                 * instead of getDrawable(int), as it allows you to fetch a drawable
                 * object associated with a particular resource ID for the given screen
                 * density/theme. Calling the deprecated getDrawable(int) method is
                 * equivalent to calling getDrawable(int, null).
                 */
                dockerDrawable = context.getResources().getDrawable(R.drawable.all_apps_button_icon, null);
            } else {
                // 其实如果我们想要提供更好的关于这个方法的兼容性，我们可以直接使用来自support包当中提供的
                // ContextCompat.getDrawable(context, R.drawable.***)
                // 这样这个方法也提供了在获取drawable实例的同时还能直接指定这个drawable的style
                // 当然了这个drawable的theme最好是直接根据当前的屏幕的分辨率来直接进行优化
                dockerDrawable = context.getResources().getDrawable(R.drawable.all_apps_button_icon);
            }

            Utilities.resizeIconDrawable(dockerDrawable);
            // allAppsButton我们是用一个TextView来进行显示的，
            // 不过无所谓，反正现在我们也不需要这个AllAppsButton了
            allAppsButton.setCompoundDrawables(
                    null,  // left
                    dockerDrawable, // top
                    null,  // right
                    null // bottom
            );

            allAppsButton.setContentDescription(context.getString(R.string.all_apps_button_label));
            allAppsButton.setOnKeyListener(new HotseatIconKeyEventListener());
            if (mLauncher != null) {
                // 添加AllAppsButton点击时的震动效果
                allAppsButton.setOnTouchListener(mLauncher.getHapticFeedbackTouchListener());
                mLauncher.setAllAppsButton(allAppsButton);
                allAppsButton.setOnClickListener(mLauncher);
                allAppsButton.setOnFocusChangeListener(mLauncher.mFocusHandler);
            }

            // Note: We do this to ensure that the hotseat is always laid out in the orientation of
            // the hotseat in order regardless of which orientation they were added
            int x = getCellXFromOrder(mAllAppsButtonRank);
            int y = getCellYFromOrder(mAllAppsButtonRank);
            CellLayout.LayoutParams lp = new CellLayout.LayoutParams(x,y,1,1);
            lp.canReorder = false;

            // TODO: 取消加载下面的AllAppsButton，然后将这个AllAppsButton替换成我们预定义的App的Icon
            // TODO: 而这个App需要经过组长来确定具体的App
            // TODO: 注意，我们不再需要AllAppsButton了，我们仅仅是需要保留AppsCustomizePagedView
            // TODO: 但是同时也要注意，我们这里的实现逻辑很渣，仅仅就是简单的把这个AllAppsButton注释掉了
            // TODO: 但是Hotseat还是把这个位置保留着，即每次我们重新启动Launcher时，AllAppsButton的位置就会
            // TODO: 空出来，我们需要重新改进这个地方
            mContent.addViewToCellLayout(allAppsButton, -1, allAppsButton.getId(), lp, true);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // We don't want any clicks to go through to the hotseat unless the workspace is in
        // the normal state.
        if (mLauncher.getWorkspace().workspaceInModalState()) {
            return true;
        }
        return false;
    }

    /**
     * 当所有的App都是出于不可用的状态时，我们创建一个单独的Folder，然后把我们所有的程序放入到这个
     * Folder当中
     *
     * @param iconCache
     * @param allApps
     * @param onWorkspace
     * @param launcher
     * @param workspace
     */
    void addAllAppsFolder(IconCache iconCache,
            ArrayList<AppInfo> allApps, ArrayList<ComponentName> onWorkspace,
            Launcher launcher, Workspace workspace) {

        if (LauncherAppState.isDisableAllApps()) {
            // 目前是位于禁止所有App的情况
            Log.d(TAG, " add all apps folder ");
            FolderInfo fi = new FolderInfo();

            fi.cellX = getCellXFromOrder(mAllAppsButtonRank);
            fi.cellY = getCellYFromOrder(mAllAppsButtonRank);
            fi.spanX = 1;
            fi.spanY = 1;
            fi.container = LauncherSettings.Favorites.CONTAINER_HOTSEAT;
            fi.screenId = mAllAppsButtonRank;
            fi.itemType = LauncherSettings.Favorites.ITEM_TYPE_FOLDER;
            fi.title = "More Apps";
            LauncherModel.addItemToDatabase(launcher, fi, fi.container, fi.screenId, fi.cellX,
                    fi.cellY, false);
            FolderIcon folder = FolderIcon.fromXml(R.layout.folder_icon, launcher,
                    getLayout(), fi, iconCache);
            workspace.addInScreen(folder, fi.container, fi.screenId, fi.cellX, fi.cellY,
                    fi.spanX, fi.spanY);

            for (AppInfo info: allApps) {
                ComponentName cn = info.intent.getComponent();
                if (!onWorkspace.contains(cn)) {
                    Log.d(TAG, "Adding to 'more apps': " + info.intent);
                    ShortcutInfo si = info.makeShortcut();
                    fi.add(si);
                }
            }
        }
    }

    void addAppsToAllAppsFolder(ArrayList<AppInfo> apps) {
        if (LauncherAppState.isDisableAllApps()) {
            View v = mContent.getChildAt(getCellXFromOrder(mAllAppsButtonRank), getCellYFromOrder(mAllAppsButtonRank));
            FolderIcon fi = null;

            if (v instanceof FolderIcon) {
                fi = (FolderIcon) v;
            } else {
                return;
            }

            FolderInfo info = fi.getFolderInfo();
            for (AppInfo a: apps) {
                ShortcutInfo si = a.makeShortcut();
                info.add(si);
            }
        }
    }
}
