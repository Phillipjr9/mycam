package com.example.virtualcamera;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Billing Manager - Handles in-app purchases using Google Play Billing
 * This allows users to purchase Pro features directly through Google Play
 */
public class BillingManager implements PurchasesUpdatedListener {
    
    private static final String TAG = "BillingManager";
    private static final String PREFS_NAME = "VirtualCameraBilling";
    private static final String KEY_PURCHASE_TOKEN = "purchase_token";
    private static final String KEY_PURCHASE_TIME = "purchase_time";
    
    // Product IDs (must match Google Play Console)
    public static final String SKU_PRO_MONTHLY = "pro_monthly";
    public static final String SKU_PRO_YEARLY = "pro_yearly";
    public static final String SKU_LIFETIME = "lifetime";
    
    private BillingClient billingClient;
    private Context context;
    private BillingManagerListener listener;
    private List<SkuDetails> skuDetailsList = new ArrayList<>();
    private List<Purchase> purchasesList = new ArrayList<>();
    
    public interface BillingManagerListener {
        void onBillingSetupFinished();
        void onPurchasesUpdated(List<Purchase> purchases);
        void onPurchaseError(String error);
    }
    
    public BillingManager(Context context, BillingManagerListener listener) {
        this.context = context;
        this.listener = listener;
        
        setupBillingClient();
    }
    
    /**
     * Setup billing client
     */
    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build();
        
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // Billing client is ready
                    querySkuDetails();
                    queryPurchases();
                    
