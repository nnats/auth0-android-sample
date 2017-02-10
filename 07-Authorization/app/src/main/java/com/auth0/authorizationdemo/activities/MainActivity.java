package com.auth0.authorizationdemo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.management.ManagementException;
import com.auth0.android.management.UsersAPIClient;
import com.auth0.android.result.UserProfile;
import com.auth0.authorizationdemo.R;
import com.auth0.authorizationdemo.utils.CredentialsManager;
import com.squareup.picasso.Picasso;


public class MainActivity extends AppCompatActivity {

    private UserProfile mUserProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button loginAgainButton = (Button) findViewById(R.id.login_again);
        loginAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginAgain();
            }
        });

        final Auth0 auth0 = new Auth0(getString(R.string.auth0_client_id), getString(R.string.auth0_domain));
        auth0.setOIDCConformant(true);
        AuthenticationAPIClient authenticationClient = new AuthenticationAPIClient(auth0);
        authenticationClient.userInfo(CredentialsManager.getCredentials(this).getAccessToken())
                .start(new BaseCallback<UserProfile, AuthenticationException>() {
                    @Override
                    public void onSuccess(final UserProfile info) {
                        String userId = (String) info.getExtraInfo().get("sub");
                        UsersAPIClient usersClient = new UsersAPIClient(auth0, CredentialsManager.getCredentials(MainActivity.this).getIdToken());
                        usersClient.getProfile(userId).start(new BaseCallback<UserProfile, ManagementException>() {
                            @Override
                            public void onSuccess(final UserProfile profile) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        mUserProfile = profile;
                                        ((TextView) findViewById(R.id.userName)).setText(mUserProfile.getName());
                                        ((TextView) findViewById(R.id.userEmail)).setText(mUserProfile.getEmail());
                                        ImageView userPicture = (ImageView) findViewById(R.id.userPicture);
                                        Picasso.with(MainActivity.this).load(mUserProfile.getPictureURL()).into(userPicture);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(ManagementException error) {
                            }
                        });
                    }

                    @Override
                    public void onFailure(AuthenticationException error) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "Failed to load the profile", Toast.LENGTH_SHORT).show();
                            }
                        });
                        finish();
                    }
                });

        Button showSettingsButton = (Button) findViewById(R.id.toSettingsButton);
        showSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });

    }

    private void showSettings() {
        String[] roles = (String[]) mUserProfile.getAppMetadata().get("roles");
        if (roles == null) {
            Toast.makeText(MainActivity.this, "Missing roles from the Profile. Check the rules in the dashboard.", Toast.LENGTH_LONG).show();
            return;
        }

        for (String role : roles) {
            if (role.contains("admin")) {
                startActivity(new Intent(this, SettingsActivity.class));
                return;
            }
        }

        Toast.makeText(MainActivity.this, "You don't have access rights to visit this page", Toast.LENGTH_SHORT).show();
    }

    private void loginAgain() {
        startActivity(new Intent(this, LoginActivity.class));
        CredentialsManager.deleteCredentials(this);
        finish();
    }
}
