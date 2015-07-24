package com.example.android.digidoor_gate;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.AlarmManager;

public class NumbPad {
    // flag values
    public static int NOFLAGS = 0;
    public static int HIDE_INPUT = 1;
    public static int HIDE_PROMPT = 2;

    static Float amountDue;

    static TextView prompt;
    static TextView promptValue;

    static Button btn1;
    static Button btn2;
    static Button btn3;
    static Button btn4;
    static Button btn5;
    static Button btn6;
    static Button btn7;
    static Button btn8;
    static Button btn9;
    static Button btn0;
    static Button btnC;
    static Button btnUnlock;
    static Button btnCallOwners;
    static Button btnScheduledUsers;

    private String value = "";
    private String addl_text = "";
    private NumbPad me;

    private int flag_hideInput = 0;
    private int flag_hidePrompt = 0;

    public interface numbPadInterface {
        public String numPadInputValue(String value);
        public String numPadCanceled();
    }

    public String getValue() {
        return value;
    }

    public void setAdditionalText(String inTxt) {
        addl_text = inTxt;
    }

    public void show(final Activity activity, final String promptString, int inFlags,
                     final numbPadInterface postrun) {
        me = this;
        flag_hideInput = inFlags % 2;
        flag_hidePrompt = (inFlags / 2) % 2;

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        //final AlertDialog.Builder dlg = new AlertDialog.Builder(activity, AlertDialog.THEME_TRADITIONAL);

        // (That new View is just there to have something inside the dialog that can grow big enough to cover the whole screen.)



       /* if (flag_hidePrompt == 0) {
            dlg.setTitle(promptString);
        }*/
        // Inflate the dialog layout
        LayoutInflater inflater = activity.getLayoutInflater();
        final View iView = inflater.inflate(R.layout.numb_pad, null, false);

        // create code to handle the change tender
        prompt = (TextView) iView.findViewById(R.id.promptText);
        prompt.setText(addl_text);
        if (addl_text.equals("")) {
            prompt.setVisibility(View.GONE);
        }
        promptValue = (TextView) iView.findViewById(R.id.promptValue);

        // Defaults
        value = "";
        promptValue.setText(" ");

        Typeface typeface = Typeface.createFromAsset(activity.getAssets(), "fonts/Gotham-light.ttf");
        btn1 = (Button) iView.findViewById(R.id.button1);
        btn2 = (Button) iView.findViewById(R.id.button2);
        btn3 = (Button) iView.findViewById(R.id.button3);
        btn4 = (Button) iView.findViewById(R.id.button4);
        btn5 = (Button) iView.findViewById(R.id.button5);
        btn6 = (Button) iView.findViewById(R.id.button6);
        btn7 = (Button) iView.findViewById(R.id.button7);
        btn8 = (Button) iView.findViewById(R.id.button8);
        btn9 = (Button) iView.findViewById(R.id.button9);
        btn0 = (Button) iView.findViewById(R.id.button0);
        btnC = (Button) iView.findViewById(R.id.buttonC);
        btnUnlock = (Button) iView.findViewById(R.id.buttonUnlock);
        btnScheduledUsers = (Button) iView.findViewById(R.id.buttonScheduled);
        btnCallOwners = (Button) iView.findViewById(R.id.buttonCall);

        btn1.setTypeface(typeface);
        btn2.setTypeface(typeface);
        btn3.setTypeface(typeface);
        btn4.setTypeface(typeface);
        btn5.setTypeface(typeface);
        btn6.setTypeface(typeface);
        btn7.setTypeface(typeface);
        btn8.setTypeface(typeface);
        btn9.setTypeface(typeface);
        btn0.setTypeface(typeface);
        btnC.setTypeface(typeface);
        btnUnlock.setTypeface(typeface);
        btnScheduledUsers.setTypeface(typeface);
        btnCallOwners.setTypeface(typeface);

        btnC.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                deleteNumber();
            }
        });
        btn1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                appendNumber("1");
            }
        });
        btn2.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                appendNumber("2");
            }
        });
        btn3.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                appendNumber("3");
            }
        });
        btn4.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                appendNumber("4");
            }
        });
        btn5.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                appendNumber("5");
            }
        });
        btn6.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                appendNumber("6");
            }
        });
        btn7.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                appendNumber("7");
            }
        });
        btn8.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                appendNumber("8");
            }
        });
        btn9.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                appendNumber("9");
            }
        });
        btn0.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                appendNumber("0");
            }
        });
        dialog.setContentView(iView);

        btnUnlock.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                postrun.numPadInputValue(me.getValue());
            }
        });

        btnCallOwners.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:+6590229794"));
                activity.startActivity(callIntent);

                Intent mStartActivity = new Intent(activity, FullscreenActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(activity, mPendingIntentId,
                        mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager)activity.getSystemService(activity.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
            }
        });

        btnScheduledUsers.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        /*dlg.setPositiveButton("UNLOCK", new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dlg, int sumthin) {
                dlg.dismiss();
                postrun.numPadInputValue(me.getValue());
            }
        });*/
/*
        dlg.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int sumthin) {
                dlg.dismiss();
                postrun.numPadCanceled();
            }
        });
*/
        //dlg.show();

        //final AlertDialog d = dlg.setView(iView).create();

        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        WindowManager wm = (WindowManager) activity.getSystemService(activity.WINDOW_SERVICE); // for activity use context instead of getActivity()
        Display display = wm.getDefaultDisplay(); // getting the screen size of device
        Point size = new Point();
        display.getSize(size);
        int width = size.x;  // Set your heights
        int height = size.y; // set your widths

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        //lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        //lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        lp.width = width-20;
        lp.height = height-200;

        dialog.getWindow().setAttributes(lp);
        dialog.show();

    }

    void appendNumber(String inNumb) {
        value = value + inNumb;
        if (flag_hideInput == 1) {
            promptValue.setText(promptValue.getText() + "*");
        } else {
            promptValue.setText(promptValue.getText() + inNumb);
        }
    }

    void deleteNumber(){
        value = "";
        //Sets the TextView to display a cleared state,
        //prompting users to re-enter pin.
        if (flag_hideInput == 1) {
            promptValue.setText(" ");
        }
    }

}
