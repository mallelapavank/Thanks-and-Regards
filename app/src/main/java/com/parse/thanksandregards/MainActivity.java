package com.parse.thanksandregards;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class MainActivity extends AppCompatActivity {

  private static final String TAG = "MainActivity";
  Switch mySwitch;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mySwitch = (Switch) findViewById(R.id.switch1);

    //hide actionbar
//    getSupportActionBar().hide();

    if(ParseUser.getCurrentUser() == null) {
      ParseAnonymousUtils.logIn(new LogInCallback() {
        @Override
        public void done(ParseUser user, ParseException e) {
          if(e == null) {
            Log.d(TAG, "Anonymous: Login Successful");
          } else {
            Log.d(TAG, "Anonymous: Login Failed");
          }
        }
      });
    } else {
      if(ParseUser.getCurrentUser().get("receiverorprovider") != null ) {
        Log.d(TAG, "Switch: Redirecting as " + ParseUser.getCurrentUser().get("receiverorprovider"));
        redirectActivity();
      }
    }

    ParseAnalytics.trackAppOpenedInBackground(getIntent());
  }

  //Redirect according to user type
  private void redirectActivity() {
    if(ParseUser.getCurrentUser().get("receiverorprovider").equals("receiver")) {
      Intent intent = new Intent(getApplicationContext(), ReceiverActivity.class);
      startActivity(intent);
    } else {
      Intent intent = new Intent(getApplicationContext(), ProviderActivity.class);
      startActivity(intent);
    }
  }

  //On button click
  public void getStarted(View view){

      //set user type
    String userType = "receiver";
    if(mySwitch.isChecked()) {
      userType = "provider";
    }

    //save user type
    ParseUser.getCurrentUser().put("receiverorprovider", userType);
    ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
      @Override
      public void done(ParseException e) {
        redirectActivity();
      }
    });
    Log.d(TAG, "onClick: Redirecting as " + userType);

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
