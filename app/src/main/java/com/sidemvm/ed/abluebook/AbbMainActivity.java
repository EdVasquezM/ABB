package com.sidemvm.ed.abluebook;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.button.MaterialButton;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AbbMainActivity extends AppCompatActivity {

    /**Support fragment manager*/
    public static FragmentManager gSFManager;
    /**Set the file path to use in whole app*/
    public static File eFormFilePath;

    /**interstitial and rewarded ad*/
    private InterstitialAd intAd;
    private RewardedVideoAd rewardedVideoAd;
    /**eForm Owner*/
    public String eFormOwner;

    /**Navigation drawer and collapsing layout views*/
    private DrawerLayout dLayout;
    public FloatingActionButton mFAB;
    public MaterialButton aBtn;
    public TextView abbCoinView;
    public MaterialButton epBtn;
    public SwitchCompat discSwitch;

    /**Flag to double tap on back to exit*/
    private boolean backToExit;

    /**billing client and related*/
    private BillingClient billingClient;
    private String NO_ADS = "No ads buyer: ";
    private String CLOSE_ADS_ID = "close_ads";
    public boolean noAdsFlag;

    /**Offline connections manager and endpoint list*/
    private ConnectionsClient cManager;
    public static List<String> epIdList = new ArrayList<>();
    public static HashMap<String, String> epList = new HashMap<>();
    /**For discover other devices with the same service id*/
    private final EndpointDiscoveryCallback dCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String epId, @NonNull DiscoveredEndpointInfo epInfo){
            cManager.requestConnection(eFormOwner, epId, cLifeCycleCallback);
        }

        @Override
        public void onEndpointLost(@NonNull String epId) {}
    };
    /**For the connection life cycle*/
    private final ConnectionLifecycleCallback cLifeCycleCallback= new ConnectionLifecycleCallback(){
        @Override
        public void onConnectionInitiated(@NonNull String epId, @NonNull ConnectionInfo info) {
            cManager.acceptConnection(epId, plCallback);
            epList.put(epId, info.getEndpointName());
        }

        @Override
        public void onConnectionResult(@NonNull String epId, @NonNull ConnectionResolution cRes){
            if (cRes.getStatus().getStatusCode() == ConnectionsStatusCodes.STATUS_OK) {
                epIdList.add(epId);
                setEpViews();
                discSwitch.setChecked(false);
            } else epList.remove(epId);
        }

        @Override
        public void onDisconnected(@NonNull String epId) {
            epList.remove(epId);
            epIdList.remove(epId);
            setEpViews();
        }
    };
    /**Manage the payload*/
    private PayloadCallback plCallback = new PayloadCallback() {
        private SimpleArrayMap<Long, Payload.File> plFiles = new SimpleArrayMap<>();
        private final SimpleArrayMap<Long, String> plNames = new SimpleArrayMap<>();

        @Override
        public void onPayloadReceived(@NonNull String epId, @NonNull Payload payload) {
            switch (payload.getType()){
                case Payload.Type.BYTES:
                    //Get the clicker message on a String
                    byte[] msgArray = payload.asBytes();
                    if (msgArray != null) {
                        String msg = new String(msgArray);
                        String[] msgSplit = msg.split(">>(.*?)::");
                        if(msg.contains("<<fName>>")){
                            plNames.put(Long.parseLong(msgSplit[1]), msgSplit[2]);
                        } else if(msg.contains("<<qLine>>")){
                            gSFManager.beginTransaction().setCustomAnimations(R.anim.in_from_right,
                                    R.anim.out_to_left, R.anim.in_from_left, R.anim.out_to_right)
                                    .replace(R.id.fragmentContainer, EfAnsFragment.newInstance
                                            (Long.parseLong(msgSplit[1]), "eFormTapper",
                                                    msgSplit[4], Long.parseLong( msgSplit[2])),
                                            "Answering").addToBackStack(null).commit();
                        } else if(msg.contains("<<aLine>>")) handleAMsg(msg);
                    }
                    break;
                case Payload.Type.FILE:
                    plFiles.put(payload.getId(), payload.asFile());
                    break;
            }
        }

        @Override
        public void onPayloadTransferUpdate
                (@NonNull String epId, @NonNull PayloadTransferUpdate plUpdate) {
            if(plUpdate.getStatus() == PayloadTransferUpdate.Status.SUCCESS){
                Payload.File pl = plFiles.remove(plUpdate.getPayloadId());
                Uri uri;
                if (pl != null) {
                    uri = getUriFF(pl.asJavaFile());
                    String fName = plNames.get(plUpdate.getPayloadId());
                    if(fName != null){
                        if(AbbFileGenerator.renameToGet(uri.getPath(), fName)){
                            AbbContent.runUnzipping(AbbMainActivity.this, new File
                                    (AbbFileGenerator.getExternalAbbFilesPath(), fName));
                        }
                    } else AbbContent.handleIncomingFile(AbbMainActivity.this, uri);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abb_main);
        //Initialize fragment, files path and connections managers
        gSFManager = getSupportFragmentManager();
        eFormFilePath = getFilesDir();
        cManager = Nearby.getConnectionsClient(this);
        backToExit = false;
        //Initialize eQuizOwner
        //if owner isn't ready just launch startup activity
        if (getSharedPreferences(getString(R.string.pref_file), MODE_PRIVATE).contains("owner"))
            eFormOwner = getSharedPreferences(getString(R.string.pref_file), MODE_PRIVATE)
                    .getString("owner", "DefaultOwner");
        else startActivity(new Intent(this, AbbStartupActivity.class));
        //set ads
        //set the no ads flag
        String noAdsOwner = getSharedPreferences(getString(R.string.pref_file), MODE_PRIVATE)
                .getString(NO_ADS, "DefaultAdsState");
        if (noAdsOwner != null) noAdsFlag = noAdsOwner.equals(eFormOwner);
        else noAdsFlag = false;
        if(!noAdsFlag){
            MobileAds.initialize(this, "ca-app-pub-9246476793989866~3783737116");
            intAd = new InterstitialAd(this);
            intAd.setAdUnitId("ca-app-pub-9246476793989866/8506592518");
            intAd.loadAd(new AdRequest.Builder().build());
            intAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    //load next interstitial ad
                    intAd.loadAd(new AdRequest.Builder().build());
                }
            });
            rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
            rewardedVideoAd.setRewardedVideoAdListener(setRewardedAd());
            loadRewardedAd();
        }
        //Setup toolbar for navigation drawer
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu_hamburger);
        toolbar.setNavigationOnClickListener(setNavClkListener());
        //Set Fab and collapsing layout views
        mFAB = findViewById(R.id.fab);
        aBtn = findViewById(R.id.a_count_btn);
        abbCoinView = findViewById(R.id.abb_coin_view);
        //setAbbCoinView(abbCoinView, getString(R.string.abb_coins_owner, eFormOwner), 0, false);
        //Set drawer view
        setDrawerView();
        //Show initial list fragment
        if (savedInstanceState == null) {
            discSwitch.setChecked(true);
            //arrange list fragment for two pane or single pane
            int listContainer;
            if(findViewById(R.id.listContainer) != null) listContainer = R.id.listContainer;
            else listContainer = R.id.fragmentContainer;
            EfListFragment efListFrag = new EfListFragment();
            gSFManager.beginTransaction().add(listContainer, efListFrag,"eFormList").commit();
            //Handle files when opened from the selection of file
            handleIncomingIntents(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        handleIncomingIntents(intent);
    }

    @Override
    public void onResume() {
        if(!noAdsFlag) rewardedVideoAd.resume(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        if(!noAdsFlag) rewardedVideoAd.pause(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        cManager.stopDiscovery();
        cManager.stopAdvertising();
        if(!noAdsFlag) rewardedVideoAd.destroy(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed(){
        //confirm exit
        if(!backToExit && gSFManager.getBackStackEntryCount() == 0) {
            backToExit = true;
            showSnackBarMsg(getString(R.string.exit_msg), -1, android.R.string.cancel,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            backToExit = false;
                        }
                    });
        }
        else super.onBackPressed();
    }

    /**to do something whit the things from intents*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       if(resultCode == RESULT_OK){
           if (requestCode == 1) {handleIncomingIntents(data);}
           if (requestCode == 2){
               if (data != null){
                   String uriToFileResult = AbbContent.uriImageToFile(this, data.getData());
                   if(uriToFileResult.equals("ok")) showSnackBarMsg
                           (getString(R.string.success_message), -1, android.R.string.ok, null);
                   else showSnackBarMsg(uriToFileResult, 0, android.R.string.ok, null);
               } else {
                   Uri uri = getUriFF(AbbFileGenerator.getExternalAbbFilesPath());
                   uri = Uri.withAppendedPath(uri, "lastPick.jpg");
                   String uriToFileResult = AbbContent.uriImageToFile(this, uri);
                   if(uriToFileResult.equals("ok")) showSnackBarMsg
                           (getString(R.string.success_message), -1, android.R.string.ok, null);
                   else showSnackBarMsg
                           (uriToFileResult, -1, android.R.string.ok, null);
               }
           }
       } else if (requestCode == 0)
           showSnackBarMsg(getString(R.string.send_by_default_msg),0, android.R.string.ok,null);
    }

    private void handleIncomingIntents(Intent intent){
        if(intent.getData() != null) {
            String result = AbbContent.handleIncomingFile(this, intent.getData());
            if(result.contains("<<aLine>>")) handleAMsg(result);
            else showSnackBarMsg(result, 0, android.R.string.ok, null);
        }
    }

    /**to select an image*/
    public boolean selectImageIntent() {
        //intent to get an image
        Intent imaIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT){
            imaIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            imaIntent.addCategory(Intent.CATEGORY_OPENABLE);
        } else  imaIntent = new Intent(Intent.ACTION_GET_CONTENT);
        imaIntent.setType("image/*");
        // Camera.
        Uri abbFilesPath = getUriFF(AbbFileGenerator.getExternalAbbFilesPath());
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        captureIntent.putExtra
                (MediaStore.EXTRA_OUTPUT, Uri.withAppendedPath(abbFilesPath, "lastPick.jpg"));
        //two add to types of intent, galleries and camera
        Intent chooserIntent = Intent.createChooser(imaIntent, getString(R.string.sel_image_title));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {captureIntent});
        //use chooser intent
        if (chooserIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooserIntent, 2);
            return true;
        } else return false;
    }

    /**to handle answer line*/
    public void handleAMsg(String aMsg){
        String[] msgSplit = aMsg.split(">>(.*?)::");
        long efID = Long.parseLong(msgSplit[1]);
        AbbContent.eFormItem ef = AbbContent.eFORM_ITEMS.get(efID);
        List<String> lines = new ArrayList<>();
        lines.add(aMsg);
        if(ef != null){
            AbbContent.writeEFormFile(ef.efName, lines, true);
            showSnackBarMsg(getString
                    (R.string.receive_answer, ef.efName),-1, android.R.string.ok,null);
            aBtn.setText(getString
                    (R.string.a_hint, String.valueOf(AbbContent.getAnswersItems(ef.efId).size())));
            EfListFragment lFrag = (EfListFragment) gSFManager.findFragmentByTag("eFormList");
            if (lFrag != null && lFrag.isVisible())
                lFrag.efListAdapter.notifyItemChanged(AbbContent.eFORM_ITEM_LIST.indexOf(ef));
        }
    }

    /**to send a message*/
    public void sendMessage(String msg){
        if(epIdList.size() == 0)
            showSnackBarMsg(getString(R.string.no_one_connected), -1, android.R.string.ok, null);
        else {
            cManager.sendPayload(epIdList, Payload.fromBytes(msg.getBytes()));
            showSnackBarMsg(getString(R.string.sharing_offline), -1, android.R.string.ok, null);
        }
    }

    /**call it to get the uri from filepath**/
    public Uri getUriFF(File file){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return FileProvider.getUriForFile
                    (this, "com.sidemvm.ed.abluebook.file_provider", file);
        else return Uri.fromFile(file);
    }

    /**to send files offline or online*/
    public void sendFile(File fToSend){
        if (epIdList.size() != 0){
            Payload pl;
            //if not found, advice and finalize
            try {
                pl = Payload.fromFile(fToSend);
                String f = getString(R.string.pl_msg_form, String.valueOf(pl.getId()), fToSend.getName());
                //send the name, then, send the file
                cManager.sendPayload(epIdList, Payload.fromBytes(f.getBytes()));
                cManager.sendPayload(epIdList, pl);
                showSnackBarMsg(getString(R.string.sharing_offline), -1, android.R.string.ok, null);
            } catch (FileNotFoundException e) {
                showSnackBarMsg(getString(R.string.fail_message), -1, android.R.string.ok, null);
            }
        } else {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/abb");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.download_abb_message));
            intent.putExtra(Intent.EXTRA_STREAM, getUriFF(fToSend));
            startActivity(Intent.createChooser(intent, getResources().getText(R.string.send_by)));
        }
    }


    /**verify if eForm already exist*/
    public boolean eFormAlreadyExist(AbbContent.eFormItem efItem) {
        boolean exist = false;
        if (AbbContent.eFORM_ITEMS.get(efItem.efId) != null) exist = true;
        else for (int i = 0; i < AbbContent.eFORM_ITEM_LIST.size(); i++)
            if (AbbContent.eFORM_ITEM_LIST.get(i).efName.equals(efItem.efName)) exist = true;
        return exist;
    }

    /**Call to show fragment of edit or answer*/
    public void eFormEditIntent(long efId) {
        AbbContent.eFormItem ef = AbbContent.eFORM_ITEMS.get(efId);
        if (ef != null) {
            gSFManager.popBackStack();
            if (ef.efOwner.equals(eFormOwner))
                gSFManager.beginTransaction().setCustomAnimations(R.anim.in_from_right,
                        R.anim.out_to_left, R.anim.in_from_left, R.anim.out_to_right)
                        .replace(R.id.fragmentContainer, EfEditFragment.newInstance(efId), "Editing")
                        .addToBackStack(null).commit();
            else gSFManager.beginTransaction().setCustomAnimations(R.anim.in_from_right,
                    R.anim.out_to_left, R.anim.in_from_left, R.anim.out_to_right)
                    .replace(R.id.fragmentContainer, EfAnsFragment.newInstance(efId,
                            "", "", 0), "Answering").addToBackStack(null).commit();
        }
    }

    /**Replace line breaks of the raw input*/
    public String getTextWithLineBreaks(String rawTextFromEditInput){
        if(rawTextFromEditInput.indexOf('\n') > -1)
            return rawTextFromEditInput.replace("\n","--linebreak--");
        else return rawTextFromEditInput;
    }

    /**Set line breaks again*/
    public String setTextWithLineBreaks (String rawTextToShow){
        if(rawTextToShow.contains("--linebreak--"))
            return rawTextToShow.replace("--linebreak--","\n");
        else return rawTextToShow;
    }

    /**set drawer view*/
    public void setDrawerView(){
        dLayout = findViewById(R.id.drawer_layout);
        final NavigationView navView = findViewById(R.id.nav_view);
        CardView dHeaderCardView = (CardView) navView.inflateHeaderView(R.layout.ef_content_layout);
        AppCompatTextView efOwnerView = dHeaderCardView.findViewById(R.id.ef_item_owner_view);
        efOwnerView.setText(eFormOwner);
        //set listener for menu on drawer
        navView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.close_owner:
                        getSharedPreferences(getString(R.string.pref_file), MODE_PRIVATE)
                                .edit().remove("owner").apply();
                        startActivity(new Intent(AbbMainActivity.this, AbbStartupActivity.class));
                        finish();
                        return true;
                    case R.id.open_abb_file:
                        //intent to get an image
                        Intent intent;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT){
                            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                        } else  intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        startActivityForResult(Intent.createChooser
                                (intent, getResources().getText(R.string.send_by)), 1);
                        dLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.end_connections:
                        cManager.stopAllEndpoints();
                        epIdList = new ArrayList<>();
                        epList = new HashMap<>();
                        setEpViews();
                        discSwitch.setChecked(false);
                        return true;
                    case R.id.close_ads:
                        dLayout.closeDrawer(GravityCompat.START);
                        //set billing
                        billingClient = BillingClient.newBuilder(AbbMainActivity.this)
                                .setListener(new PurchasesUpdatedListener() {
                                    @Override
                                    public void onPurchasesUpdated
                                            (int rCode, @Nullable List<Purchase> purchases){
                                        if(rCode == BillingResponse.OK && purchases != null){
                                            for (Purchase purchase : purchases){
                                                if (purchase.getSku().equals(CLOSE_ADS_ID))
                                                    handlePurchases();
                                            }
                                        } else if (rCode == BillingResponse.ITEM_ALREADY_OWNED
                                                && purchases != null){
                                            handlePurchases();
                                        } else showSnackBarMsg(getString(R.string.fail_message),
                                                -1, android.R.string.ok, null);

                                    }}).build();
                        closeAds();
                        return true;
                    default: return false;
                }
            }
        });
        //set switch views and ep count on drawer
        MenuItem epLayoutMenu = navView.getMenu().findItem(R.id.end_point_count_view);
        LinearLayoutCompat epLayout = (LinearLayoutCompat) epLayoutMenu.getActionView();
        epBtn = (MaterialButton) epLayout.getChildAt(1);
        epBtn.setVisibility(View.VISIBLE);
        epBtn.setText(String.valueOf(epIdList.size()));
        epBtn.setOnClickListener(setAnsViewClkListener(0));
        MenuItem discMenu = navView.getMenu().findItem(R.id.discovery_switch);
        discSwitch = (SwitchCompat) discMenu.getActionView();
        MenuItem advMenu = navView.getMenu().findItem(R.id.advertising_switch);
        final SwitchCompat advSwitch = (SwitchCompat) advMenu.getActionView();
        discSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(epIdList.size() == 0){
                    if(isChecked){
                        advSwitch.setChecked(false);
                        cManager.startDiscovery("com.sidemvm.ed.abluebook", dCallback,
                                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        showSnackBarMsg(getString(R.string.discovery_msg),
                                                0, android.R.string.ok, null);
                                    }
                                });
                    } else cManager.stopDiscovery();
                } else discSwitch.setChecked(false);
            }
        });
        advSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) {
                    discSwitch.setChecked(false);
                    cManager.startAdvertising(eFormOwner, "com.sidemvm.ed.abluebook",
                            cLifeCycleCallback, new AdvertisingOptions.Builder()
                                    .setStrategy(Strategy.P2P_STAR).build());
                } else cManager.stopAdvertising();
            }
        });
    }

    /**to handle all purchases*/
    private void handlePurchases(){
        //handle close ads purchase.
        getSharedPreferences
                (getString(R.string.pref_file), MODE_PRIVATE)
                .edit().putString(NO_ADS, eFormOwner).apply();
    }

    /**set the click listener to see the answers of a e form*/
    public View.OnClickListener setAnsViewClkListener(final long eFormId){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRewardedAd();
                listDialogFrag.newInstance(eFormId).show(gSFManager, "AnswerList");
                dLayout.closeDrawer(GravityCompat.START);
            }
        };
    }

    /**set rewarded video ad listener*/
    public RewardedVideoAdListener setRewardedAd(){
        return new RewardedVideoAdListener() {
            @Override
            public void onRewardedVideoAdLoaded() {}

            @Override
            public void onRewardedVideoAdOpened() {}

            @Override
            public void onRewardedVideoStarted() {}

            @Override
            public void onRewardedVideoAdClosed() {loadRewardedAd();}

            @Override
            public void onRewarded(RewardItem rewardItem) {

            }

            @Override
            public void onRewardedVideoAdLeftApplication() {}

            @Override
            public void onRewardedVideoAdFailedToLoad(int i) {loadRewardedAd();}

            @Override
            public void onRewardedVideoCompleted() {}
        };
    }

    /**set click listener to navigation icon in toolbar*/
    public View.OnClickListener setNavClkListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dLayout.openDrawer(GravityCompat.START);
            }
        };
    }

    /**call to set ep count views*/
    public void setEpViews(){
        String epCountValue = String.valueOf(epIdList.size());
        if(epIdList.size() == 0) abbCoinView.setText("");
        else abbCoinView.setText(getString(R.string.drawer_ep_count, epCountValue));
        epBtn.setText(epCountValue);
    }

    /**call to show an ad*/
    public void setAd(AdView adView){
        if (!noAdsFlag){
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    /**call when tap close ads*/
    private void closeAds(){
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(int responseCode) {
                if (responseCode == BillingResponse.OK){
                    List<String> skuList = new ArrayList<>();
                    skuList.add(CLOSE_ADS_ID);
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                    billingClient.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse(int rCode,List<SkuDetails> skuDetailsList){
                            if (rCode == BillingResponse.OK){
                                for(SkuDetails skuDetails : skuDetailsList){
                                    if (skuDetails.getSku().equals(CLOSE_ADS_ID)) {
                                        BillingFlowParams p = BillingFlowParams
                                                .newBuilder().setSkuDetails(skuDetails).build();
                                        billingClient
                                                .launchBillingFlow(AbbMainActivity.this, p);
                                        break;
                                    }
                                }
                            } else showSnackBarMsg(getString(R.string.fail_message),
                                    -1,android.R.string.ok, null);
                        }
                    });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {closeAds();}
        });
    }

    /**call to show interstitial ad*/
    public void showIntAd(){
        if (!noAdsFlag && intAd.isLoaded()) intAd.show();
    }

    /**1° load  2° show rewarded video to be ready when user clicks*/
    public void loadRewardedAd() {
        if (!noAdsFlag) rewardedVideoAd.loadAd("ca-app-pub-9246476793989866/8138749241",
                new AdRequest.Builder().build());
    }

    public void showRewardedAd(){
        if (!noAdsFlag && rewardedVideoAd.isLoaded()) rewardedVideoAd.show();
        else showIntAd();
    }

    /**Call to show msg on Snack Bar*/
    public void showSnackBarMsg(String msg, int length, int action, View.OnClickListener onClk) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.app_bar), msg, length);
        TextView tv= snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setMaxLines(5);
        snackbar.setAction(action, onClk);
        snackbar.setActionTextColor(getResources().getColor(R.color.colorAccent));
        snackbar.show();
    }
}
