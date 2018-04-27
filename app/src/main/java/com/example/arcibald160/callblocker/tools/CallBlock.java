package com.example.arcibald160.callblocker.tools;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.arcibald160.callblocker.data.BlockListContract;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CallBlock extends BroadcastReceiver {
    // This String will hold the incoming phone number
    private String number;
    private static final String TAG = "CallBlockReceiver";

    @Override
    public void onReceive(Context context, Intent intent)
    {

        // If, the received action is not a type of "Phone_State", ignore it
        if (!intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            return;
        } else {

            // Fetch the number of incoming call
            number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            // TODO: remove hardcoded values and make better solution
            String countryCode = "+385";
            if (number.contains(countryCode)) {
                number = number.replace(countryCode, "0");
            }

//            // debug purposes e-is ususally error but this is hack for broadcaste receiver
//            Log.e(TAG, number);

            Uri uri = BlockListContract.BlockListEntry.CONTENT_URI;

            // search only number column
            Cursor mCursor = context.getContentResolver().query(
                    uri,
                    new String[] {BlockListContract.BlockListEntry.COLUMN_NUMBER},
                    BlockListContract.BlockListEntry.COLUMN_NUMBER + "=?",
                     new String[]{ number },
                    null
            );
            // Check, whether this is a member of "Black listed" phone numbers stored in the database
            if(mCursor.getCount() != 0) {
                // If yes, invoke the method
                try {
                    declinePhone(context);
                    addToBlockedCallsList(number, context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mCursor.close();
            return;
        }
    }

    private void addToBlockedCallsList(String number, Context context) {
        Date current = new Date();

        java.sql.Date sqlDate = new java.sql.Date(current.getTime());
        java.sql.Time sqlTime = new java.sql.Time(current.getTime());

        // insert new values to blocked contacts list (used in recently)
        ContentValues mBlockedList = new ContentValues();
        mBlockedList.put(BlockListContract.BlockedCallsReceived.COLUMN_NUMBER, number);
        mBlockedList.put(BlockListContract.BlockedCallsReceived.COLUMN_DATE, String.valueOf(sqlDate));
        mBlockedList.put(BlockListContract.BlockedCallsReceived.COLUMN_TIME, String.valueOf(sqlTime));

        context.getContentResolver().insert(
                BlockListContract.BlockedCallsReceived.CONTENT_URI, // the user dictionary content URI
                mBlockedList                                        // the values to insert
        );
    }

    private void declinePhone(Context context) throws Exception {
        try {
            String serviceManagerName = "android.os.ServiceManager";
            String serviceManagerNativeName = "android.os.ServiceManagerNative";
            String telephonyName = "com.android.internal.telephony.ITelephony";

            Class<?> telephonyClass;
            Class<?> telephonyStubClass;
            Class<?> serviceManagerClass;
            Class<?> serviceManagerNativeClass;

            Method telephonyEndCall;
            Object telephonyObject;
            Object serviceManagerObject;
            telephonyClass = Class.forName(telephonyName);
            telephonyStubClass = telephonyClass.getClasses()[0];
            serviceManagerClass = Class.forName(serviceManagerName);
            serviceManagerNativeClass = Class.forName(serviceManagerNativeName);

            Method getService = // getDefaults[29];
                    serviceManagerClass.getMethod("getService", String.class);
            Method tempInterfaceMethod = serviceManagerNativeClass.getMethod("asInterface", IBinder.class);

            Binder tmpBinder = new Binder();
            tmpBinder.attachInterface(null, "fake");
            serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
            IBinder retbinder = (IBinder) getService.invoke(serviceManagerObject, "phone");
            Method serviceMethod = telephonyStubClass.getMethod("asInterface", IBinder.class);
            telephonyObject = serviceMethod.invoke(null, retbinder);
            telephonyEndCall = telephonyClass.getMethod("endCall");
            telephonyEndCall.invoke(telephonyObject);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("unable", "msg cant dissconect call....");
        }
    }
}
