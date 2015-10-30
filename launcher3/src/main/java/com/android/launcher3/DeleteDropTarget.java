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

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.android.launcher3.compat.UserHandleCompat;

/**
 * 在Launcher3当中已经没有DeleteZone了，而是通过DeleteDropTarget来进行代替了
 * TODO: 对于我们设计的Launcher3当中，所有的Workspace当中的Shortcut Icon删除掉，就是
 * TODO: 意味着我们要直接将这个程序卸载掉
 */
public class DeleteDropTarget extends ButtonDropTarget {

    private static final String TAG = "DeleteDropTarget";

    private static int DELETE_ANIMATION_DURATION = 285;
    private static int FLING_DELETE_ANIMATION_DURATION = 350;
    private static float FLING_TO_DELETE_FRICTION = 0.035f;
    private static int MODE_FLING_DELETE_TO_TRASH = 0;
    private static int MODE_FLING_DELETE_ALONG_VECTOR = 1;

    private final int mFlingDeleteMode = MODE_FLING_DELETE_ALONG_VECTOR;

    private ColorStateList mOriginalTextColor;
    private TransitionDrawable mUninstallDrawable;
    private TransitionDrawable mRemoveDrawable;
    private TransitionDrawable mCurrentDrawable;

    private boolean mWaitingForUninstall = false;

    public DeleteDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeleteDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        Log.d(TAG, " on finish inflate ... ");
        super.onFinishInflate();

        // Get the drawable
        mOriginalTextColor = getTextColors();

        // Get the hover color
        Resources r = getResources();
        mHoverColor = r.getColor(R.color.delete_target_hover_tint);
        mUninstallDrawable = (TransitionDrawable)
                r.getDrawable(R.drawable.uninstall_target_selector);

        mRemoveDrawable = (TransitionDrawable) r.getDrawable(R.drawable.remove_target_selector);

        mRemoveDrawable.setCrossFadeEnabled(true);
        mUninstallDrawable.setCrossFadeEnabled(true);

        // The current drawable is set to either the remove drawable or the uninstall drawable 
        // and is initially set to the remove drawable, as set in the layout xml.
        mCurrentDrawable = (TransitionDrawable) getCurrentDrawable();

