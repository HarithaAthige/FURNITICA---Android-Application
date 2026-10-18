package lk.hari.furnitica.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.hari.furnitica.databinding.ActivitySignInBinding;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;

    private TextView emailInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignInBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        this.emailInput = binding.signinInputEmail;


        firebaseAuth = FirebaseAuth.getInstance();

        binding.signinBtnSignup.setOnClickListener(view -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
            finish();
        });

        binding.signinForgotPassword.setOnClickListener(v -> {
            forgotPasswordProcess();
        });

        binding.signinBtnSignin.setOnClickListener(view -> {

            String email = binding.signinInputEmail.getText().toString().trim();
            String password = binding.signinInputPassword.getText().toString().trim();

            if (email.isEmpty()) {
                binding.signinInputEmail.setError("Email is required!");
                binding.signinInputEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.signinInputEmail.setError("Enter valid email!");
                binding.signinInputEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                binding.signinInputPassword.setError("Password is required!");
                binding.signinInputPassword.requestFocus();
                return;
            }

            if (password.length() < 6) {
                binding.signinInputPassword.setError("Password Must Be 6 Character");
                binding.signinInputPassword.requestFocus();
                return;
            }

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                updateUI(firebaseAuth.getCurrentUser());
                            } else {
                                Toast.makeText(SignInActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                                //updateUI(null);
                            }
                        }
                    });
        });


    }

    private void forgotPasswordProcess() {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        String email = this.emailInput.getText().toString();

        if (email.isBlank()) {
            emailInput.setError("Email is Required");
            emailInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Invalid Email");
            emailInput.requestFocus();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Password reset email sent",
                                Toast.LENGTH_SHORT).show();

                        new AlertDialog.Builder(this)
                                .setTitle("Open Email")
                                .setMessage("Do you want to open the email app?")
                                .setPositiveButton("Open", (dialog, which) -> {
                                    Intent intent = new Intent(Intent.ACTION_MAIN);
                                    intent.addCategory(Intent.CATEGORY_APP_EMAIL);
                                    try {
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e) {
                                        Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();

                    } else {
                        Log.w("LoginActivity", task.getException());
                        Toast.makeText(this,
                                "Something went wrong",
                                Toast.LENGTH_SHORT).show();
                    }

                });
    }

    private void updateUI(FirebaseUser user) {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}