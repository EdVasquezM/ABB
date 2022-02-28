package com.sidemvm.ed.abluebook;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**A login screen to register user via email/password.*/

public class AbbStartupActivity extends AppCompatActivity {

    // UI references.
    private ViewGroup viewGroup;
    private AutoCompleteTextView ownerEmailInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abb_startup);
        viewGroup = findViewById(R.id.email_owner_form);
        // Set up the registration.
        ownerEmailInput = findViewById(R.id.email);
        ownerEmailInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                //call getQuizzesOwner when tap on soft key enter
                if (id == EditorInfo.IME_ACTION_DONE) {
                    getEFormOwner();
                    return true;
                } return false;
            }
        });
        Button ownerSignInButton = findViewById(R.id.owner_registration_button);
        ownerSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                getEFormOwner();}});
        //verify if google services is available
        verifyGoogleServices();
        //verify is permissions are granted
        String[] REQUIRED_PERMISSIONS = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        //if owner is ready just launch main activity
        if (askForPermissions(REQUIRED_PERMISSIONS) &&
                getSharedPreferences(getString(R.string.pref_file), MODE_PRIVATE).contains("owner")){
            startActivity(new Intent(this, AbbMainActivity.class));
            finish();
        }
    }

    protected void verifyGoogleServices (){
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS)
            showBSD(R.string.google_needed, false, "");
    }

    private boolean askForPermissions(String... permissions) {
        int permissionGrantedCode = PackageManager.PERMISSION_GRANTED;
        boolean permissionsReady = false;
        //Loop to verify all permissions
        for (final String permission : permissions){
            if (ContextCompat.checkSelfPermission(this, permission) != permissionGrantedCode){
                showBSD(R.string.permission_rationale, true, permission);
                permissionsReady = false;
            } else permissionsReady = true;
        } return permissionsReady;
    }



    /**Attempts register owner. If there are form errors, the errors are presented*/
    private void getEFormOwner() {

        // Reset errors.
        ownerEmailInput.setError(null);

        // Store values at the time of the registration attempt.
        String ownerEmail = ownerEmailInput.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(ownerEmail)) {
            ownerEmailInput.setError(getString(R.string.error_field_required));
            focusView = ownerEmailInput;
            cancel = true;
        } else if (!isEmailValid(ownerEmail)) {
            ownerEmailInput.setError(getString(R.string.error_invalid_email));
            focusView = ownerEmailInput;
            cancel = true;
        }

        if (cancel) {
            // There was an error: focus the first form field with an error.
            focusView.requestFocus();
        } else {
            getSharedPreferences(getString(R.string.pref_file), MODE_PRIVATE)
                    .edit().putString("owner", ownerEmail).apply();
            startActivity(new Intent(this, AbbMainActivity.class));
            finish();
        }
    }

    private boolean isEmailValid(String email) {return email.contains("@");}

    private void showBSD(int dialogText, final boolean isAskingRationale, final String permission) {
        final BottomSheetDialog bsd = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.bsd_simple_msg, viewGroup, false);
        ((TextView) dialogView.findViewById(R.id.dialog_message_view)).setText(dialogText);
        dialogView.findViewById(R.id.dialog_ok_button).setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        if (isAskingRationale)
                            ActivityCompat.requestPermissions(AbbStartupActivity.this,
                                    new String[]{permission}, 1);
                        else try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
                                    ("market://details?id=com.google.android.gms")));
                        } catch (android.content.ActivityNotFoundException exc) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
                                    ("https://play.google.com/store/apps/details?id=" +
                                            "com.google.android.gms")));
                        }
                        bsd.dismiss();
                    }
                });
        bsd.setContentView(dialogView);
        bsd.setCanceledOnTouchOutside(false);
        bsd.setCancelable(false);
        bsd.show();
    }
}
