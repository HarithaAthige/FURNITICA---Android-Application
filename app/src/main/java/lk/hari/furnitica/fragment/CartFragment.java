package lk.hari.furnitica.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lk.hari.furnitica.R;
import lk.hari.furnitica.activity.MainActivity;
import lk.hari.furnitica.adapter.CartAdapter;
import lk.hari.furnitica.databinding.FragmentCartBinding;
import lk.hari.furnitica.model.CartItem;
import lk.hari.furnitica.model.Product;
import lk.hari.furnitica.receiver.NetworkChangeReceiver;
import lk.hari.furnitica.util.ShakeDetector;
import lk.hari.furnitica.worker.AbandonedCartWorker;

import android.hardware.Sensor;
import android.hardware.SensorManager;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private List<CartItem> cartItems;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;

    private NetworkChangeReceiver networkReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            shakeDetector = new ShakeDetector(() -> {
                Vibrator v = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) v.vibrate(200);
                startActivity(new Intent(requireActivity(), MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                requireActivity().finish();
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ✅ FIX: Initialize RecyclerView BEFORE Firebase call
        cartItems = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.cartCartItems.setLayoutManager(layoutManager);

        CartAdapter adapter = new CartAdapter(cartItems);
        binding.cartCartItems.setAdapter(adapter);

        if (firebaseAuth.getCurrentUser() != null) {

            String uid = firebaseAuth.getCurrentUser().getUid();

            db.collection("users").document(uid).collection("cart")
                    .get()
                    .addOnSuccessListener(qds -> {

                        if (!qds.isEmpty()) {

                            binding.cartEmptyImage.setVisibility(View.GONE);
                            binding.cartCartItems.setVisibility(View.VISIBLE);

                            // ✅ FIX: Do NOT reinitialize list
                            cartItems.clear();

                            for (DocumentSnapshot ds : qds.getDocuments()) {
                                CartItem cartItem = ds.toObject(CartItem.class);
                                if (cartItem != null) {
                                    String documentId = ds.getId();
                                    cartItem.setDocumentId(documentId);
                                    cartItems.add(cartItem);
                                }
                            }

                            adapter.notifyDataSetChanged();

                            adapter.setOnQuantityChangeListener(cartItem -> {
                                String documentId = cartItem.getDocumentId();
                                db.collection("users").document(uid)
                                        .collection("cart").document(documentId)
                                        .update("quantity", cartItem.getQuantity())
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), "Item quantity has been updated!", Toast.LENGTH_SHORT).show();
                                        });

                                updateTotal();
                                scheduleAbandonedCartNotification();
                            });

                            adapter.setOnRemoveListener(position -> {
                                String documentId = cartItems.get(position).getDocumentId();
                                db.collection("users").document(uid)
                                        .collection("cart").document(documentId)
                                        .delete().addOnSuccessListener(aVoid -> {

                                            cartItems.remove(position);
                                            adapter.notifyItemRemoved(position);
                                            adapter.notifyItemRangeChanged(position, cartItems.size());
                                            updateTotal();

                                            Toast.makeText(getContext(), "Item has been removed!", Toast.LENGTH_SHORT).show();

                                            if (cartItems.isEmpty()) {
                                                binding.cartEmptyImage.setVisibility(View.VISIBLE);
                                                binding.cartCartItems.setVisibility(View.GONE);
                                            }

                                            scheduleAbandonedCartNotification();
                                        });
                            });

                            updateTotal();

                        } else {
                            binding.cartEmptyImage.setVisibility(View.VISIBLE);
                            binding.cartCartItems.setVisibility(View.GONE);
                        }
                    });
        }

        // ✅ FIX: Navigation added
        binding.cartBtnProceed.setOnClickListener(v -> {

            if (cartItems == null || cartItems.isEmpty()) {
                Toast.makeText(getContext(), "Your cart is empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CheckoutFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void updateTotal() {
        if (cartItems == null || cartItems.isEmpty()) {
            binding.cartTextTotal.setText(String.format(Locale.US, "LKR%,.2f", 0.00));
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<String> productIds = new ArrayList<>();
        cartItems.forEach(cartItem -> productIds.add(cartItem.getProductId()));

        db.collection("products").whereIn("productId", productIds)
                .get()
                .addOnSuccessListener(qds -> {
                    Map<String, Product> productMap = new HashMap<>();
                    qds.getDocuments().forEach(ds -> {
                        Product product = ds.toObject(Product.class);
                        if (product != null) productMap.put(product.getProductId(), product);
                    });

                    double total = 0;
                    for (CartItem cartItem : cartItems) {
                        Product product = productMap.get(cartItem.getProductId());
                        if (product != null) total += product.getPrice() * cartItem.getQuantity();
                    }

                    binding.cartTextTotal.setText(String.format(Locale.US, "LKR %,.2f", total));
                });
    }

    private void scheduleAbandonedCartNotification() {
        Constraints constraints = new Constraints.Builder().build();

        OneTimeWorkRequest abandonedCartWork = new OneTimeWorkRequest.Builder(AbandonedCartWorker.class)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(requireContext())
                .enqueueUniqueWork(
                        "abandoned_cart_notification",
                        ExistingWorkPolicy.REPLACE,
                        abandonedCartWork
                );
    }

    @Override
    public void onResume() {
        super.onResume();
        if (accelerometer != null)
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);

        networkReceiver = new NetworkChangeReceiver(isConnected -> {
            if (isConnected) {
                Toast.makeText(getContext(), "Back Online", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
            }
        });
        requireContext().registerReceiver(
                networkReceiver,
                new android.content.IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION)
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(shakeDetector);

        if (networkReceiver != null) {
            requireContext().unregisterReceiver(networkReceiver);
        }
    }
}