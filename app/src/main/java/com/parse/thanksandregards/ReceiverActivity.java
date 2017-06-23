package com.parse.thanksandregards;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class ReceiverActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    Button requestButton;
    Boolean requestActive = false;
    Boolean providerActive = false;

    LocationManager locationManager;
    LocationListener locationListener;

    TextView infoTextView;
    android.os.Handler handler = new android.os.Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        requestButton = (Button) findViewById(R.id.buttonRequest);
        infoTextView = (TextView) findViewById(R.id.infoTextView);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null) {
                    if(objects.size() > 0) {
                        requestActive = true;
                        requestButton.setText("Cancel Request");
                        checkForUpdates();
                    }
                }
            }
        });
    }

    public void request(View view) {
        Log.i("Clicked", "Call Food");

        if(requestActive) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e == null) {
                        if(objects.size() > 0) {
                            for(ParseObject object : objects) {
                                object.deleteInBackground();
                            }
                            requestButton.setText("Request Again");
                            requestActive = false;
                        }
                    }
                }
            });
        } else {
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if(lastKnownLocation != null) {
                    ParseObject request = new ParseObject("Requests");
                    request.put("username", ParseUser.getCurrentUser().getUsername());
                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    request.put("location", parseGeoPoint);

                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if(e == null) {
                                requestButton.setText("Cancel Request");
                                requestActive = true;
                                checkForUpdates();
                            }
                        }
                    });
                }
            }
        }
    }

    private void checkForUpdates() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.whereExists("providerUsername");

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects.size() > 0) {
                    providerActive = true;

                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereEqualTo("username", objects.get(0).getString("providerUsername"));

                    query.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> objects, ParseException e) {
                            if(e == null) {
                                ParseGeoPoint providerLocation = objects.get(0).getParseGeoPoint("location");

                                if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(ReceiverActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                    if (lastKnownLocation != null) {
                                        ParseGeoPoint userLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                        Double distanceInKms = providerLocation.distanceInKilometersTo(userLocation);

                                        if (distanceInKms < 0.01) {

                                            infoTextView.setText("Your food has arrived!");

                                            ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
                                            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                                            query.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> objects, ParseException e) {
                                                    if(e == null) {
                                                        for (ParseObject object : objects) {
                                                            object.deleteInBackground();
                                                        }
                                                    }
                                                }
                                            });

                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    infoTextView.setText("");
                                                    requestButton.setText("Request Again");
                                                    requestButton.setVisibility(View.VISIBLE);
                                                    requestActive = false;
                                                    providerActive = false;
                                                }
                                            }, 3000);

                                        } else {

                                            Double distanceOneDP = (double) Math.round(distanceInKms * 10) / 10;
                                            infoTextView.setText("Your food is " + distanceOneDP.toString() + " kms away");

                                            LatLng providerLocationLatLng = new LatLng(providerLocation.getLatitude(), providerLocation.getLongitude());
                                            LatLng requestLocationLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

                                            ArrayList<Marker> markers = new ArrayList<>();
                                            mMap.clear();
                                            markers.add(mMap.addMarker(new MarkerOptions().position(providerLocationLatLng).title("Provider Location")));
                                            markers.add(mMap.addMarker(new MarkerOptions().position(requestLocationLatLng).title("Your Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));

                                            LatLngBounds.Builder latLngBuilder = new LatLngBounds.Builder();
                                            for (Marker marker : markers) {
                                                latLngBuilder.include(marker.getPosition());
                                            }

                                            LatLngBounds bounds = latLngBuilder.build();

                                            int padding = 100; //offset from edges of the map in pixels
                                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                                            mMap.animateCamera(cameraUpdate);

                                            requestButton.setVisibility(View.INVISIBLE);

                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {

                                                    checkForUpdates();

                                                }
                                            }, 3000);

                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    //logout user
    public void logout(View view) {
        ParseUser.logOut();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateMap(lastKnownLocation);
                }
            }
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateMap(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        //request user location permissions
        if (Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            //check if no permission
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //ask permission
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                //have permission
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (android.location.LocationListener) locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    updateMap(lastKnownLocation);
                }
            }
        }
    }

    // Update user's location and move the camera
    private void updateMap(Location location) {
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
    }

}
