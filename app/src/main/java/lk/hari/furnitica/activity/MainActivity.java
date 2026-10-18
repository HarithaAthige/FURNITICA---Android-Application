package lk.hari.furnitica.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.hari.furnitica.R;
import lk.hari.furnitica.databinding.ActivityMainBinding;
import lk.hari.furnitica.databinding.SideNavHeaderBinding;
import lk.hari.furnitica.fragment.CartFragment;
import lk.hari.furnitica.fragment.CategoryFragment;
import lk.hari.furnitica.fragment.HomeFragment;
import lk.hari.furnitica.fragment.OrdersFragment;
import lk.hari.furnitica.fragment.ProfileFragment;
import lk.hari.furnitica.model.User;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private SideNavHeaderBinding sideNavHeaderBinding;
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Bind nav header
        sideNavHeaderBinding = SideNavHeaderBinding.bind(binding.sideNavigationView.getHeaderView(0));

        drawerLayout = binding.drawerLayout;
        toolbar = binding.toolbar;
        navigationView = binding.sideNavigationView;
        bottomNavigationView = binding.bottomNavigationView;

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            navigationView.getMenu().findItem(R.id.side_nav_home).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Load user details into nav header
        refreshNavHeader();

        // Hide side nav login and show menu items if user logged in
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            navigationView.getMenu().findItem(R.id.side_nav_login).setVisible(false);
            navigationView.getMenu().findItem(R.id.side_nav_profile).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_orders).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_cart).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_logout).setVisible(true);
        }
    }

    /**
     * Refresh nav header with latest user info (username and email)
     */
    public void refreshNavHeader() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        firebaseFirestore.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(ds -> {
                    if (ds.exists()) {
                        User user = ds.toObject(User.class);
                        if (user != null) {
                            sideNavHeaderBinding.headerUserName.setText(user.getName());
                            sideNavHeaderBinding.headerUserEmail.setText(user.getEmail());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error: " + e.getMessage()));
    }

    /**
     * Update nav header instantly with provided name/email (without waiting Firestore)
     */
    public void updateNavHeaderText(String name, String email) {
        sideNavHeaderBinding.headerUserName.setText(name);
        sideNavHeaderBinding.headerUserEmail.setText(email);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        for (int i = 0; i < navigationView.getMenu().size(); i++)
            navigationView.getMenu().getItem(i).setChecked(false);
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++)
            bottomNavigationView.getMenu().getItem(i).setChecked(false);

        if (itemId == R.id.side_nav_home || itemId == R.id.bottom_nav_home) {
            loadFragment(new HomeFragment());
            navigationView.getMenu().findItem(R.id.side_nav_home).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);

        } else if (itemId == R.id.side_nav_profile || itemId == R.id.bottom_nav_profile) {
            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(this, SignInActivity.class));
                finish();
            } else {
                loadFragment(new ProfileFragment());
                navigationView.getMenu().findItem(R.id.side_nav_profile).setChecked(true);
                bottomNavigationView.getMenu().findItem(R.id.bottom_nav_profile).setChecked(true);
            }

        } else if (itemId == R.id.side_nav_orders) {
            loadFragment(new OrdersFragment());
            navigationView.getMenu().findItem(R.id.side_nav_orders).setChecked(true);


        } else if (itemId == R.id.side_nav_cart || itemId == R.id.bottom_nav_cart) {
            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(this, SignInActivity.class));
                finish();
            } else {
                loadFragment(new CartFragment());
                navigationView.getMenu().findItem(R.id.side_nav_cart).setChecked(true);
                bottomNavigationView.getMenu().findItem(R.id.bottom_nav_cart).setChecked(true);
            }

        }  else if (itemId == R.id.bottom_nav_category) {
            loadFragment(new CategoryFragment());
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_category).setChecked(true);

        } else if (itemId == R.id.side_nav_login) {
            startActivity(new Intent(this, SignInActivity.class));

        } else if (itemId == R.id.side_nav_logout) {
            firebaseAuth.signOut();
            loadFragment(new HomeFragment());
            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.side_nav_menu);
            navigationView.removeHeaderView(sideNavHeaderBinding.getRoot());
            navigationView.inflateHeaderView(R.layout.side_nav_header);
        }

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }

        return true;
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}