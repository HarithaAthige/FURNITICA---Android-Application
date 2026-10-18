package lk.hari.furnitica.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

import lk.hari.furnitica.R;
import lk.hari.furnitica.adapter.CategoryAdapter;
import lk.hari.furnitica.databinding.FragmentCategoryBinding;
import lk.hari.furnitica.model.Category;


public class CategoryFragment extends Fragment {


    private FragmentCategoryBinding binding;
    private CategoryAdapter adapter;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

//        Category c1 = new Category("cat1", "Living Room Furniture", "");
//        Category c2 = new Category("cat2", "Bedroom Furniture", "");
//        Category c3 = new Category("cat3", "Dining Room Furniture", "");
//        Category c4 = new Category("cat4", "Office Furniture", "");
//        Category c5 = new Category("cat5", "Home Decor & Accessories", "");
//
//
//        List<Category> cats = List.of(c1, c2, c3, c4, c5);
//
//        WriteBatch batch = db.batch();
//
//        for (Category c : cats) {
//            DocumentReference ref = db.collection("categories").document();
//            batch.set(ref, c);
//        }
//
//        batch.commit();


        db.collection("categories").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        //QuerySnapshot result = task.getResult();
                        //List<Category> categories = result.toObjects(Category.class);

                        List<Category> categories = task.getResult().toObjects(Category.class);
                        adapter = new CategoryAdapter(categories, category -> {

                            Bundle bundle = new Bundle();
                            bundle.putString("categoryId", category.getCategoryId());

                            ListingFragment fragment = new ListingFragment();
                            fragment.setArguments(bundle);

                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, fragment)
                                    .addToBackStack(null)
                                    .commit();

                        });

                        binding.recyclerViewCategories.setAdapter(adapter);
                    }
                });


    }
}