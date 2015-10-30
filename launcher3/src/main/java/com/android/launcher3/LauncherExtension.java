package com.android.launcher3;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.security.AlgorithmParameterGenerator;
import java.util.ArrayList;

/**
 * This class represents a very trivial LauncherExtension. It primarily serves as a simple
 * class to exercise the LauncherOverlay interface.
 */
public class LauncherExtension extends Launcher {

    private static final String TAG = "LauncherExtension";

    //------ Activity methods -------//
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setLauncherCallbacks(new LauncherExtensionCallbacks());
        super.onCreate(savedInstanceState);
    }

    /**
     * 以下定义的大部分回调方法在LauncherExtension当中都是没有提供具体的实现
     * 当时这些实现都被添加到Launcher当中了，即Launcher当中已经进行了hook，
     * 只是我们这里没有提供具体的实现
     * TODO: 我们在之后的实现当中可以提供一些实现，而不是简单的添加一些Log
     * TODO: 我们可以参考Android的源代码，可以看到这里的具体调用应该都是
     * TODO: Framework当中被@hide的API
     */
    // TODO: add implementation for all of the following callbacks implementations
    public class LauncherExtensionCallbacks implements LauncherCallbacks {

        LauncherExtensionOverlay mLauncherOverlay = new LauncherExtensionOverlay();

        @Override
        public void preOnCreate() {
            Log.d(TAG, " on Launcher preOnCreate()");
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.d(TAG, " on Launcher onCreate() method ");
        }

        @Override
        public void preOnResume() {
            Log.d(TAG, " on Launcher preOnResume() method get called");
        }

        @Override
        public void onResume() {
            Log.d(TAG, " on Launcher onResume() method get called ");
        }

        @Override
        public void onStart() {
            Log.d(TAG, " on Launcher onStart() method get called ");
        }

        @Override
        public void onStop() {
            Log.d(TAG, " on Launcher onStop() method get called ");
        }

        @Override
        public void onPause() {
            Log.d(TAG, " on Launcher onPause() method get called ");
        }

        @Override
        public void onDestroy() {
            Log.d(TAG, " on Launcher onDestroy() method get called ");
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            Log.d(TAG, " on Launcher onSaveInstanceState() method get called ");
        }

        @Override
        public void onPostCreate(Bundle savedInstanceState) {
            Log.d(TAG, " on Launcher onPostCreate() method get called ");
        }

        @Override
        public void onNewIntent(Intent intent) {
            Log.d(TAG, " on Launcher onNewIntent() method get called ");
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            Log.d(TAG, " on Launcher onActivityResult() method get called ");
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            Log.d(TAG, " on Launcher onWindowFocusChanged() method get called ");
        }

        @Override
        public boolean onPrepareOptionsMenu(Menu menu) {
            Log.d(TAG, " on Launcher onPrepareOptionsMenu() method get called ");
            return false;
        }

        @Override
        public void dump(String prefix, FileDescriptor fd, PrintWriter w, String[] args) {
        }

        @Override
        public void onHomeIntent() {
            Log.d(TAG, " on Launcher onHomeIntent() method get called ");
        }

        @Override
        public boolean handleBackPressed() {
            if (mLauncherOverlay.isOverlayPanelShowing()) {
                mLauncherOverlay.hideOverlayPanel();
                Log.d(TAG, " closing the LauncherOverlayPanel ... ");
                return true;
            }
            Log.d(TAG, " the LauncherOverlayPanel is not showing ... ");
            return false;
        }

        @Override
        public void onLauncherProviderChange() {
            // TODO: 关于LauncherProvider发生改变时，我们需要做一些改变
            // TODO: 但是这里有一个问题就是LauncherProvider在什么时候会发生改变
            // TODO: 因为考虑到LauncherProvider本身是直接从PackageManager当中
            // TODO: 提取数据的，
            // TODO: 如果我们提供多重虚拟桌面的话，我们倒是可以在这里通过添加具体的实现
            // TODO: 来通知Launcher的相关改变
            Log.d(TAG, " the Launcher provider has been changed ... ");
        }

        @Override
        public void finishBindingItems(boolean upgradePath) {
            Log.d(TAG, " finish binding the item , and the upgradePath are : " + upgradePath);
        }

        @Override
        public void onClickAllAppsButton(View v) {
            Log.d(TAG, " the AllAppsButton has been clicked on ... ");
        }

        @Override
        public void bindAllApplications(ArrayList<AppInfo> apps) {
            Log.d(TAG, " bind all applications from the LauncherCallback ");
        }

        @Override
        public void onClickFolderIcon(View v) {
            Log.d(TAG, " the folder icon has been clicked on, and the tag for that folderView are : " + v.getTag());
        }

        @Override
        public void onClickAppShortcut(View v) {
            Log.d(TAG, " the shortCut : " + v.getTag() + " has been clicked on ");
        }

        @Override
        public void onClickPagedViewIcon(View v) {
            Log.d(TAG, " the paged View icon has been clicked on ... ");
        }

        @Override
        public void onClickWallpaperPicker(View v) {
            Log.d(TAG, " on clicked on wallPaper picker ... ");
        }

        @Override
        public void onClickSettingsButton(View v) {
            Log.d(TAG, " on clicked on the settings button ");
        }

        @Override
        public void onClickAddWidgetButton(View v) {
            Log.d(TAG, " on click the adding widget to the workspace button ");
        }

        @Override
        public void onPageSwitch(View newPage, int newPageIndex) {
            Log.d(TAG, " on page switching happened ... ");
        }

        @Override
        public void onWorkspaceLockedChanged() {
            Log.d(TAG, " on workspace locked changed ... ");
        }

        @Override
        public void onDragStarted(View view) {
            Log.d(TAG, " the Drag has been started ... ");
        }

        @Override
        public void onInteractionBegin() {
            Log.d(TAG, " the interaction has been started ... ");
        }

        @Override
        public void onInteractionEnd() {
            Log.d(TAG, " the interaction has been end ... ");
        }

        @Override
        public boolean forceDisableVoiceButtonProxy() {
            return false;
        }

        @Override
        public boolean providesSearch() {
            Log.d(TAG, " getting information about whether the current launcher supports the search ability ");
            return true;
        }

        @Override
        public boolean startSearch(String initialQuery, boolean selectInitialQuery,
                Bundle appSearchData, Rect sourceBounds) {
            return false;
        }

        @Override
        public void startVoice() {
            Log.d(TAG, " start the voice search process ... ");
        }

        @Override
        public boolean hasCustomContentToLeft() {
            // TODO: add some custom content to the Launcher left part as the custom
            // TODO: that we just need
            // TODO: for the detailed effect, we could refer to the SAMSUNG S-IV custom
            // TODO: android ROM implementation
            // TODO: the detailed implementation are left at the Workspace.addToCustomContentPage()
            // TODO: and we just add all of the what we need in this method, we could implement
            // TODO: what we need
            return true;
        }

        @Override
        public void populateCustomContentContainer() {
            Log.d(TAG, " populateCustomContentContainer ");
        }

        @Override
        public View getQsbBar() {
            return mLauncherOverlay.getSearchBox();
        }

        @Override
        public Intent getFirstRunActivity() {
            return null;
        }

        @Override
        public boolean hasFirstRunActivity() {
            return false;
        }

        @Override
        public boolean hasDismissableIntroScreen() {
            return false;
        }

        @Override
        public View getIntroScreen() {
            return null;
        }

        @Override
        public boolean shouldMoveToDefaultScreenOnHomeIntent() {
            return true;
        }

        @Override
        public boolean hasSettings() {
            return true;
        }

        @Override
        public ComponentName getWallpaperPickerComponent() {
            return null;
        }

        @Override
        public boolean overrideWallpaperDimensions() {
            return false;
        }

        @Override
        public boolean isLauncherPreinstalled() {
            Log.d(TAG, " isLauncherPreinstalled ");
            return false;
        }

        @Override
        public boolean hasLauncherOverlay() {
            Log.d(TAG, "hasLauncherOverlay ");
            return true;
        }

        @Override
        public LauncherOverlay setLauncherOverlayView(InsettableFrameLayout container,
                LauncherOverlayCallbacks callbacks) {
            Log.d(TAG, " set the LauncherOverlayView from here ... ");
            mLauncherOverlay.setOverlayCallbacks(callbacks);
            mLauncherOverlay.setOverlayContainer(container);

            return mLauncherOverlay;
        }

        class LauncherExtensionOverlay implements LauncherOverlay {
            LauncherOverlayCallbacks mLauncherOverlayCallbacks;
            ViewGroup mOverlayView;
            View mSearchBox;
            View mSearchOverlay;
            boolean mShowOverlayFeedback;
            int mProgress;
            boolean mOverlayPanelShowing;

            @Override
            public void onScrollInteractionBegin() {
                if (mLauncherOverlayCallbacks.canEnterFullImmersion()) {
                    mShowOverlayFeedback = true;
                    updatePanelOffset(0);
                    mSearchOverlay.setVisibility(View.VISIBLE);
                    mSearchOverlay.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                }
            }

            @Override
            public void onScrollChange(int progress, boolean rtl) {
                mProgress = progress;
                if (mShowOverlayFeedback) {
                    updatePanelOffset(progress);
                }
            }

            private void updatePanelOffset(int progress) {
                int panelWidth = mSearchOverlay.getMeasuredWidth();
                int offset = (int) ((progress / 100f) * panelWidth);
                mSearchOverlay.setTranslationX(- panelWidth + offset);
            }

            @Override
            public void onScrollInteractionEnd() {
                if (mProgress > 25 && mLauncherOverlayCallbacks.enterFullImmersion()) {
                    ObjectAnimator oa = LauncherAnimUtils.ofFloat(mSearchOverlay, "translationX", 0);
                    oa.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator arg0) {
                            mSearchOverlay.setLayerType(View.LAYER_TYPE_NONE, null);
                        }
                    });
                    oa.start();
                    mOverlayPanelShowing = true;
                    mShowOverlayFeedback = false;
                }
            }

            @Override
            public void onScrollSettled() {
                if (mShowOverlayFeedback) {
                    mSearchOverlay.setVisibility(View.INVISIBLE);
                    mSearchOverlay.setLayerType(View.LAYER_TYPE_NONE, null);
                }
                mShowOverlayFeedback = false;
                mProgress = 0;
            }

            public void hideOverlayPanel() {
                Log.d(TAG, " hide overlay panel ...");
                mLauncherOverlayCallbacks.exitFullImmersion();
                mSearchOverlay.setVisibility(View.INVISIBLE);
                mOverlayPanelShowing = false;
            }

            public boolean isOverlayPanelShowing() {
                Log.d(TAG, " the overlay panel is showing ? " + mOverlayPanelShowing);
                return mOverlayPanelShowing;
            }

            @Override
            public void forceExitFullImmersion() {
                Log.d(TAG, " forceExitFullImmersion ... ");
                hideOverlayPanel();
            }

            public void setOverlayContainer(InsettableFrameLayout container) {
                Log.d(TAG, " set the overlay container ");
                mOverlayView = (ViewGroup) getLayoutInflater().inflate(
                        R.layout.launcher_overlay_example, container);
                mSearchOverlay = mOverlayView.findViewById(R.id.search_overlay);
                mSearchBox = mOverlayView.findViewById(R.id.search_box);
            }

            public View getSearchBox() {
                return mSearchBox;
            }

            public void setOverlayCallbacks(LauncherOverlayCallbacks callbacks) {
                mLauncherOverlayCallbacks = callbacks;
            }
        };
    }
}
