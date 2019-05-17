package com.example.androtest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsMessage;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        LocationListener {

    class GmailContentObserver extends ContentObserver {

        public GmailContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, (Uri) null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
        }
    }

    public class SmsListener extends BroadcastReceiver {

        private SharedPreferences preferences;

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub

            if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
                Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
                SmsMessage[] msgs = null;
                String msg_from;
                if (bundle != null){
                    //---retrieve the SMS message received---
                    try{
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        msgs = new SmsMessage[pdus.length];
                        for(int i=0; i<msgs.length; i++){
                            msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                            msg_from = msgs[i].getOriginatingAddress();
                            String msgBody = msgs[i].getMessageBody();
                            System.out.println(msgBody);
                        }
                    }catch(Exception e){
//                            Log.d("Exception caught",e.getMessage());
                    }
                }
            }
        }
    }

    private final StringBuilder zip = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            try {
                final String zip = getZipcodeFromCurrentLocation();
                System.out.println(zip);
                final TextView tv = (TextView) findViewById(R.id.adresse);
                tv.setText(zip);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (id == R.id.nav_gallery) {
            GmailContentObserver gco = new GmailContentObserver(new Handler());
            ContentResolver cr = getContentResolver();
            cr.registerContentObserver(Uri.parse("content://gmail-ls"),true,gco);

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_tools) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected String getZipcodeFromCurrentLocation() throws IOException {

        try {
            int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
            if (PackageManager.PERMISSION_GRANTED != permissionCheck) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    requestPermissions(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.INTERNET
                    }, 10);
                }

            }

            final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            final boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            final boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if(true == isGPSEnabled){
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

            }
            else if(true == isNetworkEnabled){
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            }
            else {
                //...
            }

        } catch (SecurityException e){
            e.printStackTrace();
        }

        return zip.toString();

    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            final Double latitude = location.getLatitude();
            final Double longitude = location.getLongitude();
            if(false == Geocoder.isPresent()){
                System.out.println("GEOCODER NOT PRESENT");
                return;
            }

            final Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            final List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            zip.delete(0, zip.length());
            zip.append(addresses.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
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

}
