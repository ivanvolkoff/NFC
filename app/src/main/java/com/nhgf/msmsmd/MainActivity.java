package com.nhgf.msmsmd;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    private String ERROR_DETECTED = "No NFC tag Detected";
    private String WRITE_SUCCESS = "Text writen successfully";
    private String WRITE_ERROR = "Error during writing";

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter[] writingTagFitler;
    Boolean writeMode;
    Tag myTag;
    Context context;
    EditText et_input_value;
    TextView txt_present_value;
    Button btn_write;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        btn_write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (myTag == null) {
                        Toast.makeText(context, ERROR_DETECTED, Toast.LENGTH_LONG).show();
                    } else {
                        write("Plain Text: " + et_input_value.getText().toString(), myTag);
                        Toast.makeText(context, WRITE_SUCCESS, Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

            }

        });
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_LONG).show();
            finish();
        }
        readFromIntent(getIntent());
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writingTagFitler = new IntentFilter[]{tagDetected};

    }

    private void write(String s, Tag myTag) throws IOException, FormatException {
        NdefRecord[] records = {createRecord(s)};
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(myTag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    private NdefRecord createRecord(String s) throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = s.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLenght = langBytes.length;
        int textLenght = textBytes.length;
        byte[] payload = new byte[1 + langLenght + textLenght];
        payload[0] = (byte) langLenght;

        System.arraycopy(langBytes, 0, payload, 1, langLenght);
        System.arraycopy(textBytes, 0, payload, 1 + langLenght, textLenght);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        return recordNFC;


    }

    private void initView() {
        et_input_value = findViewById(R.id.txt_input_value);
        txt_present_value = findViewById(R.id.txt_presentValue);
        btn_write = findViewById(R.id.btn_write);
        context = this;
    }

    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMgs != null) {
                msgs = new NdefMessage[rawMgs.length];
                for (int i = 0; i < rawMgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMgs[i];

                }
                buildTagViews(msgs);
            }
        }
    }

    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;
        String text = "";
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLenght = payload[0] & 0063;
        try {
            text = new String(payload, languageCodeLenght + 1, payload.length - languageCodeLenght - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UNSUPPORD ENCODING", e.toString());
        }
        txt_present_value.setText("NFC Content" + text);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readFromIntent(intent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        writeModeOFF();
    }

    @Override
    protected void onResume() {
        super.onResume();
        writeModeOn();
    }

    /*****************************************************************************************
     * ******************************ENABLE WRITE******************************
     ******************************************************************************************/
    private void writeModeOn() {
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this,pendingIntent,writingTagFitler,null);
    }
    /*****************************************************************************************
     * ******************************DISABLE WRITE******************************
     ******************************************************************************************/
    private void writeModeOFF(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }
}