                    if (listener != null) {
                        listener.onBillingSetupFinished();
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                    if (listener != null) {
                        listener.onPurchaseError("Billing setup failed");
                    }
                }
            }
            
            @Override
            public void onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected");
            }
        });
    }
    
    /**
     * Query SKU details from Google Play
     */
    private void querySkuDetails() {
        List<String> skuList = new ArrayList<>();
        skuList.add(SKU_PRO_MONTHLY);
        skuList.add(SKU_PRO_YEARLY);
        skuList.add(SKU_LIFETIME);
        
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBSCRIPTION);
        
        billingClient.querySkuDetailsAsync(params.build(), (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                this.skuDetailsList = skuDetailsList;
                Log.d(TAG, "SKU details loaded: " + skuDetailsList.size());
            } else {
                Log.e(TAG, "Failed to load SKU details");
            }
        });
        
        // Also query one-time products
        SkuDetailsParams.Builder oneTimeParams = SkuDetailsParams.newBuilder();
        oneTimeParams.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        
        billingClient.querySkuDetailsAsync(oneTimeParams.build(), (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                this.skuDetailsList.addAll(skuDetailsList);
            }
        });
    }
    
    /**
     * Query existing purchases
     */
    private void queryPurchases() {
        QueryPurchasesParams query = QueryPurchasesParams.newBuilder()
            .setSkuType(BillingClient.SkuType.SUBSCRIPTION)
            .build();
        
        billingClient.queryPurchasesAsync(query, (billingResult, purchasesList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                this.purchasesList = purchasesList;
                savePurchases(purchasesList);
                
                if (listener != null) {
                    listener.onPurchasesUpdated(purchasesList);
                }
            }
        });
        
        // Query one-time purchases
        QueryPurchasesParams oneTimeQuery = QueryPurchasesParams.newBuilder()
            .setSkuType(BillingClient.SkuType.INAPP)
            .build();
        
        billingClient.queryPurchasesAsync(oneTimeQuery, (billingResult, purchasesList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                this.purchasesList.addAll(purchasesList);
                savePurchases(purchasesList);
                
                if (listener != null) {
                    listener.onPurchasesUpdated(purchasesList);
                }
            }
        });
    }
    
    /**
     * Save purchases to preferences
     */
    private void savePurchases(List<Purchase> purchases) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        for (Purchase purchase : purchases) {
            editor.putString(KEY_PURCHASE_TOKEN + "_" + purchase.getSku(), purchase.getPurchaseToken());
            editor.putLong(KEY_PURCHASE_TIME + "_" + purchase.getSku(), purchase.getPurchaseTime());
        }
        
        editor.apply();
    }
    
    /**
     * Get SKU details by ID
     */
    public SkuDetails getSkuDetails(String skuId) {
        for (SkuDetails sku : skuDetailsList) {
            if (sku.getSku().equals(skuId)) {
                return sku;
            }
        }
        return null;
    }
    
    /**
     * Get all SKU details
     */
    public List<SkuDetails> getAllSkuDetails() {
        return skuDetailsList;
    }
    
    /**
     * Launch billing flow for a SKU
     */
    public void launchBillingFlow(Activity activity, String skuId) {
        SkuDetails skuDetails = getSkuDetails(skuId);
        if (skuDetails == null) {
            Log.e(TAG, "SKU details not found for: " + skuId);
            if (listener != null) {
                listener.onPurchaseError("Product not available");
            }
            return;
        }
        
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build();
        
        BillingResult result = billingClient.launchBillingFlow(activity, flowParams);
        
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Billing flow failed: " + result.getDebugMessage());
            if (listener != null) {
                listener.onPurchaseError("Billing flow failed");
            }
        }
    }
    
    /**
     * Check if user has purchased a SKU
     */
    public boolean isPurchased(String skuId) {
        for (Purchase purchase : purchasesList) {
            if (purchase.getSku().equals(skuId)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if user has any Pro subscription
     */
    public boolean hasProSubscription() {
        return isPurchased(SKU_PRO_MONTHLY) || isPurchased(SKU_PRO_YEARLY) || isPurchased(SKU_LIFETIME);
    }
    
    /**
     * Get purchase for a SKU
     */
    public Purchase getPurchase(String skuId) {
        for (Purchase purchase : purchasesList) {
            if (purchase.getSku().equals(skuId)) {
                return purchase;
            }
        }
        return null;
    }
    
    /**
     * Acknowledge a purchase
     */
    public void acknowledgePurchase(String purchaseToken) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build();
        
        billingClient.acknowledgePurchase(params, new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged");
                } else {
                    Log.e(TAG, "Failed to acknowledge purchase");
                }
            }
        });
    }
    
    /**
     * Consume a purchase (for one-time products)
     */
    public void consumePurchase(String purchaseToken) {
        ConsumeParams params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build();
        
        billingClient.consumeAsync(params, new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase consumed");
                } else {
                    Log.e(TAG, "Failed to consume purchase");
                }
            }
        });
    }
    
    /**
     * Convert SKU to license type
     */
    public static String skuToLicenseType(String sku) {
        switch (sku) {
            case SKU_PRO_MONTHLY:
                return LicenseManager.LICENSE_PRO_MONTHLY;
            case SKU_PRO_YEARLY:
                return LicenseManager.LICENSE_PRO_YEARLY;
            case SKU_LIFETIME:
                return LicenseManager.LICENSE_LIFETIME;
            default:
                return LicenseManager.LICENSE_FREE_TRIAL;
        }
    }
    
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                switch (purchase.getPurchaseState()) {
                    case Purchase.PurchaseState.PURCHASED:
                        // Purchase successful
                        if (!purchase.isAcknowledged()) {
                            acknowledgePurchase(purchase.getPurchaseToken());
                        }
                        break;
                    case Purchase.PurchaseState.PENDING:
                        // Purchase pending
                        break;
                    case Purchase.PurchaseState.UNSPECIFIED_STATE:
                        // Unknown state
                        break;
                }
            }
            
            purchasesList = purchases;
            savePurchases(purchases);
            
            if (listener != null) {
                listener.onPurchasesUpdated(purchases);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // User cancelled
            Log.d(TAG, "Purchase cancelled by user");
        } else {
            // Error
            Log.e(TAG, "Purchase error: " + billingResult.getDebugMessage());
            if (listener != null) {
                listener.onPurchaseError(billingResult.getDebugMessage());
            }
        }
    }
    
    /**
     * Destroy billing client
     */
    public void destroy() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
    
    /**
     * Get price string for SKU
     */
    public String getPrice(String skuId) {
        SkuDetails skuDetails = getSkuDetails(skuId);
        if (skuDetails != null) {
            return skuDetails.getPrice();
        }
        return "$0.00";
    }
    
    /**
     * Get title for SKU
     */
    public String getTitle(String skuId) {
        SkuDetails skuDetails = getSkuDetails(skuId);
        if (skuDetails != null) {
            return skuDetails.getTitle();
        }
        return skuId;
    }
    
    /**
     * Get description for SKU
     */
    public String getDescription(String skuId) {
        SkuDetails skuDetails = getSkuDetails(skuId);
        if (skuDetails != null) {
            return skuDetails.getDescription();
        }
        return "";
    }
}
