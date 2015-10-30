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

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * CellLayout当中可以存放的总的来说只有两种类型的事物，一种是Shortcut,一种是Widget，当然还有包含Shortcut的Folder
 * 而Hotseat仅仅是在Workspace当中开辟的一片特殊区域，但是这个区域当中存放的也是Shortcut，
 * 顶多还有包含Shortcut的Folder
 */
public class ShortcutAndWidgetContainer extends ViewGroup {

    static final String TAG = "Launcher_CellLayoutChildren";

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    private final int[] mTmpCellXY = new int[2];

    private final WallpaperManager mWallpaperManager;

    private boolean mIsHotseatLayout;

    private int mCellWidth;
    private int mCellHeight;

    private int mWidthGap;
    private int mHeightGap;

    private int mCountX;
    private int mCountY;

    private boolean mInvertIfRtl = false;

    public ShortcutAndWidgetContainer(Context context) {
        super(context);
        mWallpaperManager = WallpaperManager.getInstance(context);
    }

    public void setCellDimensions(int cellWidth, int cellHeight, int widthGap, int heightGap,
            int countX, int countY) {
        mCellWidth = cellWidth;
        mCellHeight = cellHeight;
        mWidthGap = widthGap;
        mHeightGap = heightGap;
        mCountX = countX;
        mCountY = countY;
    }

    public View getChildAt(int x, int y) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

            if ((lp.cellX <= x) && (x < lp.cellX + lp.cellHSpan) &&
                    (lp.cellY <= y) && (y < lp.cellY + lp.cellVSpan)) {
                return child;
            }
        }
        return null;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        @SuppressWarnings("all") // suppress dead code warning
        final boolean debug = false;
        if (debug) {
            // Debug drawing for hit space
            Paint p = new Paint();
            p.setColor(0x6600FF00);
            for (int i = getChildCount() - 1; i >= 0; i--) {
                final View child = getChildAt(i);
                final CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

                canvas.drawRect(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height, p);
            }
        }
        super.dispatchDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSpecSize, heightSpecSize);

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChild(child);
            }
        }
    }

    public void setupLp(CellLayout.LayoutParams lp) {
        lp.setup(mCellWidth, mCellHeight, mWidthGap, mHeightGap, invertLayoutHorizontally(),
                mCountX);
    }

    // Set whether or not to invert the layout horizontally if the layout is in RTL mode.
    public void setInvertIfRtl(boolean invert) {
        mInvertIfRtl = invert;
    }

    public void setIsHotseat(boolean isHotseat) {
        mIsHotseatLayout = isHotseat;
    }

    int getCellContentWidth() {
        final LauncherAppState app = LauncherAppState.getInstance();
        final DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        return Math.min(getMeasuredHeight(), mIsHotseatLayout ?
                grid.hotseatCellWidthPx: grid.cellWidthPx);
    }

    int getCellContentHeight() {
        final LauncherAppState app = LauncherAppState.getInstance();
        final DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        return Math.min(getMeasuredHeight(), mIsHotseatLayout ?
                grid.hotseatCellHeightPx : grid.cellHeightPx);
    }

    public void measureChild(View child) {
        final LauncherAppState app = LauncherAppState.getInstance();
        final DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();

        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

        // lp.isFullScreen用于指示this item should use the full extents of its parent.
        if (!lp.isFullscreen) {
            // 这种情况下，child拥有自己的measure size
            lp.setup(cellWidth, cellHeight, mWidthGap, mHeightGap, invertLayoutHorizontally(),
                    mCountX);

            if (child instanceof LauncherAppWidgetHostView) {
                // Widgets have their own padding, so skip
            } else {
                // Otherwise, center the icon,即处理Shortcut的padding值
                int cHeight = getCellContentHeight();
                int cellPaddingY = (int) Math.max(0, ((lp.height - cHeight) / 2f));
                int cellPaddingX = (int) (grid.edgeMarginPx / 2f);

                child.setPadding(cellPaddingX, cellPaddingY, cellPaddingX, 0);
            }
        } else {
            // 这种情况下，child会使用Parent的measure size
            lp.x = 0;
            lp.y = 0;
            lp.width = getMeasuredWidth();
            lp.height = getMeasuredHeight();
        }

        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width,
                MeasureSpec.EXACTLY);
        int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.height,
                MeasureSpec.EXACTLY);

        child.measure(childWidthMeasureSpec, childheightMeasureSpec);
    }

    private boolean invertLayoutHorizontally() {
        return mInvertIfRtl && isLayoutRtl();
    }

    public boolean isLayoutRtl() {
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL);
    }

    private static final String TAG_RELAYOUT = "Container_reLayout";

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
                int childLeft = lp.x;
                int childTop = lp.y;
                child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);
                Log.d(TAG_RELAYOUT, " the relayout detailed parameter --> childLeft : " + childLeft + "; childTop : " + childTop +
                        "; childRight : " + childLeft + lp.width + "; childBottom : " + childTop + lp.height);
                if (lp.dropped) {
                    lp.dropped = false;
                    Log.d(TAG_RELAYOUT, " we are relayout this child ... ");
                    final int[] cellXY = mTmpCellXY;
                    getLocationOnScreen(cellXY);
                    mWallpaperManager.sendWallpaperCommand(
                            getWindowToken(),
                            WallpaperManager.COMMAND_DROP,
                            cellXY[0] + childLeft + lp.width / 2,
                            cellXY[1] + childTop + lp.height / 2,
                            0,
                            null
                    );
                }
            }
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            view.setDrawingCacheEnabled(enabled);
            // Update the drawing caches
            if (!view.isHardwareAccelerated() && enabled) {
                view.buildDrawingCache(true);
            }
        }
    }

    @Override
    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        super.setChildrenDrawnWithCacheEnabled(enabled);
    }
}
