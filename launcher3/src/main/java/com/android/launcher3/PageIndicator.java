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

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import java.awt.font.TextAttribute;
import java.util.ArrayList;

public class PageIndicator extends LinearLayout {
    @SuppressWarnings("unused")
    private static final String TAG = "Launcher3_PageIndicator";

    // Want this to look good? Keep it odd
    private static final boolean MODULATE_ALPHA_ENABLED = false;

    private LayoutInflater mLayoutInflater;

    // 这个数组用于控制当前的PageIndicator的状态
    private int[] mWindowRange = new int[2];
    private int mMaxWindowSize;

    private ArrayList<PageIndicatorMarker> mMarkers =
            new ArrayList<>();
    private int mActiveMarkerIndex;

    public static class PageMarkerResources {
        int activeId;
        int inactiveId;

        public PageMarkerResources() {
            activeId = R.drawable.ic_pageindicator_current;
            inactiveId = R.drawable.ic_pageindicator_default;
        }
        public PageMarkerResources(int aId, int iaId) {
            activeId = aId;
            inactiveId = iaId;
        }

        @Override
        public String toString() {
            return " the current active marker are : " + activeId + ", and the inactiveId are : " + inactiveId;
        }
    }

    public PageIndicator(Context context) {
        this(context, null);
    }

