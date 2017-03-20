/***
 * Copyright (c) 2015-2016 CommonsWare, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commonsware.cwac.cam2.support;

import com.commonsware.cwac.cam2.CameraController;
import com.commonsware.cwac.cam2.CameraEngine;
import com.commonsware.cwac.cam2.CameraFragment;
import com.commonsware.cwac.cam2.CameraFragmentInterface;
import com.commonsware.cwac.cam2.CameraSelectionCriteria;
import com.commonsware.cwac.cam2.Facing;
import com.commonsware.cwac.cam2.FocusMode;
import com.commonsware.cwac.cam2.OrientationLockMode;
import com.commonsware.cwac.cam2.util.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import java.util.ArrayList;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

/**
 * Base class for activities that integrate with CameraFragment
 * for taking pictures or recording video.
 */
abstract public class AbstractCameraAppCompatActivity extends AppCompatActivity {

    /**
     * List<FlashMode> indicating the desired flash modes,
     * or null for always taking the default. These are
     * considered in priority-first order (i.e., we will use
     * the first FlashMode if the device supports it, otherwise
     * we will use the second FlashMode, ...). If there is no
     * match, whatever the default device behavior is will be
     * used.
     */
    public static final String EXTRA_FLASH_MODES =
            "cwac_cam2_flash_modes";

    /**
     * True if we should allow the user to change the flash mode
     * on the fly (if the camera supports it), false otherwise.
     * Defaults to false.
     */
    public static final String EXTRA_ALLOW_SWITCH_FLASH_MODE =
            "cwac_cam2_allow_switch_flash_mode";

    /**
     * A ResultReceiver to be invoked on any error that the library
     * cannot handle internally.
     */
    public static final String EXTRA_UNHANDLED_ERROR_RECEIVER =
            "cwac_cam2_unhandled_error_receiver";

    /**
     * Extra name for indicating what facing rule for the
     * camera you wish to use. The value should be a
     * CameraSelectionCriteria.Facing instance.
     */
    public static final String EXTRA_FACING = "cwac_cam2_facing";

    /**
     * Extra name for indicating that the requested facing
     * must be an exact match, without gracefully degrading to
     * whatever camera happens to be available. If set to true,
     * requests to take a picture, for which the desired camera
     * is not available, will be cancelled. Defaults to false.
     */
    public static final String EXTRA_FACING_EXACT_MATCH =
            "cwac_cam2_facing_exact_match";

    /**
     * Extra name for indicating whether extra diagnostic
     * information should be reported, particularly for errors.
     * Default is false.
     */
    public static final String EXTRA_DEBUG_ENABLED = "cwac_cam2_debug";

    /**
     * Extra name for indicating the quality of the JPEG image
     * returned by the camera, if JPEG is used as format.
     * Default is 100 (maximum).
     */
    public static final String EXTRA_JPEG_QUALITY = "cwac_cam2_jpeg_quality";

    /**
     * Extra name for indicating if MediaStore should be updated
     * to reflect a newly-taken picture. Only relevant if
     * a file:// Uri is used. Default to false.
     */
    public static final String EXTRA_UPDATE_MEDIA_STORE =
            "cwac_cam2_update_media_store";

    /**
     * DO NOT USE. Use EXTRA_FORCE_ENGINE instead, please.
     */
    @Deprecated
    public static final String EXTRA_FORCE_CLASSIC = "cwac_cam2_force_classic";

    /**
     * If set to a CameraEngine.ID value (CLASSIC or CAMERA2), will
     * force the use of that engine. If left null/unset, the default
     * is based on what device we are running on.
     */
    public static final String EXTRA_FORCE_ENGINE = "cwac_cam2_force_engine";

    /**
     * If set to true, horizontally flips or mirrors the preview.
     * Does not change the picture or video output. Used mostly for FFC,
     * though will be honored for any camera. Defaults to false.
     */
    public static final String EXTRA_MIRROR_PREVIEW = "cwac_cam2_mirror_preview";

