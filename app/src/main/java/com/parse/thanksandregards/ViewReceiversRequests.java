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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewReceiversRequests extends AppCompatActivity {

    LocationManager locationManager;
    LocationListener locationListener;

    ListView requestListView;
    ArrayList<String> requests = new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    ArrayList<Double> requestLatitude = new ArrayList<Double>();
    ArrayList<Double> requestLongitude = new ArrayList<Double>();
    ArrayList<String> usernames = new ArrayList<String>();

    public void updateListView(Location location) {

        if(location != null) {

            ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
            final ParseGeoPoint geoPointLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            query.whereNear("location", geoPointLocation);
            query.whereDoesNotExist("providerUsername");
            query.setLimit(10);

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        requests.clear();
                        requestLatitude.clear();
                        requestLongitude.clear();
                        if (objects.size() > 0) {
                            for (ParseObject object : objects) {
                                ParseGeoPoint requestLocation = (ParseGeoPoint) object.get("location");
                                if(requestLocation != null) {
                                    Double distanceInKms = geoPointLocation.distanceInKilometersTo(requestLocation);
                                    Double distanceOneDP = (double) Math.round(distanceInKms * 10) / 10;
                                    requests.add(distanceOneDP.toString() + " Kms");

                                    requestLatitude.add(requestLocation.getLatitude());
                                    requestLongitude.add(requestLocation.getLongitude());
                                    usernames.add(object.getString("username"));
                                }
                            }
                        } else {
                            requests.add("No active requests nearby");
                        }

                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            });

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateListView(lastKnownLocation);
                }
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_receivers_requests);

        setTitle("Nearby Requests");

        requestListView = (ListView) findViewById(R.id.requestListView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requests);
        requests.clear();
        requests.add("Getting nearby requests..");
        requestListView.setAdapter(arrayAdapter);

        requestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(ViewReceiversRequests.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (requestLatitude.size() > position && requestLongitude.size() > position && usernames.size() > position && lastKnownLocation != null) {
                        Intent intent = new Intent(getApplicationContext(), ProviderActivity.class);
                        intent.putExtra("requestLatitude", requestLatitude.get(position));
                        intent.putExtra("requestLongitude", requestLongitude.get(position));
                        intent.putExtra("providerLatitude", lastKnownLocation.getLatitude());
                        intent.putExtra("providerLongitude", lastKnownLocation.getLongitude());
                        intent.putExtra("username", usernames.get(position));
                        startActivity(intent);
                    }else{
                        Log.i("Redirect ProviderMap", "No Last Known Location");
                    }
                }else{
                    Log.i("Permission", "No perm for Provider");
                }
            }
        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateListView(location);
                //save provider's location
                ParseUser.getCurrentUser().put("location", new ParseGeoPoint(location.getLatitude(), location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();
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

        if(Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            //check if no permission
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //ask permission
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                //have permission
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(lastKnownLocation != null) {
                    updateListView(lastKnownLocation);
                }
            }
        }

    }
}
