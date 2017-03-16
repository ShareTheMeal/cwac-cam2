/***
 Copyright (c) 2015 CommonsWare, LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.commonsware.cwac.cam2.singleactivity.demo;

import com.commonsware.cwac.cam2.AbstractCameraActivity;
import com.commonsware.cwac.cam2.CameraEngine;
import com.commonsware.cwac.cam2.CameraFragmentInterface;
import com.commonsware.cwac.cam2.FlashMode;
import com.commonsware.cwac.cam2.ImageContext;
import com.commonsware.cwac.cam2.OrientationLockMode;
import com.commonsware.cwac.cam2.ZoomStyle;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android.Manifest;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AbstractCameraActivity {

    private static final String[] PERMS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private boolean isDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        debugEnabled = true;
        jpegQuality = 75;
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;

        super.onDestroy();
    }

    @Override
    protected String[] getNeededPermissions() {
        return (PERMS);
    }

    @Override
    protected void init() {
        super.init();

        if (!cameraFrag.isVisible()) {
            getFragmentManager()
                    .beginTransaction()
                    .show((Fragment) cameraFrag)
                    .commit();
        }
    }

    @Override
    protected boolean failIfNoPermissions() {
        return false;
    }

    @Override
    protected OrientationLockMode getOrientationLockMode() {
        return OrientationLockMode.PORTRAIT;
    }

    @Override
    protected Uri getOutputUri() {
        String filename = "cam2_";
        File testRoot = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename);

        return Uri.fromFile(testRoot);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CameraEngine.PictureTakenEvent event) {
        if (event.exception == null && !isDestroyed) {
            ImageContext imageContext = event.getImageContext();
            Bitmap thumbnail = imageContext.buildResultThumbnail(true);

            Toast.makeText(this,
                    "got image = " + getOutputUri() + ", thumbnail size = " + thumbnail.getByteCount() + ", full image size = " + imageContext
                            .getJpeg().length, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "error getting image = " + event.exception, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected boolean needsOverlay() {
        return (true);
    }

    @Override
    protected boolean needsActionBar() {
        return (true);
    }

    @Override
    protected boolean isVideo() {
        return (false);
    }

    @Override
    protected void configEngine(CameraEngine engine) {
        if (debugEnabled) {
            engine.setDebugSavePreviewFile(new File(getExternalCacheDir(), "cam2-preview.jpg"));
        }

        List<FlashMode> flashModes =
                (List<FlashMode>) getIntent().getSerializableExtra(EXTRA_FLASH_MODES);

        if (flashModes == null) {
            flashModes = new ArrayList<FlashMode>();
        }

        if (flashModes != null) {
            engine.setPreferredFlashModes(flashModes);
        }
    }

    @Override
    protected CameraFragmentInterface buildFragment() {
        return (SinglePhotoFragment.newPictureInstance(getOutputUri(),
                true,
                640,
                false,
                ZoomStyle.SEEKBAR,
                false,
                false));
    }

}
