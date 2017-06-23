package com.parse.thanksandregards;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseUser;


public class StarterApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    // Enable Local Datastore.
    Parse.enableLocalDatastore(this);

    // Add initialization code here
    Parse.initialize(new Parse.Configuration.Builder(getApplicationContext())
            .applicationId("PNXXaUkgNk6pvguRjEBSZNWQK2wHu8WIC2HMNi5p")
            .clientKey("LBz4HwUjaO7psZ1YYbEYdvMzTihdoJTyZlZzFzrq")
            .server("https://parseapi.back4app.com/")
    .build()
    );

    ParseUser.enableAutomaticUser();
    ParseACL defaultACL = new ParseACL();
    ParseACL.setDefaultACL(defaultACL, true);
  }
}
