package lk.hari.furnitica.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.widget.Toast;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private boolean wasConnected = false;
    private boolean isFirstCall = true;

    public NetworkChangeReceiver(Context context) {
        // Initialize with current network state
        wasConnected = isConnected(context);
    }

    public interface NetworkListener {
        void onNetworkChanged(boolean isConnected);
    }

    private NetworkListener listener;

    public NetworkChangeReceiver(NetworkListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        boolean isConnectedNow = isConnected(context);

        // Ignore first call after registration
        if (isFirstCall) {
            isFirstCall = false;
            return;
        }

        if (isConnectedNow && !wasConnected) {
            Toast.makeText(context, "Internet Connected", Toast.LENGTH_SHORT).show();
        } else if (!isConnectedNow && wasConnected) {
            Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show();
        }

        wasConnected = isConnectedNow;
    }

    private boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network network = cm.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            }
        }
        return false;
    }
}
