package com.example.firstapp;


import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private static final String CHANNEL_ID = "channel";

    private static int lastValue = 1;

    private static long lastUpdated = 0l;
    private Map<Integer, PointF> mPreviousProportions = new HashMap<>();

    private ArrayList<Integer> morseCode=new ArrayList<>();

    private ArrayList<Long> historicalEvents=new ArrayList<>();
    private ArrayList<Integer> historicalFrames = new ArrayList<>();

    private SurfaceView mSurfaceView;
    private CameraSource mCameraSource;

    private VideoView mVideoView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.surfaceView);

        // Check for the camera permission before accessing the camera
        int rc = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            try {
                createCameraSource();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            requestCameraPermission();
        }
    }

    private void createCameraSource() throws IOException {
        FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .build();

        FirebaseApp.initializeApp(this);
//        FirebaseDatabase database = FirebaseDatabase.getInstance();
//        DatabaseReference myRef = database.getReference().child("code");



        detector.setProcessor(new Detector.Processor<Face>() {
            @Override
            public void release() {
                Log.d(TAG, "Detector processor released");
            }

            @Override
            public void receiveDetections(Detector.Detections<Face> detections) {
                int facesDetected = detections.getDetectedItems().size();
//                Log.d(TAG, "Faces detected: " + facesDetected);

                // Draw a rectangle around each face
                GraphicOverlay graphicOverlay = findViewById(R.id.graphicOverlay);
                graphicOverlay.clear();

                for (int i = 0; i < facesDetected; ++i) {
                    Face face = detections.getDetectedItems().valueAt(i);
                    PointF leftEye = getLandmarkPosition(face, Landmark.LEFT_EYE);
                    PointF rightEye = getLandmarkPosition(face, Landmark.RIGHT_EYE);

                    EyeData leftEyeData = new EyeData();
                    leftEyeData.setPointF(leftEye);
                    leftEyeData.setOpenScore(face.getIsLeftEyeOpenProbability());

                    EyeData rightEyeData = new EyeData();
                    rightEyeData.setPointF(rightEye);
                    rightEyeData.setOpenScore(face.getIsRightEyeOpenProbability());

                    graphicOverlay.add(new FaceGraphic(graphicOverlay, face));
                    graphicOverlay.add(new EyeGraphic(graphicOverlay, leftEyeData));
                    graphicOverlay.add(new EyeGraphic(graphicOverlay, rightEyeData));

                    eyeCodeProcessing(leftEyeData, rightEyeData);
                }
            }
        });
//        detector.setProcessor(processor);

        if (!detector.isOperational()) {
            Log.w(TAG, "Detector dependencies are not yet available.");
        } else {
            mCameraSource = new CameraSource.Builder(this, detector)
                    .setAutoFocusEnabled(true)
                    .setFacing(CameraSource.CAMERA_FACING_FRONT)
                    .setRequestedFps(60.0f)
                    .setRequestedPreviewSize(2161, 1080)
                    .build();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        mCameraSource.start(mSurfaceView.getHolder());
                    } catch (IOException e) {
                        Log.e(TAG, "Error starting camera source: " + e.getMessage());
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    mCameraSource.stop();
                }
            });
        }
    }

    private PointF getLandmarkPosition(Face face, int landmarkId) {
        for (Landmark landmark : face.getLandmarks()) {
            if (landmark.getType() == landmarkId) {
                return landmark.getPosition();
            }
        }

        PointF prop = mPreviousProportions.get(landmarkId);
        if (prop == null) {
            return null;
        }

        float x = face.getPosition().x + (prop.x * face.getWidth());
        float y = face.getPosition().y + (prop.y * face.getHeight());
        return new PointF(x, y);
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This app needs the Camera permission to work properly. Granting this permission will enable you to use the camera.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    void eyeCodeProcessing(EyeData leftEye, EyeData rightEye) {
        long time = System.currentTimeMillis();

        int frame = (leftEye.getOpenScore() > 0.2 && rightEye.getOpenScore() > 0.2)?1:0;
        historicalFrames.add(frame);

        if (frame == 1) {
            if (historicalFrames.size() > 10) {
                historicalFrames.subList(0,1).clear();
            }
            historicalFrames.add(frame);
            return;
        }

        if (historicalFrames.size() < 5) {
            return;
        }
        boolean event = true;
        for(int i = historicalFrames.size()-1 ; i>=historicalFrames.size()-4;i--) {
            if (historicalFrames.get(i) == 1) {
                event = false;
                break;
            }
        }
        if (event) {
            historicalEvents.add(time);
        }
        System.out.println(time);
        if (historicalEvents.size() > 1) {
            if ((historicalEvents.get(historicalEvents.size() - 1) - historicalEvents.get(historicalEvents.size() - 2)) >= 0 &&
                    (historicalEvents.get(historicalEvents.size() - 1) - historicalEvents.get(historicalEvents.size() - 2)) <= 1000 &&
            historicalEvents.get(historicalEvents.size()-1)-lastUpdated >= 4000) {
                lastUpdated = historicalEvents.get(historicalEvents.size()-1);
                historicalEvents.clear();
                historicalFrames.clear();
                System.out.println("Pause detected !!");
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference().child("code");
//                myRef.setValue(1-myRef.getV);
                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Integer value = dataSnapshot.getValue(Integer.class);
                        if (value != null) {
                            // use the value
                            Log.d(TAG, "Value is: " + value);
                            myRef.setValue(1-value);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w(TAG, "Failed to read value.", databaseError.toException());
                    }
                });
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                        .setContentTitle("Pause detected !")
                        .setContentText("Your blinked twice!");

                // show notification
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    createNotificationChannel();
                    notificationManager.notify(0, builder.build());
                }
            }
        }

        if (historicalEvents.size() > 6) {
            historicalEvents.subList(0,1).clear();
        }

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Add any other properties as needed

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}