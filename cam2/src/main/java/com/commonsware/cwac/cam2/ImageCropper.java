/**
 * Copyright (c) 2015 CommonsWare, LLC
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commonsware.cwac.cam2;

import com.android.mms.exif.ExifInterface;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;

import static com.commonsware.cwac.cam2.JPEGWriter.PROP_JPG_QUALITY;
import static com.commonsware.cwac.cam2.PictureTransaction.PROP_SKIP_ORIENTATION_NORMALIZATION;

/**
 * ImageProcessor that writes a JPEG file out to some form
 * of local storage. At present, it supports writing out to a
 * local filesystem path.
 */
public class ImageCropper extends AbstractImageProcessor {

    private final int maxWidth;

    private final int maxHeight;

    private final boolean resizeFirst;

    /**
     * {@inheritDoc}
     */
    public ImageCropper(Context ctxt, String tag, int width, int height, boolean resizeFirst) {
        super(ctxt, tag);
        this.maxWidth = width;
        this.maxHeight = height;
        this.resizeFirst = resizeFirst;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageContext process(PictureTransaction xact, ImageContext imageContext) {

        try {
            int jpgQuality = xact.getProperties().getInt(PROP_JPG_QUALITY, 100);
            Bitmap bitmap = imageContext.getBitmap(true, !xact
                    .getProperties()
                    .getBoolean(PROP_SKIP_ORIENTATION_NORMALIZATION, false));

            if (resizeFirst) {
                bitmap = resize(bitmap);
            }

            bitmap = crop(bitmap);
            Log.d("CWAC-Cam2", "ImageCropper.process after crop bitmap width = " + bitmap.getWidth() + ", height = " + bitmap.getHeight());

            ExifInterface exif = imageContext.getExifInterface();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, jpgQuality, baos);
            exif.writeExif(bitmap, baos, jpgQuality);
            imageContext.setJpeg(baos.toByteArray());

            Log.d("CWAC-Cam2", "ImageCropper.process after updating ImageContext size = " + imageContext.getJpeg().length + ", jpgQuality = " + jpgQuality);
        } catch (Exception e) {
            // throw new UnsupportedOperationException("Exception when trying to write JPEG", e);
            AbstractCameraActivity.BUS.post(new CameraEngine.DeepImpactEvent(e));
        }

        return imageContext;
    }

    private Bitmap resize(Bitmap bitmap) {
        float width = bitmap.getWidth();
        float height = bitmap.getHeight();

        float ratio = Math.max(maxWidth / width, maxHeight / height);
        return Bitmap.createScaledBitmap(bitmap, (int) (ratio * width), (int) (ratio * height), false);
    }

    private Bitmap crop(Bitmap bitmap) {
        return Bitmap.createBitmap(bitmap, 0, 0, maxWidth, maxHeight);
    }
}
