package lk.hari.furnitica.fragment;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import lk.hari.furnitica.R;
import lk.hari.furnitica.activity.MainActivity;
import lk.hari.furnitica.activity.SignInActivity;
import lk.hari.furnitica.model.User;

public class ProfileFragment extends Fragment implements SensorEventListener {

    private CircleImageView imageProfile;
    private MaterialButton btnChangeImage, btnSaveChanges, btnLogout;
    private TextInputEditText etName, etEmail;
    private TextView tvForgotPassword;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    private Uri selectedImageUri;
    private String currentProfileImageUrl = "";

    private ActivityResultLauncher<String> imagePickerLauncher;

    // Accelerometer fields
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD = 12.0f;
    private long lastShakeTime = 0;

    public ProfileFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        if (imageProfile != null) {
                            Glide.with(requireContext())
                                    .load(selectedImageUri)
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .error(R.drawable.ic_profile_placeholder)
                                    .into(imageProfile);
                        }
                    }
                }
        );

        // Initialize accelerometer
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if(sensorManager != null){
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(accelerometer != null){
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        imageProfile = view.findViewById(R.id.imageProfile);
        btnChangeImage = view.findViewById(R.id.btnChangeImage);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        btnLogout = view.findViewById(R.id.btnLogout);
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            goToSignIn();
            return view;
        }

        loadUserProfile();

        btnChangeImage.setOnClickListener(v -> openImagePicker());
        btnSaveChanges.setOnClickListener(v -> validateAndSaveChanges());
        tvForgotPassword.setOnClickListener(v -> forgotPasswordProcess());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            goToSignIn();
        });

        return view;
    }

    private void loadUserProfile() {
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            etName.setText(user.getName() != null ? user.getName() : "");
                            etEmail.setText(user.getEmail() != null ? user.getEmail() : currentUser.getEmail());
                            currentProfileImageUrl = user.getProfilePicUrl();
                            loadProfileImageFromUrl(currentProfileImageUrl);
                        }
                    } else {
                        etName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "");
                        etEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
                        if (currentUser.getPhotoUrl() != null) {
                            currentProfileImageUrl = currentUser.getPhotoUrl().toString();
                            loadProfileImageFromUrl(currentProfileImageUrl);
                        } else {
                            imageProfile.setImageResource(R.drawable.ic_profile_placeholder);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadProfileImageFromUrl(String imageUrl) {
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(imageProfile);
        } else {
            imageProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void validateAndSaveChanges() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }

        btnSaveChanges.setEnabled(false);

        if (selectedImageUri != null) {
            uploadImageAndSave(name, email);
        } else {
            updateUserProfile(name, email, currentProfileImageUrl);
        }
    }

    private void uploadImageAndSave(String name, String email) {
        String uid = currentUser.getUid();
        StorageReference fileRef = storage.getReference().child("profile_images").child(uid + ".jpg");

        fileRef.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return fileRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    currentProfileImageUrl = uri.toString();
                    updateUserProfile(name, email, currentProfileImageUrl);
                    saveImageLocally(selectedImageUri);
                })
                .addOnFailureListener(e -> {
                    btnSaveChanges.setEnabled(true);
                    Toast.makeText(requireContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveImageLocally(Uri uri) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(requireContext().getContentResolver().openInputStream(uri));
            File file = new File(requireContext().getFilesDir(), "profile_image.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
        } catch (IOException e) {
            Log.e("ProfileFragment", "Failed to save image locally: " + e.getMessage());
        }
    }

    private void updateUserProfile(String name, String email, String profileImageUrl) {
        UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder().setDisplayName(name);
        if (!TextUtils.isEmpty(profileImageUrl)) builder.setPhotoUri(Uri.parse(profileImageUrl));

        currentUser.updateProfile(builder.build())
                .addOnSuccessListener(unused -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("uid", currentUser.getUid());
                    userMap.put("name", name);
                    userMap.put("email", email);
                    userMap.put("profilePicUrl", profileImageUrl);

                    db.collection("users").document(currentUser.getUid())
                            .set(userMap)
                            .addOnSuccessListener(unused1 -> {
                                btnSaveChanges.setEnabled(true);
                                loadProfileImageFromUrl(profileImageUrl);
                                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).refreshNavHeader();
                                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                btnSaveChanges.setEnabled(true);
                                Toast.makeText(requireContext(), "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnSaveChanges.setEnabled(true);
                    Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        if (!email.equals(currentUser.getEmail())) {
            currentUser.updateEmail(email)
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Email update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void forgotPasswordProcess() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (email.isEmpty()) {
            etEmail.setError("Email is Required");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid Email");
            etEmail.requestFocus();
            return;
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Password reset email sent", Toast.LENGTH_SHORT).show();
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Open Email")
                                .setMessage("Do you want to open the email app?")
                                .setPositiveButton("Open", (dialog, which) -> {
                                    Intent intent = new Intent(Intent.ACTION_MAIN);
                                    intent.addCategory(Intent.CATEGORY_APP_EMAIL);
                                    try {
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e) {
                                        Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        Log.w("ProfileFragment", String.valueOf(task.getException()));
                        Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToSignIn() {
        Intent intent = new Intent(requireActivity(), SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    // ----------------- Shake detection -----------------
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH;

            if(acceleration > SHAKE_THRESHOLD){
                long currentTime = System.currentTimeMillis();
                if(currentTime - lastShakeTime > 1000){ // 1 sec cooldown
                    lastShakeTime = currentTime;
                    triggerShakeAction();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void triggerShakeAction(){
        // Vibrate as feedback
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if(vibrator != null) vibrator.vibrate(100);

        // Navigate to MainActivity
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}