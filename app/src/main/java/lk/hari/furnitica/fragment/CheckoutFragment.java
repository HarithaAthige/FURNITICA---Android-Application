package lk.hari.furnitica.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.hari.furnitica.R;
import lk.hari.furnitica.databinding.FragmentCheckoutBinding;
import lk.hari.furnitica.listener.FirestoreCallback;
import lk.hari.furnitica.model.CartItem;
import lk.hari.furnitica.model.Order;
import lk.hari.furnitica.model.Product;
import lk.hari.furnitica.receiver.NetworkChangeReceiver;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

public class CheckoutFragment extends Fragment {
    private FragmentCheckoutBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private double total;
    private boolean paymentActive;

    private NetworkChangeReceiver networkReceiver;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);


        binding.shippingLayoutBtn.setOnClickListener(v -> {

            if (binding.shippingLayoutBody.getVisibility() == View.GONE) {
                binding.shippingLayoutBody.setVisibility(View.VISIBLE);
                binding.shippingLayoutBtn.setRotation(180f);
            } else {
                binding.shippingLayoutBody.setVisibility(View.GONE);
                binding.shippingLayoutBtn.setRotation(0f);
            }
        });

        binding.billingLayoutBtn.setOnClickListener(v -> {
            if (binding.billingLayoutBody.getVisibility() == View.GONE) {
                binding.billingLayoutBody.setVisibility(View.VISIBLE);
                binding.billingLayoutBtn.setRotation(180f);
            } else {
                binding.billingLayoutBody.setVisibility(View.GONE);
                binding.billingLayoutBtn.setRotation(0f);
            }
        });


        binding.shippingDetailsCheckBilling.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.billingLayout.setVisibility(View.GONE);
            } else {
                binding.billingLayout.setVisibility(View.VISIBLE);
                binding.billingLayoutBody.setVisibility(View.VISIBLE);
            }
        });


        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        double shippingCost = 350;

        getCartItems(cartItems -> {

            ArrayList<String> productIds = new ArrayList<>();
            cartItems.forEach(cartItem -> {
                productIds.add(cartItem.getProductId());
            });


            getProductsByIds(productIds, data -> {

                double subTotal = 0;

                for (CartItem cartItem : cartItems) {
                    Product product = data.get(cartItem.getProductId());
                    if (product != null) {
                        subTotal += product.getPrice() * cartItem.getQuantity();
                    }
                }

                total = subTotal + shippingCost;
                binding.checkoutSubTotal.setText(String.format(Locale.US, "LKR %,.2f", subTotal));
                binding.checkoutShipping.setText(String.format(Locale.US, "LKR %,.2f", shippingCost));
                binding.checkoutTotal.setText(String.format(Locale.US, "LKR %,.2f", total));
                paymentActive = true;

            });

        });


        String uid = firebaseAuth.getCurrentUser().getUid();

        binding.checkoutBtnProceed.setOnClickListener(v -> {

            if (paymentActive) {
                InitRequest req = new InitRequest();
                req.setSandBox(true);

                req.setMerchantId("1231547");
                req.setMerchantSecret("MTkxNjExNzg2MjE1NDE3ODg2ODEyOTE2MTY5ODE2NDEyNjExNTc5");
                req.setCurrency("LKR");
                req.setAmount(100.00);
                req.setOrderId("FURNI-001");
                req.setItemsDescription("");

                req.getCustomer().setFirstName("Haritha");
                req.getCustomer().setLastName("Athige");
                req.getCustomer().setEmail("haritha@gmail.com");
                req.getCustomer().setPhone("+94771234567");
                req.getCustomer().getAddress().setAddress("No.1, Colombo Road");
                req.getCustomer().getAddress().setCity("Kandy");
                req.getCustomer().getAddress().setCountry("Sri Lanka");

                req.setNotifyUrl("https://furnitica.requestcatcher.com/");

                Intent intent = new Intent(getActivity(), PHMainActivity.class);
                intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);

                payhereLauncher.launch(intent);

            }


        });

    }

    private void getCartItems(FirestoreCallback<List<CartItem>> callback) {
        String uid = firebaseAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).collection("cart")
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {
                        if (!qds.isEmpty()) {
                            List<CartItem> cartItems = qds.toObjects(CartItem.class);
                            callback.onCallback(cartItems);
                        }
                    }
                });
    }

    private void getProductsByIds(List<String> productIds, FirestoreCallback<Map<String, Product>> callback) {

        Map<String, Product> products = new HashMap<>();

        if (productIds == null || productIds.isEmpty()) {
            callback.onCallback(products);
            return;
        }

        db.collection("products")
                .whereIn("productId", productIds)
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {

                        qds.getDocuments().forEach(ds -> {
                            Product product = ds.toObject(Product.class);
                            if (product != null) {
                                products.put(product.getProductId(), product);
                            }
                        });

                        callback.onCallback(products);
                    }
                });

    }

    @Override
    public void onResume() {
        super.onResume();

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

        if (networkReceiver != null) {
            requireContext().unregisterReceiver(networkReceiver);
        }
    }

    /// /    private void getSubTotal() {
    /// /
    /// /        List<CartItem> cartItems = getCartItems();
    /// /
    /// /        List<String> productIds = new ArrayList<>();
    /// /        cartItems.forEach(cartItem -> {
    /// /            productIds.add(cartItem.getProductId());
    /// /        });
    /// /
    /// /        Map<String, Product> products = getProductsByIds(productIds);
    /// /
    /// /        double subTotal = 0;
    /// /        for (CartItem cartItem : cartItems) {
    /// /            Product product = products.get(cartItem.getProductId());
    /// /            if (product != null) {
    /// /                subTotal += product.getPrice() * cartItem.getQuantity();
    /// /            }
    /// /        }
    /// /
    /// /        return subTotal;
    /// /
    /// /    }
