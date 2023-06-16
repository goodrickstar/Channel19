package com.cb3g.channel19;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchaseHistoryParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import java.util.List;

//mServerice Developer payload  "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ"

public class BillingUtils implements BillingClientStateListener {
    public boolean isConnected = false;
    public final String OLD_SUBSCRIPTION = "activate";
    public final String NEW_SUBSCRIPTION = "fivedollars";

    public BillingClient billingClient;

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        isConnected = true;
        //queryPurchases();
    }

    @Override
    public void onBillingServiceDisconnected() {
        isConnected = false;
        connect();
    }

    public BillingUtils(Context context, PurchasesUpdatedListener purchasesUpdatedListener) {
        billingClient = BillingClient.newBuilder(context).setListener(purchasesUpdatedListener).enablePendingPurchases().build();
        connect();
    }

    public BillingUtils(Context context) {
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().build();
        connect();
    }

    public void connect() {
        billingClient.startConnection(this);
    }

    public void purchaseProduct(Activity activity, ProductDetails productDetails) {
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).build()
                );
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList).build();
        billingClient.launchBillingFlow(activity, billingFlowParams);
    }

    public BillingResult purchaseSubscription(Activity activity, ProductDetails productDetails) {
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken()).build()
                );
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList).build();
        return billingClient.launchBillingFlow(activity, billingFlowParams);
    }

    public void queryPurchaseHistory(PurchaseHistoryResponseListener purchaseHistoryResponseListener) {
        billingClient.queryPurchaseHistoryAsync(QueryPurchaseHistoryParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), purchaseHistoryResponseListener);
    }

    void handlePurchase(Purchase purchase, ConsumeResponseListener consumeResponseListener) {
        ConsumeParams consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
        billingClient.consumeAsync(consumeParams, consumeResponseListener);
    }

    public void acknowledgePurchase(Purchase purchase, AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener) {
        if (purchase.isAcknowledged()) return;
        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken()).build();
        //.setDeveloperPayload(purchase.getDeveloperPayload()).build();
        billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
    }

    public void queryActiveSubscriptions(PurchasesResponseListener listener) {
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(), listener);
    }

    public void queryActivePurchases(PurchasesResponseListener listener) {
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), listener);
    }

    public void queryProductDetails(String itemTitle, ProductDetailsResponseListener productDetailsResponseListener) {
        QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(ImmutableList.of(QueryProductDetailsParams.Product.newBuilder().setProductId(itemTitle).setProductType(BillingClient.ProductType.INAPP).build()))
                .build();
        billingClient.queryProductDetailsAsync(queryProductDetailsParams, productDetailsResponseListener);
    }

    public void querySubscriptionDetails(String itemTitle, ProductDetailsResponseListener productDetailsResponseListener) {
        QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(ImmutableList.of(QueryProductDetailsParams.Product.newBuilder().setProductId(itemTitle).setProductType(BillingClient.ProductType.SUBS).build()))
                .build();
        billingClient.queryProductDetailsAsync(queryProductDetailsParams, productDetailsResponseListener);
    }

}
