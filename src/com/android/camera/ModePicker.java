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

package com.android.camera;

import com.android.camera.Util;
import com.android.camera.ui.RotateImageView;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * A widget that includes three mode selections {@code RotateImageView}'s and
 * a current mode indicator.
 */
public class ModePicker extends RelativeLayout implements View.OnClickListener {
    public static final int MODE_CAMERA = 0;
    public static final int MODE_VIDEO = 1;
    public static final int MODE_PANORAMA = 2;

    // Total mode number
    private static final int MODE_NUM = 3;

    /** A callback to be called when the user wants to switch activity. */
    public interface OnModeChangeListener {
        // Returns true if the listener agrees that the mode can be changed.
        public boolean onModeChanged(int newMode);
    }

    private final int DISABLED_COLOR;
    private final int CURRENT_MODE_BACKGROUND;

    private OnModeChangeListener mListener;
    private View mModeSelectionFrame;
    private RotateImageView mModeSelectionIcon[];
    private View mCurrentModeFrame;
    private RotateImageView mCurrentModeIcon[];
    private View mCurrentModeBar;
    private View mSelectedView;

    private int mCurrentMode = -1;
    private Animation mFadeIn, mFadeOut;

    public ModePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        DISABLED_COLOR = context.getResources().getColor(R.color.icon_disabled_color);
        CURRENT_MODE_BACKGROUND = R.drawable.btn_mode_background;
        mFadeIn = AnimationUtils.loadAnimation(
                context, R.anim.mode_selection_fade_in);
        mFadeOut = AnimationUtils.loadAnimation(
                context, R.anim.mode_selection_fade_out);
        mFadeOut.setAnimationListener(mAnimationListener);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();

        mModeSelectionFrame = findViewById(R.id.mode_selection);
        mModeSelectionIcon = new RotateImageView[MODE_NUM];
        mModeSelectionIcon[MODE_PANORAMA] =
                (RotateImageView) findViewById(R.id.mode_panorama);
        mModeSelectionIcon[MODE_VIDEO] =
                (RotateImageView) findViewById(R.id.mode_video);
        mModeSelectionIcon[MODE_CAMERA] =
                (RotateImageView) findViewById(R.id.mode_camera);

