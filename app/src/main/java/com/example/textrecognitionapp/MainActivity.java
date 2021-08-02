package com.example.textrecognitionapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;

import com.example.textrecognitionapp.databinding.ActivityMainBinding;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private CameraSource cameraSource;
    private static final int requestPermissionID = 100;
    private static final String ALPHANUMERIC_REGEX = "[a-zA-Z0-9]+";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        setListeners();
        openCameraSource();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode != requestPermissionID)
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
    }

    private void setListeners()
    {
        binding.btnScanText.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Button btn = (Button) v;

                if (btn.getText().equals("Start Scan"))
                {
                    binding.btnScanText.setText("Stop Scan");
                    startScan();
                }
                else if (btn.getText().equals("Stop Scan"))
                {
                    binding.btnScanText.setText("Start Scan");
                    cameraSource.stop();
                }
            }
        });
    }

    private void startScan()
    {
        try
        {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                return;
            }

            cameraSource.start(binding.svCapturedImage.getHolder());
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void openCameraSource()
    {
        final TextRecognizer textRecognizer = new  TextRecognizer.Builder(getApplicationContext()).build();

        if (textRecognizer.isOperational())
        {
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();

            binding.svCapturedImage.getHolder().addCallback(new SurfaceHolder.Callback()
            {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder)
                {
                    try
                    {

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                        {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                            return;
                        }

                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) { }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder)
                {
                    cameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>()
            {
                @Override
                public void release() { }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections)
                {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();

                    if (items.size() != 0)
                    {
                        binding.tvScannedText.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                StringBuilder stringBuilder = new StringBuilder();

                                for(int i = 0; i < items.size(); i++)
                                {
                                    TextBlock item = items.valueAt(i);
                                    String itemStr = new String(item.getValue());
                                    String[] itemStrArray = itemStr.split("\\*#");

                                    if (itemStrArray.length > 1)
                                    {
                                        for (int j = 1; j < itemStrArray.length; j++)
                                        {
                                            String subItemStr = new String((itemStrArray[j].length() > 30 ? itemStrArray[j].substring(0, 30) : itemStrArray[j]));

                                            if (subItemStr.matches(ALPHANUMERIC_REGEX))
                                            {
                                                stringBuilder.append(subItemStr);
                                                stringBuilder.append("\n");
                                            }
                                        }
                                    }
                                }

                                if (stringBuilder.length() == 0)
                                {
                                    binding.tvScannedText.setText(R.string.scan_text_label);
                                }
                                else
                                {
                                    binding.tvScannedText.setText(stringBuilder.toString());
                                }
                            }
                        });
                    }
                }
            });
        }
    }
}