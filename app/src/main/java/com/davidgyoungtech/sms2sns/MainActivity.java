package com.davidgyoungtech.sms2sns;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT >= 23){
            requestContactPermission();
        }
    }

    private void requestContactPermission() {

        int hasContactPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
        Log.i(TAG, "Requesting SMS permission");

        if(hasContactPermission != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]   {Manifest.permission.RECEIVE_SMS}, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                // Check if the only required permission has been granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "SMS permission has now been granted. Showing result.");
                    Toast.makeText(this,"SMS Permission is Granted",Toast.LENGTH_SHORT).show();
                } else {
                    Log.i(TAG, "SMS permission was NOT granted.");
                }
                break;
        }
    }
}