    /**
     * Extra name for focus mode to apply. Value should be one of the
     * FocusMode enum values. Default is CONTINUOUS.
     * If the desired focus mode is not available, the device default
     * focus mode is used.
     */
    public static final String EXTRA_FOCUS_MODE = "cwac_cam2_focus_mode";

    /**
     * Extra name for orientation lock mode to apply. Value should be
     * one of the OrientationLockMode values. Default, shockingly,
     * is DEFAULT.
     */
    public static final String EXTRA_ORIENTATION_LOCK_MODE =
            "cwac_cam2_olock_mode";

    /**
     * Extra name for whether the camera should allow zoom and
     * how. Value should be a ZoomStyle (NONE, PINCH, SEEKBAR).
     * Default is NONE.
     */
    public static final String EXTRA_ZOOM_STYLE =
            "cwac_cam2_zoom_style";

    /**
     * Extra name for runtime permission policy. If true, we check
     * for runtime permissions and fail fast if they are not already
     * granted. If false, if we lack runtime permissions (and need them
     * based on API level), we request them ourselves. Defaults to true.
     */
    public static final String EXTRA_FAIL_IF_NO_PERMISSION =
            "cwac_cam2_fail_if_no_permission";

    /**
     * Extra name for whether the camera should show a "rule of thirds"
     * overlay above the camera preview. Defaults to false.
     */
    public static final String EXTRA_SHOW_RULE_OF_THIRDS_GRID =
            "cwac_cam2_show_rule_of_thirds_grid";

    protected FocusMode focusMode;

    protected boolean allowChangeFlashMode;

    protected ResultReceiver onError;

    protected Facing facing;

    protected boolean matchFacingExactly;

    protected CameraEngine.ID forcedEngineId;

    protected boolean debugEnabled;

    protected boolean mirrorPreview;

    protected int jpegQuality;

    /**
     * @return true if we are recording a video, false if we are
     * taking a still picture
     */
    abstract protected boolean isVideo();

    /**
     * @return a CameraFragment for the given circumstances
     */
    abstract protected CameraFragmentInterface buildFragment();

    /**
     * @return array of the names of the permissions needed by
     * this activity
     */
    abstract protected String[] getNeededPermissions();

    /**
     * Configure the CameraEngine for things that are specific
     * to a subclass.
     *
     * @param engine the CameraEngine to configure
     */
    abstract protected void configEngine(CameraEngine engine);

    protected static final String TAG_CAMERA = CameraFragment.class.getCanonicalName();

    private static final int REQUEST_PERMS = 13401;

    protected CameraFragmentInterface cameraFrag;

    public static final EventBus BUS = new EventBus();

    /**
     * Standard lifecycle method, serving as the main entry
     * point of the activity.
     *
     * @param savedInstanceState the state of a previous instance
     */
    @TargetApi(23)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.validateEnvironment(this, failIfNoPermissions());

        lockOrientation(getOrientationLockMode());

        //initing everything here, so in case of overriding onCreate(), everything will be ready in init()
        focusMode =
                (FocusMode) getIntent().getSerializableExtra(EXTRA_FOCUS_MODE);
        allowChangeFlashMode =
                getIntent().getBooleanExtra(EXTRA_ALLOW_SWITCH_FLASH_MODE, false);
        onError =
                getIntent().getParcelableExtra(EXTRA_UNHANDLED_ERROR_RECEIVER);

        mirrorPreview = getIntent()
                .getBooleanExtra(EXTRA_MIRROR_PREVIEW, false);

        facing =
                (Facing) getIntent().getSerializableExtra(EXTRA_FACING);

        matchFacingExactly = getIntent()
                .getBooleanExtra(EXTRA_FACING_EXACT_MATCH, false);

        forcedEngineId = (CameraEngine.ID) getIntent().getSerializableExtra(EXTRA_FORCE_ENGINE);

        debugEnabled = getIntent().getBooleanExtra(EXTRA_DEBUG_ENABLED, false);

