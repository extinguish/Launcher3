/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


// TODO: 如果我们要更改DeleteZone当中对应的动画，那么就从这里开始进行修改
/**
 * Implements a DropTarget.
 */
public class ButtonDropTarget extends TextView implements DropTarget, DragController.DragListener {
    private static final String TAG = "ButtonDropTarget";

    protected final int mTransitionDuration;

    protected Launcher mLauncher;
    private int mBottomDragPadding;
    protected TextView mText;
    protected SearchDropTargetBar mSearchDropTargetBar;

    /** Whether this drop target is active for the current drag */
    protected boolean mActive;

    /** The paint applied to the drag view on hover */
    protected int mHoverColor = 0;

    public ButtonDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ButtonDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Resources r = getResources();
        mTransitionDuration = r.getInteger(R.integer.config_dropTargetBgTransitionDuration);
        mBottomDragPadding = r.getDimensionPixelSize(R.dimen.drop_target_drag_padding);
    }

    void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

    public boolean acceptDrop(DragObject d) {
        return false;
    }

    public void setSearchDropTargetBar(SearchDropTargetBar searchDropTargetBar) {
        mSearchDropTargetBar = searchDropTargetBar;
    }

    protected Drawable getCurrentDrawable() {
        Drawable[] drawables = getCompoundDrawablesRelative();

        for (Drawable drawable : drawables) {
            if (drawable != null ) {
                return drawable;
            }
        }
        return null;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // 以下的方法都是需要在子类当中重新进行实现的
    /////////////////////////////////////////////////////////////////////////////////////////////
    public void onDrop(DragObject d) {
        Log.d(TAG, " on drop the DragObject : " + d.toString());
    }

    public void onFlingToDelete(DragObject d, int x, int y, PointF vec) {
        // Do nothing
        Log.d(TAG, " onFling to delete");
    }

    public void onDragEnter(DragObject d) {
        d.dragView.setColor(mHoverColor);
    }

    public void onDragOver(DragObject d) {
        // Do nothing
        Log.d(TAG, " onDrag Over ... ");
    }

    public void onDragExit(DragObject d) {
        Log.d(TAG, " on drag exit : " + d.toString());
        d.dragView.setColor(0);
    }

    public void onDragStart(DragSource source, Object info, int dragAction) {
        // Do nothing
        Log.d(TAG, " the drag are started ... ");
    }

    public boolean isDropEnabled() {
        Log.d(TAG, " is this drop enabled : " + mActive);
        return mActive;
    }

    public void onDragEnd() {
        // Do nothing
        Log.d(TAG, " the drag end ... ");
    }
    /////////////////////////////////////////////////////////////////////////////////////////////
    // 以上方法都是需要在子类当中进行重新实现的
    /////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void getHitRectRelativeToDragLayer(android.graphics.Rect outRect) {
        super.getHitRect(outRect);
        outRect.bottom += mBottomDragPadding;

        int[] coords = new int[2];
        mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, coords);
        outRect.offsetTo(coords[0], coords[1]);
    }

    private boolean isRtl() {
        return (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
    }

    Rect getIconRect(int viewWidth, int viewHeight, int drawableWidth, int drawableHeight) {
        DragLayer dragLayer = mLauncher.getDragLayer();

        // Find the rect to animate to (the view is center aligned)
        Rect to = new Rect();
        dragLayer.getViewRectRelativeToSelf(this, to);

        final int width = drawableWidth;
        final int height = drawableHeight;

        final int left;
        final int right;

        if (isRtl()) {
            right = to.right - getPaddingRight();
            left = right - width;
        } else {
            left = to.left + getPaddingLeft();
            right = left + width;
        }

        final int top = to.top + (getMeasuredHeight() - height) / 2;
        final int bottom = top +  height;

        to.set(left, top, right, bottom);

        // Center the destination rect about the trash icon
        final int xOffset = (int) -(viewWidth - width) / 2;
        final int yOffset = (int) -(viewHeight - height) / 2;
        to.offset(xOffset, yOffset);

        return to;
    }

    public void getLocationInDragLayer(int[] loc) {
        mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }
}
