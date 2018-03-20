package com.example.kanagaraj.locationfence;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.SnapshotClient;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity {

    // The fence key is how callback code determines which fence fired.
    private final String MOVIING_FENCE_KEY = "moving_fence_key";
    private final String IDLE_FENCE_KEY = "idle_fence_key";

    private final String TAG = getClass().getSimpleName();

    private PendingIntent mPendingIntent;

    private FenceReceiver mFenceReceiver;

    // The intent action which will be fired when your fence is triggered.
    private final String FENCE_RECEIVER_ACTION =
            BuildConfig.APPLICATION_ID + "FENCE_RECEIVER_ACTION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        mFenceReceiver = new FenceReceiver();
        registerReceiver(mFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupFences();
    }

    @Override
    protected void onPause() {
        // Unregister the fence:
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .removeFence(MOVIING_FENCE_KEY)
                .removeFence(IDLE_FENCE_KEY)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Fence was successfully unregistered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Fence could not be unregistered: " + e);
                    }
                });

        super.onPause();
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

    private void setupFences() {

        AwarenessFence movingFence = DetectedActivityFence.during(DetectedActivity.IN_VEHICLE, DetectedActivity.ON_BICYCLE, DetectedActivity.ON_FOOT, DetectedActivity.RUNNING, DetectedActivity.WALKING);
        AwarenessFence idleFence = DetectedActivityFence.during(DetectedActivity.STILL);

        Awareness.getFenceClient(getApplicationContext()).updateFences(new FenceUpdateRequest.Builder()
                .addFence(MOVIING_FENCE_KEY, movingFence, mPendingIntent)
                .addFence(IDLE_FENCE_KEY, idleFence, mPendingIntent)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Fence was successfully registered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Fence could not be registered: " + e);
                    }
                });

        /*Awareness.getSnapshotClient(this).getLocation().addOnSuccessListener(new OnSuccessListener<LocationResponse>() {
            @Override
            public void onSuccess(LocationResponse locationResponse) {
                if (locationResponse != null) {
                    Location location = locationResponse.getLocation();
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    AwarenessFence locationFence = LocationFence.exiting(location.getLatitude(), location.getLongitude(), 50);

                    AwarenessFence headPhoneFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN);


                    // Register the fence to receive callbacks.
                    Awareness.getFenceClient(getApplicationContext()).updateFences(new FenceUpdateRequest.Builder()
                            .addFence(FENCE_KEY, locationFence, mPendingIntent)
                            .addFence("headphoneFenceKey", headPhoneFence, mPendingIntent)
                            .build())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.i(TAG, "Fence was successfully registered.");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "Fence could not be registered: " + e);
                                }
                            });

                }
            }
        });*/
    }


    public class FenceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            if (!TextUtils.equals(FENCE_RECEIVER_ACTION, intent.getAction())) {
                Toast.makeText(context, "Received an unsupported action in FenceReceiver: action="
                        + intent.getAction(), Toast.LENGTH_LONG).show();
                return;
            }

            // The state information for the given fence is em
            FenceState fenceState = FenceState.extract(intent);
            String fenceStateStr = "";
            if (TextUtils.equals(fenceState.getFenceKey(), MOVIING_FENCE_KEY)) {

                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        fenceStateStr = "Moving";
                        break;
                    case FenceState.FALSE:
                        fenceStateStr = "Not Moving";
                        break;
                    case FenceState.UNKNOWN:
                        fenceStateStr = "unknown";
                        break;
                    default:
                        fenceStateStr = "unknown value";
                }
            }

            if (TextUtils.equals(fenceState.getFenceKey(), IDLE_FENCE_KEY)) {
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        fenceStateStr = "Idle";
                        break;
                    case FenceState.FALSE:
                        fenceStateStr = "Not Idle";
                        break;
                    case FenceState.UNKNOWN:
                        fenceStateStr = "unknown";
                        break;
                    default:
                        fenceStateStr = "unknown value";
                }
            }
            Toast.makeText(context, "Fence state: " + fenceStateStr, Toast.LENGTH_LONG).show();
        }
    }

   /* private void showLocation(final Context context) {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Awareness.getSnapshotClient(context).getLocation().addOnSuccessListener(new OnSuccessListener<LocationResponse>() {
            @Override
            public void onSuccess(LocationResponse locationResponse) {
                if (locationResponse != null) {
                    Location location = locationResponse.getLocation();
                    Toast.makeText(context, "Latitude:"+location.getLatitude()+", Longitude:"+location.getLongitude()+", Accuracy:"+location.getAccuracy()+", Provider:"+location.getProvider(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }*/
}




