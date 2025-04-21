package com.example.spoofdetect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float SPOOF_THRESHOLD_METERS = 3.0f;
    private static final int INIT_LOCATION_DELAY_MS = 5000; // 5-second delay before setting initLocation

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TextView locationTextView;
    private TextView initLocationTextView;
    private TextView thresholdTextView;
    private TextView spoofingTextView;
    private RelativeLayout mainLayout;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean initLocationSet = false;
    private MediaPlayer mediaPlayer;

    private Location initLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTextView = findViewById(R.id.locationText);
        initLocationTextView = findViewById(R.id.initLocationText);
        thresholdTextView = findViewById(R.id.thresholdText);
        spoofingTextView = findViewById(R.id.spoofingText);
        mainLayout = findViewById(R.id.mainLayout);

        initLocation = null;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mediaPlayer = MediaPlayer.create(this, R.raw.alert);
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .setWaitForAccurateLocation(false)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationResult.getLastLocation() == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                updateUI(location);

                if (!initLocationSet) {
                    handler.postDelayed(() -> {
                        if (!initLocationSet) {
                            initLocation = location;
                            initLocationSet = true;
                            initLocationTextView.setText("Initial Location:\nLat: " + initLocation.getLatitude() + "\nLon: " + initLocation.getLongitude());
                        }
                    }, INIT_LOCATION_DELAY_MS);
                }

                if (checkSpoofing(location)) {
                    flashRedScreen();
                    playSpoofAlert();
                } else {
                    resetScreen();
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    @SuppressLint("SetTextI18n")
    private void updateUI(Location location) {
        locationTextView.setText("Latitude: " + location.getLatitude() + "\nLongitude: " + location.getLongitude());

        if (initLocationSet) {
            float distance = initLocation.distanceTo(location);
            thresholdTextView.setText("Threshold: " + SPOOF_THRESHOLD_METERS + "m\nCurrent Distance: " + distance + "m");
        }
    }

    private boolean checkSpoofing(Location location) {
        return initLocationSet && initLocation.distanceTo(location) > SPOOF_THRESHOLD_METERS;
    }

    private void flashRedScreen() {
        mainLayout.setBackgroundColor(Color.RED);
        spoofingTextView.setText("SPOOFING DETECTED");
        spoofingTextView.setTextColor(Color.WHITE);
    }

    private void resetScreen() {
        mainLayout.setBackgroundColor(Color.BLACK);
        spoofingTextView.setText("");
    }

    private void playSpoofAlert() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