        // The current mode frame is for Phone UI only.
        mCurrentModeFrame = findViewById(R.id.current_mode);
        if (mCurrentModeFrame != null) {
            mCurrentModeFrame.setOnClickListener(this);
            mCurrentModeIcon = new RotateImageView[MODE_NUM];
            mCurrentModeIcon[0] = (RotateImageView) findViewById(R.id.mode_0);
            mCurrentModeIcon[1] = (RotateImageView) findViewById(R.id.mode_1);
            mCurrentModeIcon[2] = (RotateImageView) findViewById(R.id.mode_2);
        } else {
            // current_mode_bar is only for tablet.
            mCurrentModeBar = findViewById(R.id.current_mode_bar);
            enableModeSelection(true);
        }
    }

    private AnimationListener mAnimationListener = new AnimationListener() {
        public void onAnimationEnd(Animation animation) {
            changeToSelectedMode();
            mCurrentModeFrame.setVisibility(View.VISIBLE);
            mModeSelectionFrame.setVisibility(View.GONE);
        }

        public void onAnimationRepeat(Animation animation) {
        }

        public void onAnimationStart(Animation animation) {
        }
    };

    private void enableModeSelection(boolean enabled) {
        if (mCurrentModeFrame != null) {
            // Animation Effect is applied on Phone UI only.
            mModeSelectionFrame.startAnimation(enabled ? mFadeIn : mFadeOut);
            if (enabled) {
                mModeSelectionFrame.setVisibility(View.VISIBLE);
                mCurrentModeFrame.setVisibility(View.GONE);
            }
            mCurrentModeFrame.setOnClickListener(enabled ? null : this);
        }
        for (int i = 0; i < MODE_NUM; ++i) {
            mModeSelectionIcon[i].setOnClickListener(enabled ? this : null);
        }
        updateModeState();
    }

    private void changeToSelectedMode() {
        for (int i = 0; i < MODE_NUM; ++i) {
            if (mSelectedView == mModeSelectionIcon[i]) {
                setCurrentMode(i);
                return;
            }
        }
    }

    public void onClick(View view) {
        if (view == mCurrentModeFrame) {
            enableModeSelection(true);
        } else {
            // The view in selection menu is clicked.
            mSelectedView = view;
            if (mCurrentModeBar == null) {
                enableModeSelection(false);
            } else {
                changeToSelectedMode();
            }
        }
    }

    private void setMode(int mode) {
        for (int i = 0; i < MODE_NUM; ++i) {
            mModeSelectionIcon[i].setSelected(mode == i);
        }
    }

    public void setOnModeChangeListener(OnModeChangeListener listener) {
        mListener = listener;
    }

    public void setCurrentMode(int mode) {
        if (mCurrentMode == mode) return;
        setMode(mode);
        tryToSetMode(mode);
    }

    private void tryToSetMode(int mode) {
        if (mListener != null) {
            if (!mListener.onModeChanged(mode)) {
                setMode(mCurrentMode);
                return;
            }
        }
        mCurrentMode = mode;
        updateModeState();
    }

    public boolean onModeChanged(int mode) {
        setCurrentMode(mode);
        return true;
    }

    public void setDegree(int degree) {
        for (int i = 0; i < MODE_NUM; ++i) {
            mModeSelectionIcon[i].setDegree(degree);
            if (mCurrentModeFrame != null) {
                mCurrentModeIcon[i].setDegree(degree);
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        // Enable or disable the frames.
        if (mCurrentModeFrame != null) mCurrentModeFrame.setEnabled(enabled);
        mModeSelectionFrame.setEnabled(enabled);

        // Enable or disable the icons.
        for (int i = 0; i < MODE_NUM; ++i) {
            mModeSelectionIcon[i].setEnabled(enabled);
            if (mCurrentModeFrame != null) mCurrentModeIcon[i].setEnabled(enabled);
        }
        if (enabled) updateModeState();
    }

    private void highlightView(View view, boolean enabled) {
        Drawable drawable = ((ImageView) view).getDrawable();
        if (enabled) {
            drawable.clearColorFilter();
        } else {
            drawable.setColorFilter(DISABLED_COLOR, PorterDuff.Mode.SRC_ATOP);
        }
        requestLayout();
    }

    private void updateModeState() {
        // Grey-out the unselected icons.
        for (int i = 0; i < MODE_NUM; ++i) {
            highlightView(mModeSelectionIcon[i], (i == mCurrentMode));
        }
        // Update the current mode icons on the Phone UI. The selected mode
        // should be in the center of the current mode icon bar.
        if (mCurrentModeFrame != null) {
            for (int i = 0, j = 0; i < MODE_NUM; ++i) {
                int target;
                if (i == 1) {
                    // The second icon is always the selected mode.
                    target = mCurrentMode;
                } else {
                    // Set the icons in order of camera, video and panorama.
                    if (j == mCurrentMode) j++;
                    target = j++;
                }
                mCurrentModeIcon[i].setImageDrawable(
                        mModeSelectionIcon[target].getDrawable());
            }
        }
    }

    @Override
    protected void onLayout(
            boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // Layout the current mode indicator bar.
        if (mCurrentModeBar != null) {
            int viewWidth = mModeSelectionIcon[MODE_CAMERA].getWidth();
            int iconWidth = ((ImageView) mModeSelectionIcon[MODE_CAMERA])
                    .getDrawable().getIntrinsicWidth();
            int padding = (viewWidth - iconWidth) / 2;
            int l = mModeSelectionFrame.getLeft() + mCurrentMode * viewWidth;
            mCurrentModeBar.layout((l + padding),
                    (bottom - top - mCurrentModeBar.getHeight()),
                    (l + padding + iconWidth),
                    (bottom - top));
        }
    }
}
