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

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import static com.commonsware.cwac.cam2.PictureTransaction.PROP_OUTPUT;
import static com.commonsware.cwac.cam2.PictureTransaction.PROP_SKIP_ORIENTATION_NORMALIZATION;
import static com.commonsware.cwac.cam2.PictureTransaction.PROP_UPDATE_MEDIA_STORE;

/**
 * ImageProcessor that writes a PNG file out to some form
 * of local storage. At present, it supports writing out to a
 * local filesystem path.
 */
public class PNGWriter extends AbstractImageProcessor {

    /**
     * {@inheritDoc}
     */
    public PNGWriter(Context ctxt) {
        super(ctxt);
    }

    /**
     * {@inheritDoc}
     */
    public PNGWriter(Context ctxt, String tag) {
        super(ctxt, tag);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageContext process(PictureTransaction xact, ImageContext imageContext) {
        Uri output = xact.getProperties().getParcelable(PROP_OUTPUT);
        boolean updateMediaStore = xact
                .getProperties()
                .getBoolean(PROP_UPDATE_MEDIA_STORE, false);

        if (output != null) {
            try {
                if (output.getScheme().equals("file")) {
                    String path = output.getPath();
                    if (path.contains(".jpg")) {
                        path = path.replace(".jpg", ".png");
                    } else {
                        path = path + ".png";
                    }

                    File f = new File(path);

                    f.getParentFile().mkdirs();

                    FileOutputStream fos = new FileOutputStream(f);

                    Bitmap bitmap = imageContext.getBitmap(true, !xact
                            .getProperties()
                            .getBoolean(PROP_SKIP_ORIENTATION_NORMALIZATION, false));
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

                    fos.flush();
                    fos.getFD().sync();
                    fos.close();

                    if (updateMediaStore) {
                        MediaScannerConnection.scanFile(imageContext.getContext(),
                                new String[]{path}, new String[]{"image/png"},
                                null);
                    }
                } else {
                    OutputStream out = getContext().getContentResolver().openOutputStream(output);

                    Bitmap bitmap = imageContext.getBitmap(true, !xact
                            .getProperties()
                            .getBoolean(PROP_SKIP_ORIENTATION_NORMALIZATION, false));
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

                    out.flush();
                    out.close();
                }
            } catch (Exception e) {
                // throw new UnsupportedOperationException("Exception when trying to write PNG", e);
                AbstractCameraActivity.BUS.post(new CameraEngine.DeepImpactEvent(e));
            }
        }
        return imageContext;
    }
}
