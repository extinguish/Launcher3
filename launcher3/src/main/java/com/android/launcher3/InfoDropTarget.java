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

import android.content.ComponentName;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.compat.UserHandleCompat;

/**
 * InfoDropTarget的作用同DeleteDropTarget出现的位置是一样的，都是在这个CellLayout
 * 的最上面，当我们将程序的图标拖动到这里的时候，如果不是系统程序的话，那么DeleteDropTarget
 * 和InfoDropTarget就会同时显示，如果时系统的程序(任何不可以删除的程序),那么这里显示的
 * 就只有InfoDropTarget了
 * 他们都是直接继承自ButtonDropTarget,而ButtonDropTarget本身这是直接implement
 * DropTarget
 *
 */
public class InfoDropTarget extends ButtonDropTarget {

    private static final String TAG = "InfoDropTarget";

    private ColorStateList mOriginalTextColor;
    private TransitionDrawable mDrawable;

    public InfoDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InfoDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mOriginalTextColor = getTextColors();

        // Get the hover color
        Resources r = getResources();
        mHoverColor = r.getColor(R.color.info_target_hover_tint);
        mDrawable = (TransitionDrawable) getCurrentDrawable();

        if (mDrawable == null) {
            // TODO: 正常情况下，mDrawable对象应该被很顺利的得到，而不应该发生下面这样的逻辑过程
            // TODO: investigate why this is ever happening. Presently only on one known device.
            mDrawable = (TransitionDrawable) r.getDrawable(R.drawable.info_target_selector);
            setCompoundDrawablesRelativeWithIntrinsicBounds(mDrawable, null, null, null);
        }

        if (null != mDrawable) {
            mDrawable.setCrossFadeEnabled(true);
        }

        // Remove the text in the Phone UI in landscape
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!LauncherAppState.getInstance().isScreenLarge()) {
                setText("");
            }
        }
    }

    @Override
    public boolean acceptDrop(DragObject d) {
        Log.d(TAG, " accept drop : " + d.toString());
        // acceptDrop is called just before onDrop. We do the work here, rather than
        // in onDrop, because it allows us to reject the drop (by returning false)
        // so that the object being dragged isn't removed from the drag source.
        ComponentName componentName = null;
        if (d.dragInfo instanceof AppInfo) {
            Log.d(TAG, " the drag object are instance of AppInfo ... ");
            componentName = ((AppInfo) d.dragInfo).componentName;
        } else if (d.dragInfo instanceof ShortcutInfo) {
            Log.d(TAG, " the drag object are instance of ShortcutInfo ... ");
            componentName = ((ShortcutInfo) d.dragInfo).intent.getComponent();
        } else if (d.dragInfo instanceof PendingAddItemInfo) {
            Log.d(TAG, " the drag object are instance of PendingAddItemInfo ... ");
            componentName = ((PendingAddItemInfo) d.dragInfo).componentName;
        }
        final UserHandleCompat user;
        if (d.dragInfo instanceof ItemInfo) {
            user = ((ItemInfo) d.dragInfo).user;
        } else {
            user = UserHandleCompat.myUserHandle();
        }

        if (componentName != null) {
            mLauncher.startApplicationDetailsActivity(componentName, user);
        }

        // There is no post-drop animation, so clean up the DragView now
        d.deferDragViewCleanupPostAnimation = false;
        return false;
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        Log.d(TAG, " the object we are dragging are : " + info.toString());

        // 如果我们是从AllApps 界面Drag一个Item的话，就显示这个InfoDropTarget，否则这个InfoDropTarget是不显示的
        // 在最新的Launcher实现当中，我们需要在Workspace当中Drag item时也需要显示这个InfoDropTarget
        // 但是我们现在有新的逻辑，即当我们要Drag的Item是一个Folder时，由于我们是无法删除，但是也
        // 是不需要显示这个Info target的
        boolean isVisible = true;
        // Hide this button unless we are dragging something from AllApps
        // 我们在Workspace当中也更改了这个属性
        if (!source.supportsAppInfoDropTarget()) {
            isVisible = false;
        }

        // 当我们要删除的是普通的Folder时，是不可以直接将这个Folder从Launcher当中进行删除的
        // hide this button if the object we are dragging are folderInfo
        if (info instanceof FolderInfo) {
            isVisible = false;
        }

        // 如果这个ItemInfo是由第三方程序自己创建的Shortcut，那么
        // 我们是不需要显示这个InfoDropTarget的，我们所需要的就是简单的显示
        // 一个Remove Label就Ok了
        if (info instanceof ItemInfo) {
            ItemInfo itemInfo = (ItemInfo) info;
            if (itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET ||
                    itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                isVisible = false;
            }
        }

        mActive = isVisible;
        mDrawable.resetTransition();
        setTextColor(mOriginalTextColor);
        // 这里InfoDropTarget的parent就是SearchDropTargetBar
        // 所以这里可以直接控制整个DeleteZone的可见性
        // TODO: 我们在这里遇到了一个Bug，就是每当我们将Item drag都这个InfoDropTargetBar当中时，
        // TODO: 会自动触发DeleteDropTargetBar上面的操作，然后直接导致这个ItemInfo直接
        // TODO: 在CellLayout当中的状态变成了INVISIBLE，这不是我们希望的
        // TODO: 因为这个过程并没有将Item直接remove掉，而是直接变成了INVISIBLE,
        // TODO: 导致我们都无法调试
        ((ViewGroup) getParent()).setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDragEnd() {
        super.onDragEnd();
        Log.d(TAG, " onDrag end ... ");
        mActive = false;
    }

    public void onDragEnter(DragObject d) {
        Log.d(TAG, " on drag enter ... " + d.toString());
        super.onDragEnter(d);

        mDrawable.startTransition(mTransitionDuration);
        setTextColor(mHoverColor);
    }

    public void onDragExit(DragObject d) {
        super.onDragExit(d);
        Log.d(TAG, " on drag exit ... " + d.toString());
        if (!d.dragComplete) {
            mDrawable.resetTransition();
            setTextColor(mOriginalTextColor);
        }
    }
}
