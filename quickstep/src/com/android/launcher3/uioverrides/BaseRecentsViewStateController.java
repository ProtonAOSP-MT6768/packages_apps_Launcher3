/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.launcher3.uioverrides;

import static com.android.launcher3.anim.Interpolators.AGGRESSIVE_EASE_IN_OUT;
import static com.android.launcher3.anim.Interpolators.FINAL_FRAME;
import static com.android.launcher3.anim.Interpolators.INSTANT;
import static com.android.launcher3.anim.Interpolators.LINEAR;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_OVERVIEW_FADE;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_OVERVIEW_MODAL;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_OVERVIEW_SCALE;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_OVERVIEW_TRANSLATE_X;
import static com.android.launcher3.states.StateAnimationConfig.ANIM_OVERVIEW_TRANSLATE_Y;
import static com.android.launcher3.states.StateAnimationConfig.SKIP_OVERVIEW;
import static com.android.launcher3.testing.TestProtocol.BAD_STATE;
import static com.android.quickstep.views.RecentsView.ADJACENT_PAGE_HORIZONTAL_OFFSET;
import static com.android.quickstep.views.RecentsView.RECENTS_GRID_PROGRESS;
import static com.android.quickstep.views.RecentsView.RECENTS_SCALE_PROPERTY;
import static com.android.quickstep.views.RecentsView.TASK_SECONDARY_TRANSLATION;

import android.util.FloatProperty;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.launcher3.BaseQuickstepLauncher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.anim.PendingAnimation;
import com.android.launcher3.statemanager.StateManager.StateHandler;
import com.android.launcher3.states.StateAnimationConfig;
import com.android.quickstep.views.RecentsView;

/**
 * State handler for recents view. Manages UI changes and animations for recents view based off the
 * current {@link LauncherState}.
 *
 * @param <T> the recents view
 */
public abstract class BaseRecentsViewStateController<T extends RecentsView>
        implements StateHandler<LauncherState> {
    protected final T mRecentsView;
    protected final BaseQuickstepLauncher mLauncher;

    public BaseRecentsViewStateController(@NonNull BaseQuickstepLauncher launcher) {
        mLauncher = launcher;
        mRecentsView = launcher.getOverviewPanel();
    }

    @Override
    public void setState(@NonNull LauncherState state) {
        float[] scaleAndOffset = state.getOverviewScaleAndOffset(mLauncher);
        RECENTS_SCALE_PROPERTY.set(mRecentsView, scaleAndOffset[0]);
        ADJACENT_PAGE_HORIZONTAL_OFFSET.set(mRecentsView, scaleAndOffset[1]);
        TASK_SECONDARY_TRANSLATION.set(mRecentsView, 0f);

        float recentsAlpha = state.overviewUi ? 1f : 0;
        Log.d(BAD_STATE, "BaseRecentsViewStateController setState state=" + state
                + ", alpha=" + recentsAlpha);
        getContentAlphaProperty().set(mRecentsView, recentsAlpha);
        getTaskModalnessProperty().set(mRecentsView, state.getOverviewModalness());
        RECENTS_GRID_PROGRESS.set(mRecentsView,
                state.displayOverviewTasksAsGrid(mLauncher.getDeviceProfile()) ? 1f : 0f);
    }

    @Override
    public void setStateWithAnimation(LauncherState toState, StateAnimationConfig config,
            PendingAnimation builder) {
        Log.d(BAD_STATE, "BaseRecentsViewStateController setStateWithAnimation state=" + toState
                + ", config.skipOverview=" + config.hasAnimationFlag(SKIP_OVERVIEW));
        if (config.hasAnimationFlag(SKIP_OVERVIEW)) {
            return;
        }
        setStateWithAnimationInternal(toState, config, builder);
    }

    /**
     * Core logic for animating the recents view UI.
     *
     * @param toState state to animate to
     * @param config current animation config
     * @param setter animator set builder
     */
    void setStateWithAnimationInternal(@NonNull final LauncherState toState,
            @NonNull StateAnimationConfig config, @NonNull PendingAnimation setter) {
        float[] scaleAndOffset = toState.getOverviewScaleAndOffset(mLauncher);
        setter.setFloat(mRecentsView, RECENTS_SCALE_PROPERTY, scaleAndOffset[0],
                config.getInterpolator(ANIM_OVERVIEW_SCALE, LINEAR));
        setter.setFloat(mRecentsView, ADJACENT_PAGE_HORIZONTAL_OFFSET, scaleAndOffset[1],
                config.getInterpolator(ANIM_OVERVIEW_TRANSLATE_X, LINEAR));
        setter.setFloat(mRecentsView, TASK_SECONDARY_TRANSLATION, 0f,
                config.getInterpolator(ANIM_OVERVIEW_TRANSLATE_Y, LINEAR));

        float recentsAlpha = toState.overviewUi ? 1 : 0;
        Log.d(BAD_STATE, "BaseRecentsViewStateController setStateWithAnimationInternal toState="
                + toState + ", alpha=" + recentsAlpha);
        setter.setFloat(mRecentsView, getContentAlphaProperty(), recentsAlpha,
                config.getInterpolator(ANIM_OVERVIEW_FADE, AGGRESSIVE_EASE_IN_OUT));

        setter.setFloat(
                mRecentsView, getTaskModalnessProperty(),
                toState.getOverviewModalness(),
                config.getInterpolator(ANIM_OVERVIEW_MODAL, LINEAR));
        boolean showAsGrid = toState.displayOverviewTasksAsGrid(mLauncher.getDeviceProfile());
        setter.setFloat(mRecentsView, RECENTS_GRID_PROGRESS, showAsGrid ? 1f : 0f,
                showAsGrid ? INSTANT : FINAL_FRAME);
    }

    abstract FloatProperty getTaskModalnessProperty();

    /**
     * Get property for content alpha for the recents view.
     *
     * @return the float property for the view's content alpha
     */
    abstract FloatProperty getContentAlphaProperty();
}