    public PageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.PageIndicator, defStyle, 0);
        // 我们的WorkSpace默认只有15个page，不过已经很多了，如果再多的话，估计就可使卡了
        // 关键我们的screen的width有限，也不一定可以同时放的下15个point，挤爆了
        mMaxWindowSize = a.getInteger(R.styleable.PageIndicator_windowSize, 15);
        mWindowRange[0] = 0;
        mWindowRange[1] = 0;
        mLayoutInflater = LayoutInflater.from(context);
        a.recycle();

        // Set the layout transition properties
        LayoutTransition transition = getLayoutTransition();
        transition.setDuration(175);
    }

    private void enableLayoutTransitions() {
        LayoutTransition transition = getLayoutTransition();
        transition.enableTransitionType(LayoutTransition.APPEARING);
        transition.enableTransitionType(LayoutTransition.DISAPPEARING);
        transition.enableTransitionType(LayoutTransition.CHANGE_APPEARING);
        transition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
    }

    private void disableLayoutTransitions() {
        LayoutTransition transition = getLayoutTransition();
        transition.disableTransitionType(LayoutTransition.APPEARING);
        transition.disableTransitionType(LayoutTransition.DISAPPEARING);
        transition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        transition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
    }

    void offsetWindowCenterTo(int activeIndex, boolean allowAnimations) {
        Log.d(TAG, " inside the PageIndicator implementation, and we need to control the offsetWindowCenterTo() method");
        if (activeIndex < 0) {
            new Throwable().printStackTrace();
        }

        int windowSize = Math.min(mMarkers.size(), mMaxWindowSize);
        int hWindowSize = (int) windowSize / 2;
        float hfWindowSize = windowSize / 2f;
        int windowStart = Math.max(0, activeIndex - hWindowSize);
        int windowEnd = Math.min(mMarkers.size(), windowStart + mMaxWindowSize);
        windowStart = windowEnd - Math.min(mMarkers.size(), windowSize);
        int windowMid = windowStart + (windowEnd - windowStart) / 2;
        boolean windowAtStart = (windowStart == 0);
        boolean windowAtEnd = (windowEnd == mMarkers.size());
        boolean windowMoved = (mWindowRange[0] != windowStart) ||
                (mWindowRange[1] != windowEnd);

        if (!allowAnimations) {
            disableLayoutTransitions();
        }

        // Remove all the previous children that are no longer in the window
        for (int i = getChildCount() - 1; i >= 0; --i) {
            PageIndicatorMarker marker = (PageIndicatorMarker) getChildAt(i);
            int markerIndex = mMarkers.indexOf(marker);
            if (markerIndex < windowStart || markerIndex >= windowEnd) {
                Log.d(TAG, " --> remove page marker view  at : " + markerIndex);
                removeView(marker);
            }
        }

        // Add all the new children that belong in the window
        final int MARKER_SIZE = mMarkers.size();
        for (int i = 0; i < MARKER_SIZE; ++i) {
            PageIndicatorMarker marker = (PageIndicatorMarker) mMarkers.get(i);
            if (windowStart <= i && i < windowEnd) {
                if (indexOfChild(marker) < 0) {
                    Log.d(TAG, " --> add page marker at : " + i);
                    addView(marker, i - windowStart);
                }
                // and set the relative marker state
                if (i == activeIndex) {
                    Log.d(TAG, " activate : " + i);
                    marker.activate(windowMoved);
                } else {
                    Log.d(TAG, " inactivate : " + i);
                    marker.inactivate(windowMoved);
                }
            } else {
                marker.inactivate(true);
            }

            if (MODULATE_ALPHA_ENABLED) {
                // 如果允许对PageMarker施行动画效果
                // Update the marker's alpha
                float alpha = 1f;
                if (mMarkers.size() > windowSize) {
                    if ((windowAtStart && i > hWindowSize) ||
                        (windowAtEnd && i < (mMarkers.size() - hWindowSize)) ||
                        (!windowAtStart && !windowAtEnd)) {
                        alpha = 1f - Math.abs((i - windowMid) / hfWindowSize);
                    }
                }
                marker.animate().alpha(alpha).setDuration(500).start();
            }
        }

        if (!allowAnimations) {
            enableLayoutTransitions();
        }

        mWindowRange[0] = windowStart;
        mWindowRange[1] = windowEnd;
    }

    void addMarker(int index, PageMarkerResources marker, boolean allowAnimations) {
        index = Math.max(0, Math.min(index, mMarkers.size()));

        PageIndicatorMarker m =
            (PageIndicatorMarker) mLayoutInflater.inflate(R.layout.page_indicator_marker,
                    this, false);
        m.setMarkerDrawables(marker.activeId, marker.inactiveId);

        mMarkers.add(index, m);
        offsetWindowCenterTo(mActiveMarkerIndex, allowAnimations);
    }
    void addMarkers(ArrayList<PageMarkerResources> markers, boolean allowAnimations) {
        for (PageMarkerResources markerResources : markers) {
            Log.d(TAG, " add markers " + markerResources.toString());
            addMarker(Integer.MAX_VALUE, markerResources, allowAnimations);
        }
    }

    void updateMarker(int index, PageMarkerResources marker) {
        Log.d(TAG, " update tha marker state at : " + index);
        PageIndicatorMarker m = mMarkers.get(index);
        m.setMarkerDrawables(marker.activeId, marker.inactiveId);
    }

    void removeMarker(int index, boolean allowAnimations) {
        if (mMarkers.size() > 0) {
            Log.d(TAG, " remove the marker at : " + index);
            index = Math.max(0, Math.min(mMarkers.size() - 1, index));
            mMarkers.remove(index);
            offsetWindowCenterTo(mActiveMarkerIndex, allowAnimations);
        }
    }
    void removeAllMarkers(boolean allowAnimations) {
        while (mMarkers.size() > 0) {
            Log.d(TAG, " remove all the markers ");
            removeMarker(Integer.MAX_VALUE, allowAnimations);
        }
    }

    void setActiveMarker(int index) {
        // Center the active marker
        mActiveMarkerIndex = index;
        offsetWindowCenterTo(index, false);
    }

    void dumpState(String txt) {
        System.out.println(txt);
        System.out.println("\tmMarkers: " + mMarkers.size());
        for (int i = 0; i < mMarkers.size(); ++i) {
            PageIndicatorMarker m = mMarkers.get(i);
            System.out.println("\t\t(" + i + ") " + m);
        }
        System.out.println("\twindow: [" + mWindowRange[0] + ", " + mWindowRange[1] + "]");
        System.out.println("\tchildren: " + getChildCount());
        for (int i = 0; i < getChildCount(); ++i) {
            PageIndicatorMarker m = (PageIndicatorMarker) getChildAt(i);
            System.out.println("\t\t(" + i + ") " + m);
        }
        System.out.println("\tactive: " + mActiveMarkerIndex);
    }
}
