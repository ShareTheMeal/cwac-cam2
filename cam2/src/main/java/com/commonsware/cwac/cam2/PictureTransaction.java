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

package com.commonsware.cwac.cam2;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * Class encapsulating the information needed to take a picture
 * using a camera. Use a PictureTransaction.Builder to create
 * an instance. Beyond that, this is an opaque blob to you.
 */
public class PictureTransaction {

    /**
     * Property key to identify the Uri where
     * the image should be written. Look up the value for this
     * property in the PictureTransaction.
     */
    public static final String PROP_OUTPUT = "output";

    /**
     * Property key to identify if the MediaStore should be
     * updated to reflect the written-out picture (boolean).
     * Look up the value for this property in the PictureTransaction.
     * Only relevant if PROP_OUTPUT has a file scheme.
     */
    public static final String PROP_UPDATE_MEDIA_STORE = "update";

    /**
     * Property key for boolean indicating if we should skip the
     * default logic to rotate the image based on the EXIF orientation
     * tag. Defaults to false (meaning: do the rotation if needed).
     */
    public static final String PROP_SKIP_ORIENTATION_NORMALIZATION
            = "skipOrientationNormalization";

  private ArrayList<ImageProcessor> processors=new ArrayList<ImageProcessor>();
  private Bundle props=new Bundle();

  private PictureTransaction() {
    // use the builder, please
  }

  ImageContext process(ImageContext imageContext) {

    for (ImageProcessor processor : processors) {
      imageContext = processor.process(this, imageContext);
    }

    return(imageContext);
  }

  ImageProcessor findProcessorByTag(String tag) {
    for (ImageProcessor processor : processors) {
      if (processor.getTag().equals(tag)) {
        return(processor);
      }
    }

    return(null);
  }

  /**
   * Accesses a Bundle of properties for use by the chain of
   * ImageProcessors. This is public for the use by ImageProcessor
   * implementations. Using these properties in any other fashion
   * is beyond the scope of the supported API.
   *
   * @return the properties Bundle
   */
  public Bundle getProperties() {
    return(props);
  }

  /**
   * Builder class to create an instance of a PictureTransaction.
   */
  public static class Builder {
    private PictureTransaction result=new PictureTransaction();

    /**
     * @return the PictureTransaction built up by the Builder API
     */
    public PictureTransaction build() {
      return(result);
    }

    /**
     * Adds an ImageProcessor to the chain of processors for
     * manipulating the picture.
     *
     * @param processor the ImageProcessor to add to the chain
     * @return the Builder, for more API calls
     */
    public Builder append(ImageProcessor processor) {
      result.processors.add(processor);

      return(this);
    }

    /**
     * Indicates that the picture should be written to the
     * designated Uri.
     *
     * @param ctxt   any Context will do
     * @param output Uri to which you have write
     *               access, where the photo should be taken
     * @param lossless whether it should use PNG (true) or JPEG (false)
     * @param updateMediaStore true if MediaStore should be updated,
     *                         false otherwise
     * @return the Builder, for more API calls
     */
    public Builder toUri(Context ctxt,
            Uri output,
            boolean lossless,
            int jpgQuality,
            boolean updateMediaStore,
            boolean skipOrientationNormalization) {

        if (lossless) {
            PNGWriter png = (PNGWriter) result.findProcessorByTag(PNGWriter.class.getCanonicalName());
            if (png == null) {
                png = new PNGWriter(ctxt);
                append(png);
            }
        } else {
            JPEGWriter jpeg = (JPEGWriter) result.findProcessorByTag(JPEGWriter.class.getCanonicalName());
            if (jpeg == null) {
                jpeg = new JPEGWriter(ctxt);
                append(jpeg);
            }
        }

      result.getProperties().putParcelable(PROP_OUTPUT, output);
      result
          .getProperties()
          .putBoolean(PROP_UPDATE_MEDIA_STORE, updateMediaStore);
      result
        .getProperties()
        .putBoolean(PROP_SKIP_ORIENTATION_NORMALIZATION, skipOrientationNormalization);
      result
              .getProperties()
              .putInt(JPEGWriter.PROP_JPG_QUALITY, jpgQuality);

        return (this);
    }
  }
}
