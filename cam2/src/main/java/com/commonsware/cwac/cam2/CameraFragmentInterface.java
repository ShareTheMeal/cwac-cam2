package com.commonsware.cwac.cam2;

public interface CameraFragmentInterface {

    boolean isVisible();

    void setMirrorPreview(boolean setMirror);

    void setController(CameraController controller);

    void performCameraAction();

    void stopVideoRecording();

    void shutdown();

}
