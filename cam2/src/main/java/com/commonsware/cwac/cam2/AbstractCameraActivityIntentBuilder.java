package com.commonsware.cwac.cam2;

import com.commonsware.cwac.cam2.util.Utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ResultReceiver;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_ALLOW_SWITCH_FLASH_MODE;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_DEBUG_ENABLED;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_FACING;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_FACING_EXACT_MATCH;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_FAIL_IF_NO_PERMISSION;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_FLASH_MODES;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_FOCUS_MODE;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_FORCE_ENGINE;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_MIRROR_PREVIEW;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_ORIENTATION_LOCK_MODE;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_SHOW_RULE_OF_THIRDS_GRID;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_UNHANDLED_ERROR_RECEIVER;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_UPDATE_MEDIA_STORE;
import static com.commonsware.cwac.cam2.AbstractCameraActivity.EXTRA_ZOOM_STYLE;

abstract public class AbstractCameraActivityIntentBuilder<T extends AbstractCameraActivityIntentBuilder> {

    abstract public Intent buildChooserBaseIntent();

    protected final Intent result;

    private final Context ctxt;

    /**
     * Standard constructor. May throw a runtime exception
     * if the environment is not set up properly (see
     * validateEnvironment() on Utils).
     *
     * @param ctxt any Context will do
     */
    public AbstractCameraActivityIntentBuilder(Context ctxt, Class clazz) {
        this.ctxt = ctxt.getApplicationContext();
        result = new Intent(ctxt, clazz);
    }

    /**
     * Returns the Intent defined by the builder.
     *
     * @return the Intent to use to start the activity
     */
    public Intent build() {
        Utils.validateEnvironment(ctxt,
                result.getBooleanExtra(EXTRA_FAIL_IF_NO_PERMISSION, true));

        return (result);
    }

    /**
     * Returns an ACTION_CHOOSER Intent, to offer a choice
     * between this library's activity and existing camera
     * apps.
     *
     * @param title title for chooser dialog, or null
     * @return the Intent to use to start the activity
     */
    public Intent buildChooser(CharSequence title) {
        Intent original = build();

        Intent toChooseFrom = buildChooserBaseIntent();

        if (original.hasExtra(MediaStore.EXTRA_OUTPUT)) {
            toChooseFrom.putExtra(MediaStore.EXTRA_OUTPUT,
                    original.getParcelableExtra(MediaStore.EXTRA_OUTPUT));
        }

        Intent chooser = Intent.createChooser(toChooseFrom, title);

        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                new Intent[]{original});

