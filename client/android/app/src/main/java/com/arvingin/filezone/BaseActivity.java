package com.arvingin.filezone;

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.Arrays;

public class BaseActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    private RequestPermissionResultListener requestPermissionResultListener;

    public void requestPermissions(RequestPermissionResultListener l, String... permissions) {
        requestPermissionResultListener = l;

        String[] waitingPermissions = new String[permissions.length];
        int waitingPermissionCount = 0;

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {

                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    waitingPermissions[waitingPermissionCount] = permission;
                    waitingPermissionCount++;
                }
            }
        }

        if (0 == waitingPermissionCount) {
            if (null != l) l.onResult(true);
        } else {
            waitingPermissions = Arrays.copyOf(waitingPermissions, waitingPermissionCount);
            ActivityCompat.requestPermissions(this, waitingPermissions, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS:
                boolean result = true;

                for (Integer grantResult : grantResults) {
                    if (1 == grantResult) {
                        result = false;
                        break;
                    }
                }

                if (null != requestPermissionResultListener)
                    requestPermissionResultListener.onResult(result);

                break;
        }
    }
}
