package com.cb3g.channel19;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.android.multidex.myapplication.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.vdurmont.emoji.EmojiParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Locations extends AppCompatActivity implements ChildEventListener, OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraMoveListener {
    Location myLocation = null;
    private GoogleMap map;
    private boolean update = true;
    private ImageView profile;
    private TextView handle;
    private TextView location;
    private TextView speed;
    private TextView altitude;
    private TextView distance;
    private TextView direction;
    private ConstraintLayout infoTab;
    private Coordinates selected = null;
    private final List<Marker> markers = new ArrayList<>();
    private FusedLocationProviderClient mFusedLocationClient;
    private final LocationCallback locationCallback = new locationCallback();
    private Coordinates userToFind = null;
    private boolean follow = false;
    private SeekBar zoomBar;

    public void startOrStopGPS(boolean start) {
        if (start) {
            if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) && RadioService.operator.getLocationEnabled().get()) {
                if (mFusedLocationClient == null)
                    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 50000)
                        .setWaitForAccurateLocation(true)
                        .setMinUpdateIntervalMillis(50000)
                        .setMaxUpdateDelayMillis(30000)
                        .build();
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        } else {
            if (mFusedLocationClient != null) {
                mFusedLocationClient.removeLocationUpdates(locationCallback);
                mFusedLocationClient = null;
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (infoTab.getVisibility() == View.VISIBLE) {
            selected = null;
            updateSelected();
        } else super.onBackPressed();
    }

    private void updateSelected() {
        if (selected == null) {
            infoTab.setVisibility(View.GONE);
            map.setPadding(0, 0, 0, 0);
        } else {
            infoTab.setVisibility(View.VISIBLE);
            map.setPadding(0, 0, 0, infoTab.getHeight());
            if (!this.isFinishing())
                Glide.with(this).load(selected.getProfile()).apply(RadioService.profileOptions).thumbnail(0.1f).into(profile);
            handle.setText(selected.getHandle());
            for (UserListEntry entry : RadioService.users) {
                if (entry.getUser_id().equals(selected.getUserId()))
                    location.setText(entry.getHometown().replace(EmojiParser.parseToUnicode(" :globe_with_meridians:"), "").trim());
            }
            altitude.setText("Altitude: " + Math.round(selected.getAltitude() * 3.28084) + "ft");
            speed.setText("Speed: " + Math.round(selected.getSpeed() * 2.2369f) + " mph");
            direction.setText("Bearing: " + getBearing(selected.getBearing()) + " - " + selected.getBearing() + "°");
            if (myLocation != null)
                distance.setText("Distance: " + getDistance(myLocation, selected.getLatitude(), selected.getLongitude()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.map_layout);
        infoTab = findViewById(R.id.info_tab);
        profile = findViewById(R.id.black_profile_picture_iv);
        handle = findViewById(R.id.black_handle_tv);
        location = findViewById(R.id.black_banner_tv);
        speed = findViewById(R.id.speed);
        altitude = findViewById(R.id.altitude);
        direction = findViewById(R.id.direction);
        distance = findViewById(R.id.distance);
        SwitchCompat trace = findViewById(R.id.follow);
        trace.getThumbDrawable().setColorFilter(Utils.colorFilter(Color.WHITE));
        trace.getTrackDrawable().setColorFilter(Utils.colorFilter(Color.BLACK));
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (getIntent().hasExtra("data")) {
            userToFind = RadioService.gson.fromJson(getIntent().getStringExtra("data"), Coordinates.class);
            selected = userToFind;
        }
        trace.setChecked(follow);
        trace.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Utils.vibrate(buttonView);
            follow = isChecked;
            if (isChecked) {
                trace.getTrackDrawable().setColorFilter(Utils.colorFilter(Color.WHITE));
                if (follow)
                    moveCamera(selected.getLatitude(), selected.getLongitude(), map.getCameraPosition().zoom);
            } else trace.getTrackDrawable().setColorFilter(Utils.colorFilter(Color.BLACK));
        });
        zoomBar = findViewById(R.id.zoomBar);
        zoomBar.getProgressDrawable().setColorFilter(Utils.colorFilter(Color.WHITE));
        zoomBar.getThumb().setColorFilter(Utils.colorFilter(Color.WHITE));
        zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (follow)
                        moveCamera(selected.getLatitude(), selected.getLongitude(), (float) progress);
                    else map.moveCamera(CameraUpdateFactory.zoomTo(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        startOrStopGPS(true);
        listenForCoordinates(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        startOrStopGPS(false);
        listenForCoordinates(false);
    }

    public void listenForCoordinates(boolean listen) {
        if (listen)
            RadioService.databaseReference.child("locations").addChildEventListener(this);
        else {
            RadioService.databaseReference.child("locations").removeEventListener(this);
        }
    }

    @Override
    public void onCameraIdle() {
        if (update) {
            map.getUiSettings().setMapToolbarEnabled(false);
            map.getUiSettings().setZoomControlsEnabled(true);
            if (ActivityCompat.checkSelfPermission(Locations.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                myLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(new Criteria(), false));
            }
            if (getSharedPreferences("settings", MODE_PRIVATE).getBoolean("darkmap", true))
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(Locations.this, R.raw.map_dark));
            for (Coordinates coordinate : RadioService.coordinates) {
                if (coordinate.getLatitude() != 0 && coordinate.getLongitude() != 0 && isInChannel(coordinate.getUserId())) {
                    Marker marker;
                    marker = map.addMarker(new MarkerOptions().position(new LatLng(coordinate.getLatitude(), coordinate.getLongitude())).title(coordinate.getHandle()));
                    if (coordinate.getBearing() != 0) {
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.arrow));
                        marker.setRotation(coordinate.getBearing());
                    } else
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.star));
                    marker.setTag(coordinate);
                    marker.setFlat(true);
                    markers.add(marker);
                }
            }
            if (userToFind == null) {
                if (myLocation != null)
                    moveCamera(myLocation.getLatitude(), myLocation.getLongitude(), 6.f);
                else
                    moveCamera(39.131101, -95.799165, 2.5f);
            } else {
                moveCamera(userToFind.getLatitude(), userToFind.getLongitude(), 8f);
                updateSelected();
            }
            update = false;
        }
    }

    private void moveCamera(double lattitude, double longitude, float zoom) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lattitude, longitude), zoom));
    }

    private boolean isInChannel(String userId) {
        if (RadioService.users.isEmpty() || RadioService.operator.getUser_id().equals(userId))
            return false;
        for (UserListEntry user : RadioService.users) {
            if (user.getUser_id().equals(userId)) return true;
        }
        return false;
    }

    private String getDistance(Location from, double latitude, double longitude) {
        Location des = new Location("providername");
        des.setLatitude(latitude);
        des.setLongitude(longitude);
        return " " + String.format(Locale.getDefault(), "%.0f", ((float) 0.621371 * from.distanceTo(des)) / 1000) + "m";
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        zoomBar.setMax((int) map.getMaxZoomLevel());
        map.setOnCameraIdleListener(this);
        map.setOnMarkerClickListener(this);
        map.setOnCameraMoveListener(this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        selected = (Coordinates) marker.getTag();
        moveCamera(selected.getLatitude(), selected.getLongitude(), zoomBar.getProgress());
        updateSelected();
        return false;
    }

    private String getBearing(float bearing) {
        if (bearing != 0) {
            if (bearing < 23) return "N";
            if (bearing < 68) return "NE";
            if (bearing < 113) return "E";
            if (bearing < 158) return "SE";
            if (bearing < 203) return "S";
            if (bearing < 248) return "SW";
            if (bearing < 292) return "W";
            if (bearing < 337) return "NW";
            return "N";
        }
        return "";
    }

    @Override
    public void onCameraMove() {
        zoomBar.setProgress((int) map.getCameraPosition().zoom);
        for (Marker marker : markers) {
            if (selected == null) marker.hideInfoWindow();
        }
    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
        Coordinates coordinate = snapshot.getValue(Coordinates.class);
        if (isInChannel(coordinate.getUserId())) {
            if (coordinate.getLatitude() != 0 && coordinate.getLongitude() != 0) {
                Marker marker;
                marker = map.addMarker(new MarkerOptions().position(new LatLng(coordinate.getLatitude(), coordinate.getLongitude())).title(coordinate.getHandle()));
                if (coordinate.getBearing() != 0) {
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.arrow));
                    marker.setRotation(coordinate.getBearing());
                } else
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.star));
                marker.setTag(coordinate);
                marker.setFlat(true);
                markers.add(marker);
            }
        }
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
        Coordinates coordinate = snapshot.getValue(Coordinates.class);
        if (isInChannel(coordinate.getUserId())) {
            for (int i = 0; i < markers.size(); i++) {
                Coordinates current = (Coordinates) markers.get(i).getTag();
                if (current.getUserId().equals(coordinate.getUserId())) {
                    markers.get(i).setPosition(new LatLng(coordinate.getLatitude(), coordinate.getLongitude()));
                    if (coordinate.getBearing() != 0) {
                        markers.get(i).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.arrow));
                        markers.get(i).setRotation(coordinate.getBearing());
                    } else
                        markers.get(i).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.star));
                    markers.get(i).setTag(coordinate);
                }
                if (selected != null) {
                    if (selected.getUserId().equals(coordinate.getUserId())) {
                        selected = coordinate;
                        updateSelected();
                        if (follow)
                            moveCamera(selected.getLatitude(), selected.getLongitude(), map.getCameraPosition().zoom);
                    }
                }
            }
        }
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot snapshot) {
        Coordinates coordinate = snapshot.getValue(Coordinates.class);
        for (int i = 0; i < markers.size(); i++) {
            if (((Coordinates) markers.get(i).getTag()).getUserId().equals(coordinate.getUserId()))
                markers.remove(i);
        }
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {

    }

    static class locationCallback extends LocationCallback {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult != null) {
                for (Location location : locationResult.getLocations()) {
                    RadioService.databaseReference.child("locations").child(RadioService.operator.getUser_id()).setValue(new Coordinates(RadioService.operator.getUser_id(), RadioService.operator.getHandle(), RadioService.operator.getProfileLink(), location.getLatitude(), location.getLongitude(), location.getBearing(), location.getSpeed(), location.getAltitude()));
                }
            }
        }
    }
}
