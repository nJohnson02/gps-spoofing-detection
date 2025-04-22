package com.example.spoofdetect;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int HISTORY_LIMIT = 60;
    private static final double AGC_THRESHOLD = -5.0;  // AGC drop (dB)
    private static final double CN0_THRESHOLD = -0.5;  // base threshold for C/N₀
    private static final double CN0_SCALE = 0.25;      // slope factor for boundary

    private LocationManager locationManager;
    private TextView cn0TextView, interferenceTextView;

    private final LinkedList<Double> cn0History = new LinkedList<>();
    private final LinkedList<Double> agcHistory = new LinkedList<>();

    private boolean interferenceAlert = false;
    private MediaPlayer mediaPlayer;

    private boolean historyReady() {
        return cn0History.size() >= HISTORY_LIMIT && agcHistory.size() >= HISTORY_LIMIT;
    }

    private final GnssMeasurementsEvent.Callback gnssCallback = new GnssMeasurementsEvent.Callback() {
        @Override
        public void onGnssMeasurementsReceived(@NonNull GnssMeasurementsEvent eventArgs) {
            List<Double> cn0List = new ArrayList<>();
            List<Double> agcList = new ArrayList<>();

            for (GnssMeasurement m : eventArgs.getMeasurements()) {
                double cn0 = m.getCn0DbHz();
                double agc = m.getAutomaticGainControlLevelDb();

                if (cn0 > 0 && agc > 0) {
                    cn0List.add(cn0);
                    agcList.add((double) agc);
                }

            }

            int satelliteCount = cn0List.size();
            double avgCn0 = average(cn0List);
            double avgAgc = average(agcList);

            if (avgCn0 > 0 && avgAgc > 0) {
                cn0History.add(avgCn0);
                agcHistory.add(avgAgc);
            }

            if (cn0History.size() > HISTORY_LIMIT) {
                cn0History.removeFirst();
                agcHistory.removeFirst();
            }

            runOnUiThread(() -> {
                cn0TextView.setText(String.format("Satellites: %d\nAvg C/N₀: %.2f dB-Hz\nAGC: %.2f dB", satelliteCount, avgCn0, avgAgc));
                if (!historyReady()) {
                    interferenceTextView.setText("Collecting GNSS data...");
                    interferenceTextView.setTextColor(Color.LTGRAY);
                    return;
                } else if (!interferenceAlert) {
                    interferenceTextView.setText("GNSS data ready. Monitoring for spoofing...");
                    interferenceTextView.setTextColor(Color.GREEN);
                }
                checkInterference();
            });
        }
    };

    private void checkInterference() {
        List<Double> recentCn0 = cn0History.subList(50, 60);
        List<Double> prevCn0 = cn0History.subList(0, 50);
        List<Double> recentAgc = agcHistory.subList(50, 60);
        List<Double> prevAgc = agcHistory.subList(0, 50);

        double avgRecentCn0 = average(recentCn0);
        double avgPrevCn0 = average(prevCn0);
        double avgRecentAgc = average(recentAgc);
        double avgPrevAgc = average(prevAgc);

        double deltaCn0 = avgRecentCn0 - avgPrevCn0;
        double deltaAgc = avgRecentAgc - avgPrevAgc;

        if (deltaAgc < AGC_THRESHOLD && !interferenceAlert) {
            if (deltaCn0 < CN0_THRESHOLD + CN0_SCALE * deltaAgc) {
                triggerAlert();
            } else {
                triggerAlert();
            }
        }
    }


    private void triggerAlert() {
        interferenceAlert = true;
        interferenceTextView.setText("⚠️ GPS Spoofing/Jamming Detected");
        interferenceTextView.setTextColor(Color.RED);

        if (mediaPlayer != null) mediaPlayer.release();
        mediaPlayer = MediaPlayer.create(this, R.raw.alert);
        mediaPlayer.start();

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(500);
            }
        }
    }

    private double average(List<Double> values) {
        double sum = 0.0;
        for (double v : values) sum += v;
        return values.isEmpty() ? 0 : sum / values.size();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cn0TextView = findViewById(R.id.cn0_text);
        interferenceTextView = findViewById(R.id.interference_status);

        interferenceTextView.setOnClickListener(v -> {
            interferenceAlert = false;
            interferenceTextView.setText("No GPS interference detected");
            interferenceTextView.setTextColor(Color.WHITE);
        });

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            registerGnssCallback();
        }
    }

    private void registerGnssCallback() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.registerGnssMeasurementsCallback(gnssCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            registerGnssCallback();
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.unregisterGnssMeasurementsCallback(gnssCallback);
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}