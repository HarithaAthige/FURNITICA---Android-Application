package lk.hari.furnitica.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lk.hari.furnitica.R;
import lk.hari.furnitica.adapter.ListingAdapter;
import lk.hari.furnitica.databinding.FragmentListingBinding;
import lk.hari.furnitica.model.Product;

public class ListingFragment extends Fragment {


    private FragmentListingBinding binding;
    private ListingAdapter adapter;
    private String categoryId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getString("categoryId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentListingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerViewListing.setLayoutManager(new GridLayoutManager(getContext(), 2));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

//        List<Map<ArrayList, String>> list = new ArrayList<>();
//        list.add();


//        Product p19 = new Product("pid19", "ASTRIDE Ergofit Ergonomic Office Chair", "2D Headrest, Adjustable Arms & Lumbar Support ", 30000, "cat4", Arrays.asList("imageurl1", "imageurl2"), 20, true, 4, null);
//        Product p20 = new Product("pid20", "4 TIER DESK ORGANIZER", "SHELVES SLIDE FROM FRONT", 8500, "cat4", Arrays.asList("imageurl1", "imageurl2"), 10, true, 3, null);
//        Product p21 = new Product("pid21", "Metal Mesh Magazine Holder", "4 Layer Metal Mesh Magazine Holder, Countertop Desk File Organizer 4 Slot| Vertical File Dividers Book Holder & Cabinet Document Storage Rack", 2000, "cat4", Arrays.asList("imageurl1", "imageurl2"), 15, true, 4, null);
//        Product p22 = new Product("pid22", "Auromin Digital Alarm Clock", "Auromin Digital Alarm Clock with Auto Sensor, Time, Date, Temperature Display & Silent Operations", 1500, "cat4", Arrays.asList("imageurl1", "imageurl2"), 15, true, 4, null);
//
//
//        List<Product> list = List.of(p19, p20, p21, p22);
//
//        WriteBatch batch = db.batch();
//
//        for (Product p : list) {
//            DocumentReference ref = db.collection("products").document();
//            batch.set(ref, p);
//        }
//
//        batch.commit();

        db.collection("products")
                .whereEqualTo("categoryId", categoryId)
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(ds -> {
                    if (!ds.isEmpty()) {
                        List<Product> products = ds.toObjects(Product.class);

                        adapter = new ListingAdapter(products, product -> {

                            Bundle bundle = new Bundle();
                            bundle.putString("productId", product.getProductId());

                            ProductDetailsFragment productDetailsFragment = new ProductDetailsFragment();
                            productDetailsFragment.setArguments(bundle);

                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, productDetailsFragment)
                                    .addToBackStack(null)
                                    .commit();

                        });

                        binding.recyclerViewListing.setAdapter(adapter);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Firestore", "Error:" + e.getMessage());
                    }
                });

        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

}
