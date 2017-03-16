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
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import static com.commonsware.cwac.cam2.PictureTransaction.PROP_OUTPUT;
import static com.commonsware.cwac.cam2.PictureTransaction.PROP_SKIP_ORIENTATION_NORMALIZATION;
import static com.commonsware.cwac.cam2.PictureTransaction.PROP_UPDATE_MEDIA_STORE;

/**
 * ImageProcessor that writes a JPEG file out to some form
 * of local storage. At present, it supports writing out to a
 * local filesystem path.
 */
public class JPEGWriter extends AbstractImageProcessor {

  /**
   * {@inheritDoc}
   */
  public JPEGWriter(Context ctxt) {
    super(ctxt);
  }

  /**
   * {@inheritDoc}
   */
  public JPEGWriter(Context ctxt, String tag) {
    super(ctxt, tag);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ImageContext process(PictureTransaction xact, ImageContext imageContext) {
    Uri output=xact.getProperties().getParcelable(PROP_OUTPUT);
    boolean updateMediaStore=xact
        .getProperties()
        .getBoolean(PROP_UPDATE_MEDIA_STORE, false);
    byte[] jpeg=imageContext.getJpeg(!xact
      .getProperties()
      .getBoolean(PROP_SKIP_ORIENTATION_NORMALIZATION, false));

    if (output!=null) {
      try {
        if (output.getScheme().equals("file")) {
          String path=output.getPath();
          File f=new File(path);

          f.getParentFile().mkdirs();

          FileOutputStream fos=new FileOutputStream(f);

          fos.write(jpeg);
          fos.flush();
          fos.getFD().sync();
          fos.close();

          if (updateMediaStore) {
            MediaScannerConnection.scanFile(imageContext.getContext(),
                new String[]{path}, new String[]{"image/jpeg"},
                null);
          }
        }
        else {
          OutputStream out=getContext().getContentResolver().openOutputStream(output);

          out.write(jpeg);
          out.flush();
          out.close();
        }
      }
      catch (Exception e) {
        // throw new UnsupportedOperationException("Exception when trying to write JPEG", e);
        AbstractCameraActivity.BUS.post(new CameraEngine.DeepImpactEvent(e));
      }
    }
    return imageContext;
  }
}