        // Remove the text in the Phone UI in landscape
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!LauncherAppState.getInstance().isScreenLarge()) {
                setText("");
            }
        }
    }

    // 用于判断当前的程序是否是从AllApplication的界面即AppsCustomizePagedView当中得来的
    private boolean isAllAppsApplication(DragSource source, Object info) {
        return source.supportsAppInfoDropTarget() && (info instanceof AppInfo);
    }

    // 用于判断当前的AppWidget是否是从AppsCustomizePagedView当中得来的
    // 因为这个AppWidget还有可能来自于Workspace当中的screen
    private boolean isAllAppsWidget(DragSource source, Object info) {
        if (source instanceof AppsCustomizePagedView) {
            if (info instanceof PendingAddItemInfo) {
                PendingAddItemInfo addInfo = (PendingAddItemInfo) info;
                switch (addInfo.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                        return true;
                }
            }
        }
        return false;
    }


    // TODO: 在我们设计的Launcher当中，文件夹是不可以删除的，因为关于文件夹删除之后的操作是未定义的
    // TODO: Workspace上面包含直接就是Application了，所以当我们删除文件夹时，里面的程序我们不知道
    // TODO: 应该怎么处理，所以我们不支持文件夹的删除
    // 以下的四个方法代表的是我们在WorkSpace当中可以删除的ItemInfo的类型
    // 这四种类型可以是Folde, ShortcutInfo, LauncherAppWidgetInfo, FolderInfo
    private boolean isDragSourceWorkspaceOrFolder(DragObject d) {
        return (d.dragSource instanceof Workspace) || (d.dragSource instanceof Folder);
    }

    private boolean isWorkspaceOrFolderApplication(DragObject d) {
        boolean result = isDragSourceWorkspaceOrFolder(d) && (d.dragInfo instanceof ShortcutInfo);
        Log.d(TAG_ANIM, " -----------> isWorkspaceOrFolderApplication ? --> " + result);
        return result;
//        return isDragSourceWorkspaceOrFolder(d) && (d.dragInfo instanceof ShortcutInfo);
    }

    // 以下的方法仅仅用于判断我们当前所Drag的Item是不是一个ShortcutInfo,不包含判断她是不是一个FolderInfo
    // 我们之前所使用的实现当中在是将FolderInfo和Application来一起进行判断的
    private boolean isWorkspaceApplication(DragObject d) {
        return (d.dragSource instanceof Workspace) && (d.dragInfo instanceof ShortcutInfo);
    }

    private boolean isWorkspaceOrFolderWidget(DragObject d) {
        return isDragSourceWorkspaceOrFolder(d) && (d.dragInfo instanceof LauncherAppWidgetInfo);
    }

    private boolean isWorkspaceFolder(DragObject d) {
        return (d.dragSource instanceof Workspace) && (d.dragInfo instanceof FolderInfo);
    }

    private void setHoverColor() {
        if (mCurrentDrawable != null) {
            mCurrentDrawable.startTransition(mTransitionDuration);
        }
        setTextColor(mHoverColor);
    }

    private void resetHoverColor() {
        if (mCurrentDrawable != null) {
            mCurrentDrawable.resetTransition();
        }
        setTextColor(mOriginalTextColor);
    }

    @Override
    public boolean acceptDrop(DragObject d) {
        Log.d(TAG, " accept drop from : " + d.toString());
        return willAcceptDrop(d.dragInfo);
    }

    public static boolean willAcceptDrop(Object info) {
        if (info instanceof ItemInfo) {
            ItemInfo item = (ItemInfo) info;
            if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET ||
                    item.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                // 当我们的Shortcut是来自于我们的第三方程序创建的Shortcut时，
                // 这里的判断就是真的，
                // 因为这种Shortcut真的就只是一个普通的Shortcut，而不涉及到一个具体的程序
                // 即是一个真正意义上的Shortcut，所以我们不会再施加更复杂的判断
                Log.d(TAG, " --- SIMPLE THIRD-PARTY APPLICATION CREATED SHORTCUT -----");
                Log.d(TAG, " current item type are AppWidget and Shortcut ");
                return true;
            }

            if (!LauncherAppState.isDisableAllApps() &&
                    item.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER) {
                // 这种情况对应的就是Workspace当中的Folder删除时的情况
                // TODO: 我们应该告诉用户这个不能删除
                // TODO: 华为EUI的具体做法是将DeleteZone当中的垃圾箱改成了“文件夹不能删除”的字样
                // TODO: 当然，做法有很多种，看谁的设计更好更人性化了
                // 当前的要删除的是一个Folder
                Log.d(TAG, " current item type are Folder ");
                return true;
            }

            if (!LauncherAppState.isDisableAllApps() &&
                    item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION &&
                    item instanceof AppInfo) {
                AppInfo appInfo = (AppInfo) info;
                // 这种情况对应的就是AppsCustomizePagedView当中的程序的AppInfo

                // 如果当前的程序是系统程序或者Vendor预装的程序，DeleteZone是不可以接受这个程序的
                // 在这里我们区分普通程序和系统程序之间的区别的方法就是直接判断当前的AppInfo是否具有
                // DOWNLOAD_FLAG,如果有，那么我们就可以直接删除这个程序了
                // TODO: 我们现在需要将DOWNLOAD_FLAG也添加到ShortcutInfo当中
                // TODO: 当然如果我们无法添加这个标记的话，我们还需要从另一方面进行解决，那就是
                // TODO: 就是在程序安装初始的时候就添加上这个标记
                // TODO: 查看AppInfo当中关于如何利用PackageManager来初始化FLAG的具体过程进行理解
                Log.d(TAG, " the current item are Application, we could also accept it, but if the application" +
                        " are System, we just neglect it");
                return (appInfo.flags & AppInfo.DOWNLOADED_FLAG) != 0;
            }

            // 现在我们走的是这个流程，我们要注意了
            // 因为我们是通过打开一个锁(即LauncherAppState.isDisableAllApps()进入到
            // Workspace当中的，所以我们的控制流程是在这里的)
            if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION &&
                    item instanceof ShortcutInfo) {
                // 这种情况对应的就是Workspace当中的Shortcut删除时的情况

                Log.d(TAG, " the current item type are ITEM_TYPE_APPLICATION and ShortcutInfo ... ");
                if (LauncherAppState.isDisableAllApps()) {
                    ShortcutInfo shortcutInfo = (ShortcutInfo) info;
                    Log.d(TAG, " --------> we need to judge whether we need to perform the following operations ");
                    // 如果我们要删除的Shortcut或者App当中附加了DOWNLOADED_FLAG标记，那么就代表这个App是可以被卸载的，
                    // 否则这个App就是系统的App，即不可以被卸载的
                    Log.d(TAG, " the flag for this shortcut are : " + shortcutInfo.flags);
                    boolean result = (shortcutInfo.flags & AppInfo.DOWNLOADED_FLAG) != 0;

                    Log.d(TAG, " ----> AND THE RESULT WE CALCULATED OUT ARE : " + result);
                    return result;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        boolean isVisible = true;
        // 在这里我们就直接将所有的都设置成UninstallLabel
        // 下面这个被我们注释掉的是最原始的实现
//        boolean useUninstallLabel = !LauncherAppState.isDisableAllApps() &&
//                isAllAppsApplication(source, info);
        // 以下是新的实现，因为在我们新的实现当中，我们已经将AllAppsCustomizePagedView
        // 这个PagedView去掉了，仅仅保留了Workspace，所以isAllAppsApplication()方法的
        // 的返回值应该一直为true, 所以我们在以下的判断当中，就直接将这一重判断去掉了
        ItemInfo itemInfo = (ItemInfo) info;
        boolean useUninstallLabel = LauncherAppState.isDisableAllApps() &&
                // 以下是普通的由第三方程序创建的Shortcut，而不是对应于程序的那种Shortcut
                !(itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET ||
                    itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT);

        // 以下的这个 useDeleteLabel基本上都没啥意义了，当然我们目前还不确定是否会
        // 在将来的某个时刻需要这个Label
        boolean useDeleteLabel = !useUninstallLabel && source.supportsDeleteDropTarget();
        Log.d(TAG, " use delete label ? " + useDeleteLabel);

        Log.d(TAG, " DOES WE WILL ACCEPT THIS DROP ACTION ? ---> " + willAcceptDrop(info) + "" +
                " AND THIS IS A WIDGET ? ----> " + isAllAppsWidget(source, info));
        // If we are dragging an application from AppsCustomize, only show the control if we can
        // delete the app (it was downloaded), and rename the string to "uninstall" in such a case.
        // Hide the delete target if it is a widget from AppsCustomize.
        if (!willAcceptDrop(info) || isAllAppsWidget(source, info)) {
            Log.d(TAG, " -------------> we need to set the invisible tag here as false <----------");
            isVisible = false;
        }

        // TODO: 当我们将我们的ItemInfo Drag到InfoDropTarget当中时，
        // TODO: 我们是不应该走入到这一流程当中的，我们应该直接退出的，即
        // TODO: 直接启动相关的Activity就OK了，在这里瞎生啥事？？？
        if (useUninstallLabel) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.d(TAG, " ----> WE ARE ENTER THE MULTI-USER ENVIRONMENT ... ");
                // TODO: 考虑到多用户模式下程序的删除(即将Shortcut模式同多用户模式结合起来)
                // 当我们是多用户模式时，我们通常情况下还要考虑到多用户模式下程序的删除过程
                // 如果当前的程序被不止一个用户所安装和使用，那么我们在删除时就要考虑到
                // 仅仅是为当前用户删除还是为所有用户删除这个App
                UserManager userManager = (UserManager)
                        getContext().getSystemService(Context.USER_SERVICE);
                Bundle restrictions = userManager.getUserRestrictions();
                if (restrictions.getBoolean(UserManager.DISALLOW_APPS_CONTROL, false)
                        || restrictions.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS, false)) {
                    isVisible = false;
                }
            }
        }

        // 如果我们当前是从Workspace当中删除Shortcut，那么我们在这里就需要使用useUninstallLabel
        // 如果我们当前是从AppsCustomizePagedView当中删除AppIcon，那么这里就需要使用useDeleteLabel
        // 我们目前的需求是统一使用useUninstallLabel，而不使用删除的Label
        if (useUninstallLabel) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(mUninstallDrawable, null, null, null);
        } else if (useDeleteLabel) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(mRemoveDrawable, null, null, null);
        } else {
            isVisible = false;
        }
        mCurrentDrawable = (TransitionDrawable) getCurrentDrawable();

        mActive = isVisible;
        resetHoverColor();
        ((ViewGroup) getParent()).setVisibility(isVisible ? View.VISIBLE : View.GONE);

        if (isVisible && getText().length() > 0) {
            if (info instanceof FolderInfo) {
                Log.d("test_debug_info", " we are dragging folder info ... ");
                setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                setText(R.string.folder_remove_hint);

                // TODO: 在这设置了文字以及取消Drawable之后，同时我们还要采取具体的禁止删除的相关措施
                // TODO: 当然我们所要采取的具体的措施就是在在DeleteDropTarget上面不采取任何措施，
                // TODO: 即不进行任何的动作，不删除也补提醒
                // TODO: ????????????

            } else {
                setText(useUninstallLabel ? R.string.delete_target_uninstall_label
                        : R.string.delete_target_label);
            }
        }
    }

    @Override
    public void onDragEnd() {
        super.onDragEnd();
        Log.d(TAG, " on drag end ... ");
        mActive = false;
    }

    public void onDragEnter(DragObject d) {
        super.onDragEnter(d);
        Log.d(TAG, " on drag enter ... ");
        // 当我们需要删除的Target进入到DeleteZone当中时，展示Hover动画效果
        setHoverColor();
    }

    public void onDragExit(DragObject d) {
        super.onDragExit(d);
        Log.d(TAG, " on drag exit ... ");
        if (!d.dragComplete) {
            resetHoverColor();
        } else {
            // Restore the hover color if we are deleting
            d.dragView.setColor(mHoverColor);
        }
    }

    private static final String TAG_ANIM = "DeleteTrashAnim";
    // TODO: 这个方法本身很奇特，
    // TODO: 目前已经不符合我们自己的需求了，因为我们需要的是可以将这个DragObject
    // TODO: 并不是直接从Workspace当中直接移除掉，而是在用户确定要删除这个Item时，
    // TODO: 然后再进行确定的删除，但是目前确实直接删除了，这不是我们希望的，改正
    private void animateToTrashAndCompleteDrop(final DragObject d) {
        Log.d(TAG_ANIM, " animate to trash complete drop ... ");
        final DragLayer dragLayer = mLauncher.getDragLayer();
        final Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);

        int width = mCurrentDrawable == null ? 0 : mCurrentDrawable.getIntrinsicWidth();
        int height = mCurrentDrawable == null ? 0 : mCurrentDrawable.getIntrinsicHeight();

        final Rect to = getIconRect(
                d.dragView.getMeasuredWidth(), // view width
                d.dragView.getMeasuredHeight(), // view height
                width, // drawable width
                height // drawable height
        );

        final float scale = (float) to.width() / from.width();

        mSearchDropTargetBar.deferOnDragEnd();
        deferCompleteDropIfUninstalling(d);

        // 当动画执行完之后，就执行下面的Runnable当中定义的action
        Runnable onAnimationEndRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG_ANIM, " the trash animation are end, and we are enter the complete drop function ... ");
                // 调用completeDrop()方法，完成相关的判断逻辑
                completeDrop(d);
                // 调用关于SearchDropTargetBar的回调
                mSearchDropTargetBar.onDragEnd();

                mLauncher.exitSpringLoadedDragModeDelayed(true, 0, null);
            }
        };

        dragLayer.animateView(
                d.dragView, // DragView
                from, // from rect
                to, // to rect
                scale,
                1f,
                1f,
                0.1f,
                0.1f,
                DELETE_ANIMATION_DURATION, // 285
                new DecelerateInterpolator(2),
                new LinearInterpolator(),
                onAnimationEndRunnable, // onCompleteRunnable, 即动画执行完之后执行的动作
                DragLayer.ANIMATION_END_DISAPPEAR,
                null
        );
    }

    private void deferCompleteDropIfUninstalling(DragObject d) {
        Log.d(TAG_ANIM, " defer complete drop if uninstalling ... ");
        mWaitingForUninstall = false;
        if (isUninstallFromWorkspace(d)) {
            if (d.dragSource instanceof Folder) {
                ((Folder) d.dragSource).deferCompleteDropAfterUninstallActivity();
            } else if (d.dragSource instanceof Workspace) {
                Log.d(TAG_ANIM, " defer complete drop after uninstalling activity ... ");
                ((Workspace) d.dragSource).deferCompleteDropAfterUninstallActivity();
            }
            mWaitingForUninstall = true;
        }
    }

    private boolean isUninstallFromWorkspace(DragObject d) {
        Log.d(TAG_ANIM, " we are uninstalling from workSpace ... " + d.toString());
        if (LauncherAppState.isDisableAllApps() && isWorkspaceOrFolderApplication(d)) {
            ShortcutInfo shortcut = (ShortcutInfo) d.dragInfo;
            Log.d(TAG_ANIM, "-------------------------------------------------------------------");
            Log.d(TAG_ANIM, " truly judging whether we are uninstalling from the workspace ---> ");
            // 如果我们的返回值是true的话，我们就进入到了另一个逻辑，而那个逻辑正是我们所需要的
            boolean result = InstallShortcutReceiver.isValidShortcutLaunchIntent(shortcut.intent);
            Log.d(TAG_ANIM, " and the result are : " + result);
            return result;

            // 下面的旧有的实现跟我们需要的逻辑刚好相反，所以我们直接注释掉了，上面的逻辑才是我们需要的
            // Only allow manifest shortcuts to initiate an un-install.
//            return !InstallShortcutReceiver.isValidShortcutLaunchIntent(shortcut.intent);
        }
        return false;
    }

    /**
     * 当结束Drag之后进行的回调
     *
     * @param d
     */
    private void completeDrop(DragObject d) {
        Log.d(TAG_ANIM, " complete drop, and the DragObject are : " + d.toString());
        ItemInfo item = (ItemInfo) d.dragInfo;
        boolean wasWaitingForUninstall = mWaitingForUninstall;
        mWaitingForUninstall = false;
        // 不过目前，我们是不需要下面这个操作步骤的
        // 如果我们是从AppsCustomizePagedView当中进行删除的话，我们就直接启动uninstall activity来进行处理
        // 否则还是从直接从其他方面进行处理
        if (isAllAppsApplication(d.dragSource, item)) {
            // 下面的逻辑基本上我们是用不到的
            // Uninstall the application if it is being dragged from AppsCustomize
            AppInfo appInfo = (AppInfo) item;
            mLauncher.startApplicationUninstallActivity(appInfo.componentName, appInfo.flags,
                        appInfo.user);

        } else if (isUninstallFromWorkspace(d)) {
            Log.d(TAG_ANIM, " we are uninstall from workspace ... ");

            ShortcutInfo shortcut = (ShortcutInfo) item;
            if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET ||
                    item.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                Log.d(TAG_ANIM, " FUCKING!!!!!!!!!!!!!!!!!");
                LauncherModel.deleteItemFromDatabase(mLauncher, item);

            }
            if (shortcut.intent != null && shortcut.intent.getComponent() != null) {
                Log.d(TAG_ANIM, " NORMAL APP UNINSTALL PROCESS ... ");
                final ComponentName componentName = shortcut.intent.getComponent();
                final DragSource dragSource = d.dragSource;
                final UserHandleCompat user = shortcut.user;
                mWaitingForUninstall = mLauncher.startApplicationUninstallActivity(
                        componentName, shortcut.flags, user);
                if (mWaitingForUninstall) {
                    final Runnable checkIfUninstallWasSuccess = new Runnable() {
                        @Override
                        public void run() {
                            mWaitingForUninstall = false;
                            String packageName = componentName.getPackageName();
                            boolean uninstallSuccessful = !AllAppsList.packageHasActivities(
                                    getContext(), packageName, user);
                            if (dragSource instanceof Folder) {
                                ((Folder) dragSource).
                                        onUninstallActivityReturned(uninstallSuccessful);
                            } else if (dragSource instanceof Workspace) {
                                // 当我们从“确定用户是否卸载”页面返回之后(无论用户点击的是“确定”还是“取消”)，
                                // 下面的这个流程才会被执行
                                // 这个回调是在特定的流程之后执行的
                                Log.d(TAG_ANIM, " ------> the drag source are workspace ... ");
                                ((Workspace) dragSource).
                                        onUninstallActivityReturned(uninstallSuccessful);
                            }
                        }
                    };
                    mLauncher.addOnResumeCallback(checkIfUninstallWasSuccess);
                }
            }
        } else if (isWorkspaceOrFolderApplication(d)) {
            // 下面的这段代码逻辑我们永远也不会再调用了，因为不是我们需要的
            Log.d(TAG_ANIM, "如果我看到了这个Log信息的话，那就只能说明你日了狗了~！");
            // 此时的DragObject的具体类型是ShortcutInfo
            Log.d(TAG, " ---------------WORKSPACE FOLDER APPLICATION ------------------");

            // 当我们在Workspace当中删除Shortcut时，我们直接将这个程序卸载，如果用户不同意卸载，
            // 那么就不会删除这个Shortcut
//            ShortcutInfo appInfo = (ShortcutInfo) item;
//            if (!(item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET ||
//                    item.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT)) {
//                // 如果当前我们要操作的Shortcut并不是由第三方程序创建的Shortcut时，我们再执行下面的操作
//                // 因为由第三方程序创建的Shortcut本身并没有设置Flag位，所以还是原始的默认值，即0
//                // 所以会被launcher误认为是一个System App
//                mLauncher.startApplicationUninstallActivity(appInfo.getTargetComponent(), appInfo.flags,
//                        appInfo.user);
//            }

            // TODO: 但是目前还有一个问题，就是如果用户并没有真正的卸载这个程序，而仅仅只是尝试了一下
            // TODO: 然后又取消了之后的具体的删除过程，那么图标还是会被删除的，所以我们要在这里进行进一步的整理
            // TODO: 过程，即在用户真正的完成了相关程序的卸载之后再进行shortcut的清除过程
            // TODO: 所以我们将下面的这个过程移到监听程序卸载的地方，即当我们确定的收到了程序卸载的消息之后，我们
            // TODO: 再将这个Item从Workspace当中移除掉
            // 以下的这个LauncherModel.deleteItemFromDatabase()这个方法并不是真正的完成将一个Shortcut从Workspace
            // 当中移除出去的过程
            // TODO: 仔细调试一下下面的这个过程
            // TODO: 以下的这个过程到底是否是真正需要的一个过程？？？
            LauncherModel.deleteItemFromDatabase(mLauncher, item);

        } else if (isWorkspaceFolder(d)) {
            Log.d(TAG, " ---------------WORKSPACE FOLDER ------------------");
            // TODO: 在Launcher3当中，我们是不能删除Workspace当中的Folder的
            // Remove the folder from the workspace and delete the contents from launcher model
            FolderInfo folderInfo = (FolderInfo) item;
            mLauncher.removeFolder(folderInfo);
            LauncherModel.deleteFolderContentsFromDatabase(mLauncher, folderInfo);
        } else if (isWorkspaceOrFolderWidget(d)) {
            Log.d(TAG, " ---------------WORKSPACE FOLDER WIDGET ... ------------------");
            // Remove the widget from the workspace
            mLauncher.removeAppWidget((LauncherAppWidgetInfo) item);
            LauncherModel.deleteItemFromDatabase(mLauncher, item);

            final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) item;
            final LauncherAppWidgetHost appWidgetHost = mLauncher.getAppWidgetHost();
            if ((appWidgetHost != null) && launcherAppWidgetInfo.isWidgetIdValid()) {
                // Deleting an app widget ID is a void call but writes to disk before returning
                // to the caller...
                new AsyncTask<Void, Void, Void>() {
                    public Void doInBackground(Void... args) {
                        appWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
            }
        }

        if (wasWaitingForUninstall && !mWaitingForUninstall) {
            if (d.dragSource instanceof Folder) {
                ((Folder) d.dragSource).onUninstallActivityReturned(false);
            } else if (d.dragSource instanceof Workspace) {
                ((Workspace) d.dragSource).onUninstallActivityReturned(false);
            }
        }
    }

    public void onDrop(DragObject d) {
        Log.d(TAG, " on drop the dragObject, and the object we drop are : " + d.toString());
        animateToTrashAndCompleteDrop(d);
    }

    /**
     * Creates an animation from the current drag view to the delete trash icon.
     */
    private AnimatorUpdateListener createFlingToTrashAnimatorListener(final DragLayer dragLayer,
                                                                      DragObject d, PointF vel, ViewConfiguration config) {

        int width = mCurrentDrawable == null ? 0 : mCurrentDrawable.getIntrinsicWidth();
        int height = mCurrentDrawable == null ? 0 : mCurrentDrawable.getIntrinsicHeight();
        final Rect to = getIconRect(d.dragView.getMeasuredWidth(), d.dragView.getMeasuredHeight(),
                width, height);
        final Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);

        // Calculate how far along the velocity vector we should put the intermediate point on
        // the bezier curve
        float velocity = Math.abs(vel.length());
        float vp = Math.min(1f, velocity / (config.getScaledMaximumFlingVelocity() / 2f));
        int offsetY = (int) (-from.top * vp);
        int offsetX = (int) (offsetY / (vel.y / vel.x));
        final float y2 = from.top + offsetY;                        // intermediate t/l
        final float x2 = from.left + offsetX;
        final float x1 = from.left;                                 // drag view t/l
        final float y1 = from.top;
        final float x3 = to.left;                                   // delete target t/l
        final float y3 = to.top;

        final TimeInterpolator scaleAlphaInterpolator = new TimeInterpolator() {
            @Override
            public float getInterpolation(float t) {
                return t * t * t * t * t * t * t * t;
            }
        };
        return new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final DragView dragView = (DragView) dragLayer.getAnimatedView();
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                float tp = scaleAlphaInterpolator.getInterpolation(t);
                float initialScale = dragView.getInitialScale();
                float finalAlpha = 0.5f;
                float scale = dragView.getScaleX();
                float x1o = ((1f - scale) * dragView.getMeasuredWidth()) / 2f;
                float y1o = ((1f - scale) * dragView.getMeasuredHeight()) / 2f;
                float x = (1f - t) * (1f - t) * (x1 - x1o) + 2 * (1f - t) * t * (x2 - x1o) +
                        (t * t) * x3;
                float y = (1f - t) * (1f - t) * (y1 - y1o) + 2 * (1f - t) * t * (y2 - x1o) +
                        (t * t) * y3;

                dragView.setTranslationX(x);
                dragView.setTranslationY(y);
                dragView.setScaleX(initialScale * (1f - tp));
                dragView.setScaleY(initialScale * (1f - tp));
                dragView.setAlpha(finalAlpha + (1f - finalAlpha) * (1f - tp));
            }
        };
    }

    /**
     * Creates an animation from the current drag view along its current velocity vector.
     * For this animation, the alpha runs for a fixed duration and we update the position
     * progressively.
     */
    private static class FlingAlongVectorAnimatorUpdateListener implements AnimatorUpdateListener {
        private DragLayer mDragLayer;
        private PointF mVelocity;
        private Rect mFrom;
        private long mPrevTime;
        private boolean mHasOffsetForScale;
        private float mFriction;

        private final TimeInterpolator mAlphaInterpolator = new DecelerateInterpolator(0.75f);

        public FlingAlongVectorAnimatorUpdateListener(DragLayer dragLayer, PointF vel, Rect from,
                                                      long startTime, float friction) {
            mDragLayer = dragLayer;
            mVelocity = vel;
            mFrom = from;
            mPrevTime = startTime;
            mFriction = 1f - (dragLayer.getResources().getDisplayMetrics().density * friction);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            final DragView dragView = (DragView) mDragLayer.getAnimatedView();
            float t = ((Float) animation.getAnimatedValue()).floatValue();
            long curTime = AnimationUtils.currentAnimationTimeMillis();

            if (!mHasOffsetForScale) {
                mHasOffsetForScale = true;
                float scale = dragView.getScaleX();
                float xOffset = ((scale - 1f) * dragView.getMeasuredWidth()) / 2f;
                float yOffset = ((scale - 1f) * dragView.getMeasuredHeight()) / 2f;

                mFrom.left += xOffset;
                mFrom.top += yOffset;
            }

            mFrom.left += (mVelocity.x * (curTime - mPrevTime) / 1000f);
            mFrom.top += (mVelocity.y * (curTime - mPrevTime) / 1000f);

            dragView.setTranslationX(mFrom.left);
            dragView.setTranslationY(mFrom.top);
            dragView.setAlpha(1f - mAlphaInterpolator.getInterpolation(t));

            mVelocity.x *= mFriction;
            mVelocity.y *= mFriction;
            mPrevTime = curTime;
        }
    }

    private AnimatorUpdateListener createFlingAlongVectorAnimatorListener(final DragLayer dragLayer,
                                                                          DragObject d, PointF vel, final long startTime, final int duration,
                                                                          ViewConfiguration config) {
        final Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);

        return new FlingAlongVectorAnimatorUpdateListener(dragLayer, vel, from, startTime,
                FLING_TO_DELETE_FRICTION);
    }

    private static final String TAG_FLING = "DeleteDrop_OnFling";
    // 这就是普通的Delete了
    public void onFlingToDelete(final DragObject d, int x, int y, PointF vel) {
        Log.d(TAG_FLING, " onFling to delete ... ");
        final boolean isAllApps = d.dragSource instanceof AppsCustomizePagedView;

        // Don't highlight the icon as it's animating
        d.dragView.setColor(0);
        d.dragView.updateInitialScaleToCurrentScale();
        // Don't highlight the target if we are flinging from AllApps
        if (isAllApps) {
            resetHoverColor();
        }

        if (mFlingDeleteMode == MODE_FLING_DELETE_TO_TRASH) {
            // Defer animating out the drop target if we are animating to it
            mSearchDropTargetBar.deferOnDragEnd();
            mSearchDropTargetBar.finishAnimations();
        }

        final ViewConfiguration config = ViewConfiguration.get(mLauncher);
        final DragLayer dragLayer = mLauncher.getDragLayer();
        final int duration = FLING_DELETE_ANIMATION_DURATION;
        final long startTime = AnimationUtils.currentAnimationTimeMillis();

        // NOTE: Because it takes time for the first frame of animation to actually be
        // called and we expect the animation to be a continuation of the fling, we have
        // to account for the time that has elapsed since the fling finished.  And since
        // we don't have a startDelay, we will always get call to update when we call
        // start() (which we want to ignore).
        final TimeInterpolator tInterpolator = new TimeInterpolator() {
            private int mCount = -1;
            private float mOffset = 0f;

            @Override
            public float getInterpolation(float t) {
                if (mCount < 0) {
                    mCount++;
                } else if (mCount == 0) {
                    mOffset = Math.min(0.5f, (float) (AnimationUtils.currentAnimationTimeMillis() -
                            startTime) / duration);
                    mCount++;
                }
                return Math.min(1f, mOffset + t);
            }
        };
        AnimatorUpdateListener updateCb = null;
        if (mFlingDeleteMode == MODE_FLING_DELETE_TO_TRASH) {
            updateCb = createFlingToTrashAnimatorListener(dragLayer, d, vel, config);
        } else if (mFlingDeleteMode == MODE_FLING_DELETE_ALONG_VECTOR) {
            updateCb = createFlingAlongVectorAnimatorListener(dragLayer, d, vel, startTime,
                    duration, config);
        }
        deferCompleteDropIfUninstalling(d);

        Runnable onAnimationEndRunnable = new Runnable() {
            @Override
            public void run() {
                // If we are dragging from AllApps, then we allow AppsCustomizePagedView to clean up
                // itself, otherwise, complete the drop to initiate the deletion process
                if (!isAllApps) {
                    mLauncher.exitSpringLoadedDragMode();
                    completeDrop(d);
                }
                mLauncher.getDragController().onDeferredEndFling(d);
            }
        };
        dragLayer.animateView(d.dragView, updateCb, duration, tInterpolator, onAnimationEndRunnable,
                DragLayer.ANIMATION_END_DISAPPEAR, null);
    }
}
