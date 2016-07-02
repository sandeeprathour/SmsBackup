package com.gmail.sandeeprathour.smsbackup;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private static final String TAG = "MainActivity";

    private static final int RC_SIGN_IN = 100;

    private GoogleApiClient mGoogleApiClient;

    private EditText valueToSave;
    private TextView valueFromDb;
    private Button saveToDbButton;
    private Button fetchFromDbButton;
    private Button readSmsesButton;

    DatabaseReference myRef;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        firebaseAuth = FirebaseAuth.getInstance();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        findViewById(R.id.sign_in_button).setOnClickListener(this);

        valueToSave = (EditText) findViewById(R.id.value_to_save);
        valueFromDb = (TextView) findViewById(R.id.value_from_datastore);
        saveToDbButton = (Button) findViewById(R.id.save_to_db);
        fetchFromDbButton = (Button) findViewById(R.id.fetch_from_db);
        readSmsesButton = (Button) findViewById(R.id.read_smses);


        readSmsesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get sim details
                getSimDetails();

                // Get smses
                Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

                if (cursor.moveToFirst()) { // must check the result to prevent exception
                    do {
                        String msgData = "";
                        for(int idx=0;idx<cursor.getColumnCount();idx++)
                        {
                            msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                        }
                        // use msgData
                        Log.d(TAG, msgData);
                    } while (cursor.moveToNext());
                } else {
                    // empty box, no SMS
                }
            }
        });
    }

    private void readSmses() {
        // Get sim details
        getSimDetails();

        // Get smses
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                String msgData = "";
                for(int idx=0;idx<cursor.getColumnCount();idx++)
                {
                    msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                }
                // use msgData
                Log.d(TAG, msgData);
            } while (cursor.moveToNext());
        } else {
            // empty box, no SMS
        }
    }
    private void getSimDetails() {
        TelephonyManager telemamanger = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String getSimSerialNumber = telemamanger.getSimSerialNumber();
        Log.d(TAG, "Sim serial number is " + getSimSerialNumber);
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

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Unable to connect to Google API Client");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sign_in_button) {
            signIn();
        }
    }

    private final void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
                handleSignInResult(result);


                myRef.child("users").child(firebaseAuth.getCurrentUser().getUid()).child("messages").addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Log.d(TAG, "Child added " + s);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        Log.d(TAG, "Child changed " + s);
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        Log.d(TAG, "Child removed " + dataSnapshot.getValue());
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                        Log.d(TAG, "Child moved " + dataSnapshot.getValue());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


                saveToDbButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DatabaseReference messageNode = myRef.child("user").child(firebaseAuth.getCurrentUser().getUid()).child("messages");

                        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

                        if (cursor.moveToFirst()) { // must check the result to prevent exception
                            do {
                                String msgData = "";

                                DatabaseReference msgNode = messageNode.child("sms_" + cursor.getString(cursor.getColumnIndex("_id")));
                                for(int idx=0;idx<cursor.getColumnCount();idx++)
                                {
                                    msgNode.child(cursor.getColumnName(idx)).setValue(cursor.getString(idx));
                                    msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                                }
                                // use msgData
                                Log.d(TAG, msgData);
                            } while (cursor.moveToNext());
                        } else {
                            // empty box, no SMS
                        }
                    }
                });

            } else {
                // Google Sign In failed, update UI appropriately
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        // [START_EXCLUDE silent]
//        showProgressDialog();
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        // [START_EXCLUDE]
//                        hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess() + result.getStatus());
        Log.d(TAG, "Current logged in user " + FirebaseAuth.getInstance().getCurrentUser());
    }

}