//
    private final ActivityResultLauncher<Intent> payhereLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                        PHResponse<StatusResponse> response =
                                (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                        if (response != null && response.isSuccess()) {

                            StatusResponse statusResponse = response.getData();

                            //Save order to firestore
                            saveOrder(statusResponse);

                            Log.i("PAYHERE", "Payment Success!");

                        } else {
                            Log.e("PAYHERE", response != null ? response.toString() : "Payment Failed");
                        }

                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Log.e("PAYHERE", "Payment Canceled!");
                }

            });

    private void saveOrder(StatusResponse statusResponse) {
        getCartItems(cartItems -> {

            String uid = firebaseAuth.getCurrentUser().getUid();

            Order order = new Order();
            order.setOrderId(String.valueOf(System.currentTimeMillis()));
            order.setUserId(uid);
            order.setTotalAmount(total);
            order.setStatus("PAID");
            order.setOrderDate(Timestamp.now().toDate().getTime());


            String shipping_name = binding.shippingDetailsName.getText().toString();
            String shipping_email = binding.shippingDetailsEmail.getText().toString();
            String shipping_contact = binding.shippingDetailsContact.getText().toString();
            String shipping_address1 = binding.shippingDetailsAddress1.getText().toString();
            String shipping_address2 = binding.shippingDetailsAddress2.getText().toString();
            String shipping_city = binding.shippingDetailsCity.getText().toString();
            String shipping_postCode = binding.shippingDetailsPostcode.getText().toString();

            Order.Address shippingAddress = Order.Address.builder().name(shipping_name).email(shipping_email).contact(shipping_contact).address1(shipping_address1).address2(shipping_address2).city(shipping_city).postcode(shipping_postCode).build();

            order.setShippingAddress(shippingAddress);

            if (!binding.shippingDetailsCheckBilling.isChecked()) {
                String billing_name = binding.billingDetailsName.getText().toString();
                String billing_email = binding.billingDetailsEmail.getText().toString();
                String billing_contact = binding.billingDetailsContact.getText().toString();
                String billing_address1 = binding.billingDetailsAddress1.getText().toString();
                String billing_address2 = binding.billingDetailsAddress2.getText().toString();
                String billing_city = binding.billingDetailsCity.getText().toString();
                String billing_postCode = binding.billingDetailsPostcode.getText().toString();

                Order.Address billingAddress = Order.Address.builder().name(billing_name).email(billing_email).contact(billing_contact).address1(billing_address1).address2(billing_address2).city(billing_city).postcode(billing_postCode).build();

                order.setBillingAddress(billingAddress);
            }

            /// ///
            ArrayList<String> productIds = new ArrayList<>();
            cartItems.forEach(cartItem -> {
                productIds.add(cartItem.getProductId());
            });

            List<Order.OrderItem> orderItems = new ArrayList<>();

            getProductsByIds(productIds, data -> {

                for (CartItem cartItem : cartItems) {

                    Product product = data.get(cartItem.getProductId());

                    if (product != null) {

                        List<Order.OrderItem.Attribute> attributes = new ArrayList<>();

                        for (CartItem.Attribute at : cartItem.getAttributes()) {
                            Order.OrderItem.Attribute attribute =
                                    Order.OrderItem.Attribute.builder()
                                            .name(at.getName())
                                            .value(at.getValue())
                                            .build();

                            attributes.add(attribute);
                        }

                        Order.OrderItem orderItem = Order.OrderItem.builder()
                                .productId(cartItem.getProductId())
                                .unitPrice(product.getPrice())
                                .quantity(cartItem.getQuantity())
                                .attributes(attributes)
                                .build();

                        orderItems.add(orderItem);
                    }
                }


                order.setOrderItems(orderItems);

                db.collection("orders").document(order.getOrderId())
                        .set(order)
                        .addOnSuccessListener(aVoid -> {

                            Toast.makeText(getContext(), "Payment Success & Order Saved!", Toast.LENGTH_LONG).show();

                            // clear cart
                            db.collection("users").document(uid).collection("cart")
                                    .get()
                                    .addOnSuccessListener(qds -> {
                                        qds.getDocuments().forEach(ds -> ds.getReference().delete());
                                    });

                            // navigate
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, new HomeFragment())
                                    .commit();

                        })
                        .addOnFailureListener(e ->
                                Log.e("ORDER", "Order save failed " + e.getMessage())
                        );

            });


        });
    }
}