        jpegQuality = getIntent().getIntExtra(EXTRA_JPEG_QUALITY, 100);
    }

    @TargetApi(23)
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        //we remove the shadow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
        }

        if (useRuntimePermissions()) {
            String[] perms = netPermissions(getNeededPermissions());

            if (perms.length == 0) {
                init();
            } else if (!failIfNoPermissions()) {
                requestPermissions(perms, REQUEST_PERMS);
            } else {
                throw new IllegalStateException("We lack the necessary permissions!");
            }
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions,
            int[] grantResults) {
        String[] perms = netPermissions(getNeededPermissions());

        if (perms.length == 0) {
            init();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    /**
     * Standard lifecycle method, for when the fragment moves into
     * the started state. Passed along to the CameraController.
     */
    @Override
    public void onStart() {
        super.onStart();

        BUS.register(this);
    }

    /**
     * Standard lifecycle method, for when the fragment moves into
     * the stopped state. Passed along to the CameraController.
     */
    @Override
    public void onStop() {
        BUS.unregister(this);

        if (cameraFrag != null) {
            if (isChangingConfigurations()) {
                cameraFrag.stopVideoRecording();
            } else {
                cameraFrag.shutdown();
            }
        }

        super.onStop();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            cameraFrag.performCameraAction();

            return (true);
        }

        return (super.onKeyUp(keyCode, event));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CameraController.NoSuchCameraEvent event) {
        finish();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CameraController.ControllerDestroyedEvent event) {
        finish();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CameraEngine.CameraTwoGenericEvent event) {
        finish();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CameraEngine.DeepImpactEvent event) {
        finish();
    }

    protected Uri getOutputUri() {
        Uri output = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            ClipData clipData = getIntent().getClipData();

            if (clipData != null && clipData.getItemCount() > 0) {
                output = clipData.getItemAt(0).getUri();
            }
        }

        if (output == null) {
            output = getIntent().getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        }

        return (output);
    }

    protected void init() {
        if (cameraFrag == null) {
            cameraFrag = (CameraFragmentInterface) getFragmentManager().findFragmentByTag(TAG_CAMERA);
        }

        boolean fragNeedsToBeAdded = false;

        if (cameraFrag == null) {
            cameraFrag = buildFragment();

            if (cameraFrag instanceof Fragment) {
                fragNeedsToBeAdded = true;
            }
        }

        CameraController ctrl =
                new CameraController(focusMode, onError,
                        allowChangeFlashMode, isVideo(), jpegQuality);

        cameraFrag.setController(ctrl);
        cameraFrag.setMirrorPreview(mirrorPreview);

        if (facing == null) {
            facing = Facing.BACK;
        }

        CameraSelectionCriteria criteria =
                new CameraSelectionCriteria.Builder()
                        .facing(facing)
                        .facingExactMatch(matchFacingExactly)
                        .build();

        ctrl.setEngine(CameraEngine.buildInstance(this, forcedEngineId), criteria);
        ctrl.getEngine().setDebug(debugEnabled);
        configEngine(ctrl.getEngine());

        if (fragNeedsToBeAdded) {
            getFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, (Fragment) cameraFrag, TAG_CAMERA)
                    .commit();
        }
    }

    boolean canSwitchSources() {
        return (!matchFacingExactly);
    }

    protected OrientationLockMode getOrientationLockMode() {
        return (OrientationLockMode) getIntent().getSerializableExtra(EXTRA_ORIENTATION_LOCK_MODE);
    }

    protected void lockOrientation(OrientationLockMode mode) {
        if (mode == null || mode == OrientationLockMode.DEFAULT) {
            int orientation = getResources().getConfiguration().orientation;

            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
            } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(SCREEN_ORIENTATION_UNSPECIFIED);
            }
        } else if (mode == OrientationLockMode.LANDSCAPE) {
            setRequestedOrientation(SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(
                    SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }

    @TargetApi(23)
    private boolean hasPermission(String perm) {
        if (useRuntimePermissions()) {
            return (checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED);
        }

        return (true);
    }

    private boolean useRuntimePermissions() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    protected boolean failIfNoPermissions() {
        return (getIntent().getBooleanExtra(EXTRA_FAIL_IF_NO_PERMISSION, true));
    }

    private String[] netPermissions(String[] wanted) {
        ArrayList<String> result = new ArrayList<String>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return (result.toArray(new String[result.size()]));
    }

}