        return (chooser);
    }

    /**
     * Indicates what camera should be used as the starting
     * point. Defaults to the rear-facing camera.
     *
     * @param facing which camera to use
     * @return the builder, for further configuration
     */
    public T facing(Facing facing) {
        result.putExtra(EXTRA_FACING, facing);

        return ((T) this);
    }

    /**
     * Indicates that the desired facing value for the camera
     * must be an exact match (and, if not, cancel the request).
     *
     * @return the builder, for further configuration
     */
    public T facingExactMatch() {
        result.putExtra(EXTRA_FACING_EXACT_MATCH, true);

        return ((T) this);
    }

    /**
     * Call if you want extra diagnostic information dumped to
     * LogCat. Not ideal for use in production.
     *
     * @return the builder, for further configuration
     */
    public T debug() {
        result.putExtra(EXTRA_DEBUG_ENABLED, true);

        return ((T) this);
    }

    /**
     * Indicates where to write the picture to. Defaults to
     * returning a thumbnail bitmap in the "data" extra, as
     * with ACTION_IMAGE_CAPTURE. Note that you need to have
     * write access to the supplied file.
     *
     * @param f file in which to write the picture
     * @return the builder, for further configuration
     */
    public T to(File f) {
        return ((T) to(Uri.fromFile(f)));
    }

    /**
     * Indicates where to write the picture to. Defaults to
     * returning a thumbnail bitmap in the "data" extra, as
     * with ACTION_IMAGE_CAPTURE. Note that you need to have
     * write access to the supplied Uri.
     *
     * @param output Uri to which to write the picture
     * @return the builder, for further configuration
     */
    public T to(Uri output) {
        result.putExtra(MediaStore.EXTRA_OUTPUT, output);

        return ((T) this);
    }

    /**
     * Indicates that the picture that is taken should be
     * passed over to MediaStore for indexing. By default,
     * this does not happen automatically and is the responsibility
     * of your app, should the image be reachable by MediaStore
     * in the first place. This setting is only relevant for file://
     * Uri values.
     *
     * @return the builder, for further configuration
     */
    public T updateMediaStore() {
        result.putExtra(EXTRA_UPDATE_MEDIA_STORE, true);

        return ((T) this);
    }

    /**
     * Forces the use of a specific engine based on its ID. Default
     * is an engine chosen by the device we are running on.
     *
     * @param engineId CLASSIC or CAMERA2
     * @return the builder, for further configuration
     */
    public T forceEngine(CameraEngine.ID engineId) {
        result.putExtra(EXTRA_FORCE_ENGINE, engineId);

        return ((T) this);
    }

    @Deprecated
    public T forceClassic() {
        return (forceEngine(CameraEngine.ID.CLASSIC));
    }

    /**
     * Horizontally flips or mirrors the preview images.
     *
     * @return the builder, for further configuration
     */
    public T mirrorPreview() {
        result.putExtra(EXTRA_MIRROR_PREVIEW, true);

        return ((T) this);
    }

    /**
     * Sets the desired focus mode. Default is CONTINUOUS.
     *
     * @return the builder, for further configuration
     */
    public T focusMode(FocusMode focusMode) {
        result.putExtra(EXTRA_FOCUS_MODE, focusMode);

        return ((T) this);
    }

    /**
     * Sets the desired flash mode. This is a suggestion; if
     * the device does not support this mode, the device default
     * behavior will be used.
     *
     * @param mode the desired flash mode
     * @return the builder, for further configuration
     */
    public T flashMode(FlashMode mode) {
        return (flashModes(new FlashMode[]{mode}));
    }

    /**
     * Sets the desired flash modes, in priority-first order
     * (the first flash mode will be used if supported, otherwise
     * the second flash mode will be used if supported, ...).
     * These are a suggestion; if none of these modes are supported,
     * the default device behavior will be used.
     *
     * @param modes the flash modes to try
     * @return the builder, for further configuration
     */
    public T flashModes(FlashMode[] modes) {
        return (flashModes(Arrays.asList(modes)));
    }

    /**
     * Sets the desired flash modes, in priority-first order
     * (the first flash mode will be used if supported, otherwise
     * the second flash mode will be used if supported, ...).
     * These are a suggestion; if none of these modes are supported,
     * the default device behavior will be used.
     *
     * @param modes the flash modes to try
     * @return the builder, for further configuration
     */
    public T flashModes(List<FlashMode> modes) {
        result.putExtra(EXTRA_FLASH_MODES,
                new ArrayList<FlashMode>(modes));

        return ((T) this);
    }

    /**
     * Call if we should allow the user to change the flash mode
     * on the fly (if the camera supports it).
     */
    public T allowSwitchFlashMode() {
        result.putExtra(EXTRA_ALLOW_SWITCH_FLASH_MODE, true);

        return ((T) this);
    }

    /**
     * Indicates the video quality to use for recording this
     * video. Matches EXTRA_VIDEO_QUALITY, except uses an enum
     * for type safety. Note that this is also used for still
     * image quality, despite the name of the extra.
     *
     * @param q LOW or HIGH
     * @return the builder, for further configuration
     */
    public T quality(ImageQuality q) {
        result.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, q.getValue());

        return ((T) this);
    }

    /**
     * Provides a ResultReceiver, which will be invoked on any
     * error that the library cannot handle itself.
     *
     * @param rr a ResultReceiver to get error information
     * @return the builder, for further configuration
     */
    public T onError(ResultReceiver rr) {
        result.putExtra(EXTRA_UNHANDLED_ERROR_RECEIVER, rr);

        return ((T) this);
    }

    /**
     * Specifies an OrientationLockMode to apply to the camera
     * operation.
     *
     * @param mode an OrientationLockMode value
     * @return the builder, for further configuration
     */
    public T orientationLockMode(OrientationLockMode mode) {
        result.putExtra(EXTRA_ORIENTATION_LOCK_MODE, mode);

        return ((T) this);
    }

    /**
     * Call to configure the ZoomStyle to be used. Default
     * is NONE.
     *
     * @return the builder, for further configuration
     */
    public T zoomStyle(ZoomStyle zoomStyle) {
        result.putExtra(EXTRA_ZOOM_STYLE, zoomStyle);

        return ((T) this);
    }

    /**
     * Call to request that the library request permissions from the
     * user, rather than that being handled by the app.
     *
     * @return the builder, for further configuration
     */
    public T requestPermissions() {
        result.putExtra(EXTRA_FAIL_IF_NO_PERMISSION, false);

        return ((T) this);
    }

    /**
     * Call to request that we show a "rule of thirds" grid over the camera
     * preview.
     *
     * @return the builder, for further configuration
     */
    public T showRuleOfThirdsGrid() {
        result.putExtra(EXTRA_SHOW_RULE_OF_THIRDS_GRID, true);

        return ((T) this);
    }
}
