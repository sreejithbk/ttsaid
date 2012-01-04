/*
 * This file is part of the TTSAid project.
 *
 * Copyright (C) 2011-2012 Carlos Barcellos <carlosbar@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301 USA
 */

package com.ttsaid;

import java.util.ArrayList;
import java.util.Locale;

import com.ttsaid.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.CheckBox;

public class TTSActivity extends Activity {
	private int MY_DATA_CHECK_CODE = 0x0001;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private int interval;
	private boolean screenEvent;
	private boolean phoneNumber;
	private boolean smsReceive;
	private boolean timeSpeech;
	private SharedPreferences prefs;
	private TextToSpeech mTTS;
	private int SELECT_LANGUAGE_ACTIVITY = 0x01021848;
	
	/* get result from activities */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == MY_DATA_CHECK_CODE) {
			if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				// missing data, install it
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			} else {
				mTTS = new TextToSpeech(TTSActivity.this,new OnInitListener() {
					public void onInit(int status) {
					}
				});
			}
		} else if(requestCode == SELECT_LANGUAGE_ACTIVITY && resultCode == RESULT_OK) {
			((EditText) findViewById(R.id.language)).setText(data.getStringExtra("selected"));
		}
	}

	/* list activity creation */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the result to CANCELED. This will cause the widget host to cancel
		// out of the widget placement if they press the back button.
		setResult(RESULT_CANCELED);

		/* verify if TTS data is up to date */
		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
		
		//
		// preferences database
		//
		prefs = getSharedPreferences(LocalService.PREFS_DB, 0);

		// set current view

		String versionName;
		
		try {
			versionName = this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch(Exception e) {
			versionName = "?";
		}
		setContentView(R.layout.config);
		setTitle(String.format("%s v. %s",getString(R.string.app_name),versionName));

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();

		/* get parameters */

		interval = prefs.getInt("SET_INTERVAL", interval);
		screenEvent = prefs.getBoolean("SET_SCREEN_EVENT", false);
		phoneNumber = prefs.getBoolean("SET_PHONE_NUMBER", false);
		smsReceive = prefs.getBoolean("SET_SMS_RECEIVE", false);
		timeSpeech = prefs.getBoolean("SET_TIME_SPEECH", false);
		
		// get widget id

		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		// subclass seekbar events

		
		((Button) findViewById(R.id.selectInterval)).setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				final AlertDialog.Builder dlg = new AlertDialog.Builder(TTSActivity.this);
				final LayoutInflater layoutInflater = LayoutInflater.from(TTSActivity.this);
				final View view = layoutInflater.inflate(R.layout.interval, null);
				dlg.setView(view);
				
				((SeekBar) view.findViewById(R.id.interval)).setMax(8);
				((SeekBar) view.findViewById(R.id.interval)).setKeyProgressIncrement(1);
				((SeekBar) view.findViewById(R.id.interval)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

							public void onStopTrackingTouch(SeekBar seekBar) {
							}

							public void onStartTrackingTouch(SeekBar seekBar) {
							}

							public void onProgressChanged(SeekBar seekBar,
									int progress, boolean fromUser) {
								String hour = "";
								int m;
								
								if (progress == 0) {
									((TextView) view.findViewById(R.id.intervalValue)).setText(getString(R.string.off));
									return;
								}
								if (progress > 3) {
									hour = new Integer(progress / 4).toString();
								}
								m = new Integer(progress % 4 * 15);
								if (hour.length() > 0) {
									hour = hour + "h:" + String.format("%02d", m) + "m";
								} else {
									hour = m + " min";
								}
								((TextView) view.findViewById(R.id.intervalValue)).setText(hour);
							}
						});
				
				dlg.setOnCancelListener(new OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						setTimeInterval(((SeekBar) view.findViewById(R.id.interval)).getProgress());
					}
				});
				((SeekBar) view.findViewById(R.id.interval)).setProgress(interval);
				dlg.show();
			}
		});

		((CheckBox) findViewById(R.id.screenEvent))
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						screenEvent = isChecked;
					}
				});

		((CheckBox) findViewById(R.id.phoneNumber))
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						phoneNumber = isChecked;
					}
				});

		((CheckBox) findViewById(R.id.timeSpeech))
		.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				timeSpeech = isChecked;
			}
		});
		
		((CheckBox) findViewById(R.id.smsReceive))
		.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				smsReceive = isChecked;
			}
		});
		
		((Button) findViewById(R.id.searchLanguage)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Locale [] loclist = Locale.getAvailableLocales();
				ArrayList<String> list = new ArrayList<String>();
				
				for(int x=0;x < loclist.length;x++) {
					int avail = mTTS.isLanguageAvailable(loclist[x]);
					if(mTTS != null && (avail == TextToSpeech.LANG_COUNTRY_AVAILABLE || (loclist[x].getCountry().length() == 0 && avail == TextToSpeech.LANG_AVAILABLE))) {
						String str = String.format("%s %s,%s,%s",loclist[x].getDisplayLanguage(),loclist[x].getDisplayCountry(),loclist[x].getLanguage(),loclist[x].getCountry());
						int y;
		
						for(y=0;y < list.size();y++) {
							if(((String) list.get(y)).equals(str)) {
								break;
							}
						}
						if(y >= list.size()) {
							list.add(str);
						}
					}
				}
				
				final Intent newIntent = new Intent(TTSActivity.this, SelectLanguage.class);
				newIntent.putExtras(new Bundle());
				newIntent.putExtra("list_items",list.toArray(new String[list.size()]));
				startActivityForResult(newIntent,SELECT_LANGUAGE_ACTIVITY);
			}
		});
		
		/* set current values */

		//((SeekBar) findViewById(R.id.interval)).setProgress(interval);
		((CheckBox) findViewById(R.id.timeSpeech)).setChecked(timeSpeech);
		((CheckBox) findViewById(R.id.screenEvent)).setChecked(screenEvent);
		((CheckBox) findViewById(R.id.phoneNumber)).setChecked(phoneNumber);
		((EditText) findViewById(R.id.incomingMessage)).setText(prefs.getString("INCOMING_MESSAGE","Incoming Call!"));
		((CheckBox) findViewById(R.id.smsReceive)).setChecked(smsReceive);
		((EditText) findViewById(R.id.smsMessage)).setText(prefs.getString("SMS_MESSAGE","SMS Received from"));
		((EditText) findViewById(R.id.language)).setText(prefs.getString("SET_LANGUAGE","en_US"));
		setTimeInterval(interval);

	}

	private void setTimeInterval(int progress)
	{
		String hour = "";
		int	m;
		
		interval = progress;
		
		if (interval == 0) {
			((TextView) findViewById(R.id.intervalValue)).setText(getString(R.string.off));
			return;
		}
		if (interval > 3) {
			hour = new Integer(interval / 4).toString();
		}
		m = new Integer(interval % 4 * 15);
		if (hour.length() > 0) {
			hour = hour + "h:" + String.format("%02d", m) + "m";
		} else {
			hour = m + " min";
		}
		((TextView) findViewById(R.id.intervalValue)).setText(hour);
	}

	
	@Override
	public void onBackPressed() {
		// super.onBackPressed();

		RemoteViews views = new RemoteViews(TTSActivity.this.getPackageName(),
				R.layout.main);
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(TTSActivity.this);

		/* set play action on widget canvas */

		Intent play = new Intent(LocalService.PLAY_SOUND);
		play.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(TTSActivity.this, 0, play, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.play, pendingIntent);

		Intent config = new Intent(TTSActivity.this, TTSActivity.class);
		config.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		PendingIntent pendingConfig = PendingIntent.getActivity(TTSActivity.this, 0, config, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.config, pendingConfig);

		/* update view */

		appWidgetManager.updateAppWidget(mAppWidgetId, views);

		/* set new values on service */

		Intent intent = new Intent(TTSActivity.this, LocalService.class);
		startService(intent);

		/* set result */

		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);

		/* save preferences */

		SharedPreferences.Editor prefset = prefs.edit();
		prefset.putInt("SET_INTERVAL", interval);
		prefset.putBoolean("SET_TIME_SPEECH", timeSpeech);
		prefset.putBoolean("SET_SCREEN_EVENT", screenEvent);
		prefset.putBoolean("SET_PHONE_NUMBER", phoneNumber);
		prefset.putBoolean("SET_SMS_RECEIVE", smsReceive);
		prefset.putString("INCOMING_MESSAGE",((EditText) findViewById(R.id.incomingMessage)).getText().toString());		
		prefset.putString("SMS_MESSAGE",((EditText) findViewById(R.id.smsMessage)).getText().toString());
		String lang =  ((EditText) findViewById(R.id.language)).getText().toString();
		if(lang.length() == 0) lang = "en";
		prefset.putString("SET_LANGUAGE",lang);
		prefset.commit();

		/* start service */

		intent = new Intent(TTSActivity.this, LocalService.class);
		startService(intent);

		finish();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(mTTS != null) mTTS.shutdown();
	}
}