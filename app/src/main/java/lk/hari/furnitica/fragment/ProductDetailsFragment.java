package lk.hari.furnitica.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.hari.furnitica.R;
import lk.hari.furnitica.activity.MainActivity;
import lk.hari.furnitica.activity.SignInActivity;
import lk.hari.furnitica.adapter.ProductSliderAdapter;
import lk.hari.furnitica.adapter.SectionAdapter;
import lk.hari.furnitica.databinding.FragmentProductDetailsBinding;
import lk.hari.furnitica.model.CartItem;
import lk.hari.furnitica.model.Product;
import lk.hari.furnitica.util.ShakeDetector;

import android.hardware.Sensor;
import android.hardware.SensorManager;

public class ProductDetailsFragment extends Fragment {

    private FragmentProductDetailsBinding binding;
    private String productId;
    private int quantity = 1;
    private int avbQuantity;
    private Map<String, ChipGroup> attributeGroups = new HashMap<>();

    // Sensor fields
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString("productId");
        }

        // Initialize Shake Sensor
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            shakeDetector = new ShakeDetector(() -> {
                Vibrator v = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) v.vibrate(200);

                // Navigate to MainActivity
                startActivity(new Intent(requireActivity(), MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                requireActivity().finish();
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().findViewById(R.id.bottom_navigation_view).setVisibility(View.GONE);

        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Load Product Details
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products")
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(qds -> {
                    if (!qds.isEmpty()) {
                        Product product = qds.getDocuments().get(0).toObject(Product.class);

                        ProductSliderAdapter adapter = new ProductSliderAdapter(product.getImages());
                        binding.productImageSlider.setAdapter(adapter);
                        binding.dotsIndicator.attachTo(binding.productImageSlider);

                        binding.productTitle.setText(product.getTitle());
                        binding.productDetailsDescription.setText(product.getDescription());
                        binding.productDetailsRating.setRating(product.getRating());
                        binding.productDetailsPrice.setText("LKR " + product.getPrice());
                        binding.productDetailsAvbQty.setText(String.valueOf(product.getStockCount()));
                        avbQuantity = product.getStockCount();

                        if (product.getAttributes() != null) {
                            product.getAttributes().forEach(attribute -> renderAttribute(attribute, binding.productDetailsAttributeContainer));
                        }
                    }
                });

        binding.productDetailsBtnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.productDetailsQuantity.setText(String.valueOf(quantity));
            }
        });

        binding.productDetailsBtnPlus.setOnClickListener(v -> {
            if (quantity < avbQuantity) {
                quantity++;
                binding.productDetailsQuantity.setText(String.valueOf(quantity));
            }
        });

        loadTopSellProducts();

        binding.productDetailsBtnAddCart.setOnClickListener(v -> {
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(getActivity(), SignInActivity.class));
            } else {
                List<CartItem.Attribute> attributes = getFinalSelection();
                CartItem cartItem = new CartItem(productId, quantity, attributes);

                String uid = firebaseAuth.getCurrentUser().getUid();
                db.collection("users").document(uid).collection("cart").document(productId)
                        .set(cartItem)
                        .addOnSuccessListener(unused ->
                                Toast.makeText(getContext(), "Item added to cart", Toast.LENGTH_SHORT).show()
                        );
            }
        });
    }

    private void loadTopSellProducts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products")
                .whereNotEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(qds -> {
                    if (!qds.isEmpty()) {
                        List<Product> products = qds.toObjects(Product.class);
                        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                        binding.productDetailsTopSellSection.itemSectionContainer.setLayoutManager(layoutManager);

                        SectionAdapter adapter = new SectionAdapter(products, product -> {
                            Bundle bundle = new Bundle();
                            bundle.putString("productId", product.getProductId());
                            ProductDetailsFragment fragment = new ProductDetailsFragment();
                            fragment.setArguments(bundle);
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        });

                        binding.productDetailsTopSellSection.itemSectionTitle.setText("Top Selling Products");
                        binding.productDetailsTopSellSection.itemSectionContainer.setAdapter(adapter);
                    }
                });
    }

    private void renderAttribute(Product.Attribute attribute, ViewGroup container) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView label = new TextView(getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(5, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        label.setLayoutParams(layoutParams);
        row.addView(label);

        ChipGroup group = new ChipGroup(getContext());
        group.setSelectionRequired(true);
        group.setSingleSelection(true);

        attribute.getValues().forEach(value -> {
            Chip chip = new Chip(getContext());
            chip.setCheckable(true);
            chip.setChipStrokeWidth(2f);
            chip.setTag(value);
            if ("color".equals(attribute.getType())) {
                chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor(value)));
            } else {
                chip.setText(value);
            }
            group.addView(chip);
        });

        row.addView(group);
        container.addView(row);
        attributeGroups.put(attribute.getName(), group);
    }

    private List<CartItem.Attribute> getFinalSelection() {
        List<CartItem.Attribute> attributes = new ArrayList<>();
        for (Map.Entry<String, ChipGroup> entry : attributeGroups.entrySet()) {
            String attributeName = entry.getKey();
            ChipGroup chipGroup = entry.getValue();
            int checkedChipId = chipGroup.getCheckedChipId();
            if (checkedChipId != -1) {
                Chip chip = getView().findViewById(checkedChipId);
                String value = chip.getTag().toString();
                attributes.add(new CartItem.Attribute(attributeName, value));
            }
        }
        return attributes;
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().findViewById(R.id.bottom_navigation_view).setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().findViewById(R.id.bottom_navigation_view).setVisibility(View.GONE);
        if (accelerometer != null) sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(shakeDetector);
    }
}