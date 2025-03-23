# ReplayViewer - Name bound to change at some point

## Description
This is an Android camera / delayed video stream application meant primarily for capturing your own sports moments, and then watching it on the delayed stream and possibly saving the clip as an mp4 file.
Features include:
- Custom configurations including the ability to choose resolution, frame rate, front/back camera, delay and mediaplayer clip length.
- Easy stream realtime/delay mode switching for quick re-centering of the device on a tripod etc.
- Saved configurations, save time by saving your favourite location based configurations.
- Responsive in-app mediaplayer that allows you to easily replay the saved clip with good controls.
- Ability to save current frame as an image or the whole clip as a video into your media gallery.


## Requirements
- Android device with decent performance; Might not work on some older models.
    - Optionally: Run through Android studio using emulator(Will propably be quite slow and not give a good image of the app.)

 ### Permissions
 - Camera
 - Write/Read files
 
To install the app, download the .apk from the latest release onto an Android device and install it. 
Doing this might require you to give the device permission to use unknown files.

## Libraries and Frameworks Used

- **Kotlin**: The primary language used for development.
- **Gradle**: Used for dependency management and build automation.
- **Android Jetpack Compose**: Primary UI toolkit used to build the interfaces for the application.
- **Kotlin Coroutines**: Used for managing background threads, critical for processing frames with the limited performance of mobile devices.
- **Android MediaMuxer & MediaCodec**: Used for video processing, encoding mp4 files out of saved video clips.
- **Android CameraX**: Highly automated camera library that allows fast/easy use-cases for camera. ( Might switch to using Camera2 only in future )
- **Android Camera2**: Low-level camera library that allows higher degree of control over cameras and more complex use-cases than CameraX alone.
- **Material3**: Material Design library for easy components.

## Images
![image](https://github.com/user-attachments/assets/469b9038-104a-427f-af18-63a364e43fa0)
![image](https://github.com/user-attachments/assets/cb21d9d0-2168-4a99-9d96-0002a0f1d47d)
![image](https://github.com/user-attachments/assets/6548315f-9c4e-4848-b799-9864c62785ab)
![image](https://github.com/user-attachments/assets/a1f50e4c-4ee3-4bae-891d-4e171c893373)
![image](https://github.com/user-attachments/assets/5836d98d-e5ed-487e-8146-f7358bf53740)




