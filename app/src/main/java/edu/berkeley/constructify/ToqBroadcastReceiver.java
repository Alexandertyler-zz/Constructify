/**********************************************************************************
 * Copyright (c) 2013-2014 Qualcomm MEMS Technology, Inc. All rights reserved
 **********************************************************************************/
package edu.berkeley.constructify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.qualcomm.toq.smartwatch.api.v1.deckofcards.Constants;


/**
 * Broadcast receiver for Toq app install intent.
 */
public class ToqBroadcastReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        // Launch the demo app activity to complete the install of the deck of cards applet
        Intent launchIntent= new Intent(context, MainNav.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(launchIntent);
    }
}