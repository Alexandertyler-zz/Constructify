package edu.berkeley.constructify;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;

import com.qualcomm.toq.smartwatch.api.v1.deckofcards.Constants;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.Card;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.NotificationTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteToqNotification;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.CardImage;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.DeckOfCardsLauncherIcon;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.util.ParcelableUtil;

import java.io.InputStream;
import java.util.Iterator;


public class MainNav extends Activity {

    //TOQ VARS
    private final static String PREFS_FILE= "prefs_file";
    private final static String DECK_OF_CARDS_KEY= "deck_of_cards_key";
    private final static String DECK_OF_CARDS_VERSION_KEY= "03";

    private DeckOfCardsEventListener deckOfCardsEventListener;

    private DeckOfCardsManager mDeckOfCardsManager;
    private RemoteDeckOfCards mRemoteDeckOfCards;
    private RemoteResourceStore mRemoteResourceStore;
    private CardImage[] mCardImages;
    private ToqBroadcastReceiver toqReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main_nav);


        mDeckOfCardsManager = DeckOfCardsManager.getInstance(getApplicationContext());
        toqReceiver = new ToqBroadcastReceiver();

        deckOfCardsEventListener= new DeckOfCardsEventListenerImpl();
        init();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_nav, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.install_button) {
            install();
            return true;
        } else if (id == R.id.uninstall_button) {
            uninstall();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class DeckOfCardsEventListenerImpl implements DeckOfCardsEventListener {

        public void onCardOpen(final String cardId) {}

        public void onCardVisible(final String cardId) {
            runOnUiThread(new Runnable() {
                public void run() {
                    appNotify();
                    //Toast.makeText(toqFsm.this, cardId, Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void onCardInvisible(final String cardId) {}

        public void onCardClosed(final String cardId) {}

        public void onMenuOptionSelected(final String cardId, final String menuOption) {}

        public void onMenuOptionSelected(final String cardId, final String menuOption, final String quickReplyOption) {}
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void appNotify() {
        Toast.makeText(this, "A recent task has been updated by a worker.", Toast.LENGTH_LONG).show();

        ImageButton imageButton = (ImageButton) findViewById(R.id.taskButton);
        imageButton.setBackground(getResources().getDrawable(R.drawable.const3));


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Constructify Update")
                .setContentText("Peter Kim just updated 'Adding Support Beams' to 85%");

        Intent resultIntent = new Intent(this, MainNav.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(1, mBuilder.build());

    }


    private void sendNotification() {
        String[] notificationMsg = new String[2];
        notificationMsg[0] = "New Task: Task B.\n" +
                             "Move to cement wall on west wing.\n" +
                             "How close are support beams to being finished?\n" +
                             "+ 78% -";


        // Create a NotificationTextCard
        NotificationTextCard notificationCard = new NotificationTextCard(System.currentTimeMillis(),
                "Task Update", notificationMsg);

        // Draw divider between lines of text
        notificationCard.setShowDivider(true);
        // Vibrate to alert user when showing the notification
        notificationCard.setVibeAlert(true);
        // Create a notification with the NotificationTextCard we made
        RemoteToqNotification notification = new RemoteToqNotification(this, notificationCard);

        try {
            // Send the notification
            mDeckOfCardsManager.sendNotification(notification);
            Toast.makeText(this, "Update sent to Peter!", Toast.LENGTH_SHORT).show();
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Peter didn't get notification :(", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onStart() {
        super.onStart();
        mDeckOfCardsManager.addDeckOfCardsEventListener(deckOfCardsEventListener);

        if (!mDeckOfCardsManager.isConnected()) {
            try {

                mDeckOfCardsManager.connect();

            } catch (RemoteDeckOfCardsException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Installs applet to Toq watch if app is not yet installed
     */
    private void install() {
        boolean isInstalled = true;

        try {
            isInstalled = mDeckOfCardsManager.isInstalled();
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
        }

        if (!isInstalled) {
            try {
                mDeckOfCardsManager.installDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
            } catch (RemoteDeckOfCardsException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error: Cannot install application", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "App is already installed!", Toast.LENGTH_SHORT).show();
        }

        try{
            storeDeckOfCards();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void uninstall() {
        boolean isInstalled = true;

        try {
            isInstalled = mDeckOfCardsManager.isInstalled();
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
        }

        if (isInstalled) {
            try{
                mDeckOfCardsManager.uninstallDeckOfCards();
            }
            catch (RemoteDeckOfCardsException e){
                Toast.makeText(this, "Error uninstalling deck of cards.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Already uninstalled.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Adds a deck of cards to the applet
     */
    private void addSimpleTextCard(ListCard listCard) {
        int currSize = listCard.size();

        // Create a SimpleTextCard with 1 + the current number of SimpleTextCards
        SimpleTextCard simpleTextCard = new SimpleTextCard(Integer.toString(currSize+1));

        simpleTextCard.setHeaderText("Constructify");
        simpleTextCard.setTitleText("Task Update");
        String[] messages = {"Your new active task is:", "Task B", "Please update your previous task completion percentage"};
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(false);
        simpleTextCard.setShowDivider(true);

        listCard.add(simpleTextCard);

        try {
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards);
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to Create SimpleTextCard", Toast.LENGTH_SHORT).show();
        }
    }


    private void removeDeckOfCards() {
        ListCard listCard = mRemoteDeckOfCards.getListCard();
        if (listCard.size() == 0) {
            return;
        }

        listCard.remove(0);

        try {
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards);
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to delete Card from ListCard", Toast.LENGTH_SHORT).show();
        }

    }

    // Initialise
    private void init(){

        // Create the resource store for icons and images
        mRemoteResourceStore= new RemoteResourceStore();

        DeckOfCardsLauncherIcon whiteIcon = null;
        DeckOfCardsLauncherIcon colorIcon = null;

        // Get the launcher icons
        try{
            whiteIcon= new DeckOfCardsLauncherIcon("white.launcher.icon", getBitmap("bw.png"), DeckOfCardsLauncherIcon.WHITE);
            colorIcon= new DeckOfCardsLauncherIcon("color.launcher.icon", getBitmap("color.png"), DeckOfCardsLauncherIcon.COLOR);
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("Can't get launcher icon");
            return;
        }

        mCardImages = new CardImage[1];
        try{
            mCardImages[0]= new CardImage("card.image.1", getBitmap("image1.png"));
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("Can't get picture icon");
            return;
        }

        // Try to retrieve a stored deck of cards
        try {
            // If there is no stored deck of cards or it is unusable, then create new and store
            if ((mRemoteDeckOfCards = getStoredDeckOfCards()) == null){
                mRemoteDeckOfCards = createDeckOfCards();
                storeDeckOfCards();
            }
        }
        catch (Throwable th){
            th.printStackTrace();
            mRemoteDeckOfCards = null; // Reset to force recreate
        }

        // Make sure in usable state
        if (mRemoteDeckOfCards == null){
            mRemoteDeckOfCards = createDeckOfCards();
        }

        // Set the custom launcher icons, adding them to the resource store
        mRemoteDeckOfCards.setLauncherIcons(mRemoteResourceStore, new DeckOfCardsLauncherIcon[]{whiteIcon, colorIcon});

        // Re-populate the resource store with any card images being used by any of the cards
        for (Iterator<Card> it= mRemoteDeckOfCards.getListCard().iterator(); it.hasNext();){

            String cardImageId= ((SimpleTextCard)it.next()).getCardImageId();

            if ((cardImageId != null) && !mRemoteResourceStore.containsId(cardImageId)){

                if (cardImageId.equals("card.image.1")){
                    mRemoteResourceStore.addResource(mCardImages[0]);
                }

            }
        }
    }

    // Read an image from assets and return as a bitmap
    private Bitmap getBitmap(String fileName) throws Exception{

        try{
            InputStream is= getAssets().open(fileName);
            return BitmapFactory.decodeStream(is);
        }
        catch (Exception e){
            throw new Exception("An error occurred getting the bitmap: " + fileName, e);
        }
    }

    private RemoteDeckOfCards getStoredDeckOfCards() throws Exception{

        if (!isValidDeckOfCards()){
            Log.w(Constants.TAG, "Stored deck of cards not valid for this version of the demo, recreating...");
            return null;
        }

        SharedPreferences prefs= getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        String deckOfCardsStr= prefs.getString(DECK_OF_CARDS_KEY, null);

        if (deckOfCardsStr == null){
            return null;
        }
        else{
            return ParcelableUtil.unmarshall(deckOfCardsStr, RemoteDeckOfCards.CREATOR);
        }

    }

    /**
     * Uses SharedPreferences to store the deck of cards
     * This is mainly used to
     */
    private void storeDeckOfCards() throws Exception{
        // Retrieve and hold the contents of PREFS_FILE, or create one when you retrieve an editor (SharedPreferences.edit())
        SharedPreferences prefs = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        // Create new editor with preferences above
        SharedPreferences.Editor editor = prefs.edit();
        // Store an encoded string of the deck of cards with key DECK_OF_CARDS_KEY
        editor.putString(DECK_OF_CARDS_KEY, ParcelableUtil.marshall(mRemoteDeckOfCards));
        // Store the version code with key DECK_OF_CARDS_VERSION_KEY
        editor.putInt(DECK_OF_CARDS_VERSION_KEY, Constants.VERSION_CODE);
        // Commit these changes
        editor.commit();
    }

    // Check if the stored deck of cards is valid for this version of the demo
    private boolean isValidDeckOfCards(){

        SharedPreferences prefs= getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        // Return 0 if DECK_OF_CARDS_VERSION_KEY isn't found
        int deckOfCardsVersion= prefs.getInt(DECK_OF_CARDS_VERSION_KEY, 0);

        return deckOfCardsVersion >= Constants.VERSION_CODE;
    }

    // Create some cards with example content
    private RemoteDeckOfCards createDeckOfCards(){

        ListCard listCard= new ListCard();

        SimpleTextCard simpleTextCard = new SimpleTextCard("card0");

        simpleTextCard.setHeaderText("Constructify");
        simpleTextCard.setTitleText("Task Update");
        String[] messages = {"Your new active task is:", "Task B", "Please update your previous task completion percentage"};
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(true);
        listCard.add(simpleTextCard);


        return new RemoteDeckOfCards(this, listCard);
    }

    public void alertAndChange(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Set As Current Task")
                .setMessage("Are you sure you want to make this the active task?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        ImageButton imageButton = (ImageButton) findViewById(R.id.taskButton);
                        imageButton.setBackground(getResources().getDrawable(R.drawable.const3));
                        sendNotification();

                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
