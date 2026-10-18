package lk.hari.furnitica.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import lk.hari.furnitica.R;
import lk.hari.furnitica.adapter.SectionAdapter;
import lk.hari.furnitica.adapter.SliderAdapter;
import lk.hari.furnitica.databinding.FragmentHomeBinding;
import lk.hari.furnitica.model.Product;
import lk.hari.furnitica.model.SliderItem;
import lk.hari.furnitica.receiver.NetworkChangeReceiver;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private FragmentHomeBinding binding;
    private String productId;
    private ViewPager2 viewPagerSlider;
    private Handler sliderHandler;
    private GoogleMap mMap;

    private NetworkChangeReceiver networkReceiver;


    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPagerSlider != null && viewPagerSlider.getAdapter() != null) {
                int itemCount = viewPagerSlider.getAdapter().getItemCount();
                if (itemCount > 0) {
                    int nextItem = (viewPagerSlider.getCurrentItem() + 1) % itemCount;
                    viewPagerSlider.setCurrentItem(nextItem);
                }
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        // MAP SETUP
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // SLIDER SETUP
        viewPagerSlider = binding.viewPagerSlider;
        sliderHandler = new Handler(Looper.getMainLooper());

        List<SliderItem> sliderItems = new ArrayList<>();
        sliderItems.add(new SliderItem(R.drawable.banner_1));
        sliderItems.add(new SliderItem(R.drawable.banner_2));
        sliderItems.add(new SliderItem(R.drawable.banner_3));

        SliderAdapter sliderAdapter = new SliderAdapter(sliderItems, viewPagerSlider);
        viewPagerSlider.setAdapter(sliderAdapter);

        viewPagerSlider.setClipToPadding(false);
        viewPagerSlider.setClipChildren(false);
        viewPagerSlider.setOffscreenPageLimit(3);

        if (viewPagerSlider.getChildCount() > 0) {
            viewPagerSlider.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        viewPagerSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 3000);
            }
        });

        // BANNER CLICK
        binding.homeBanner.setOnClickListener(v -> {
            // You can navigate to offers page here
        });

        return rootView;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng home = new LatLng(7.2506931, 80.5882114);
        mMap.addMarker(new MarkerOptions().position(home).title("Your Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(home, 15));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        loadTopSellProducts();

        MaterialButton btnCallSupport = view.findViewById(R.id.btnCallSupport);

        btnCallSupport.setOnClickListener(v -> {
            String supportPhone = "+94774250727";
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + supportPhone));
            startActivity(intent);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sliderHandler != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
        }

        if (networkReceiver != null) {
            requireContext().unregisterReceiver(networkReceiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sliderHandler != null && viewPagerSlider != null) {
            sliderHandler.postDelayed(sliderRunnable, 3000);
        }

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
    public void onDestroyView() {
        super.onDestroyView();
        if (sliderHandler != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
        }
        viewPagerSlider = null;
        binding = null;
    }

    private void loadTopSellProducts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products")
                .whereNotEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(qds -> {
                    if (!isAdded() || binding == null || getContext() == null) return;

                    if (qds != null && !qds.isEmpty()) {
                        List<Product> products = qds.toObjects(Product.class);

                        LinearLayoutManager layoutManager =
                                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);

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
}