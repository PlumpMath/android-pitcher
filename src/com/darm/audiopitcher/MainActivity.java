package com.darm.audiopitcher;

import java.io.IOException;

import com.smp.soundtouchandroid.SoundStreamAudioPlayer;
import com.triggertrap.seekarc.SeekArc;
import com.triggertrap.seekarc.SeekArc.OnSeekArcChangeListener;

import android.R.menu;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DebugUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private Button playButton, pauseButton;
	private ImageView fondoButton, seekArcImage, regionEspecial;
	private SeekArc seekArc;
	private long startTime, endTime;

	boolean running = false, loaded = false, paused = false;
	float actVolume, maxVolume, volume;
	AudioManager audioManager;
	private View v;
	protected boolean menuIsVisible = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Fullscreen
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);

		// hide bottom bar at start only
		// View decorView = getWindow().getDecorView();
		// int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
		// View.SYSTEM_UI_FLAG_FULLSCREEN;
		// decorView.setSystemUiVisibility(uiOptions);

		v = new View(this);
		hideMenu();

		init();

		// Shows/Hides menuBar, needs Root permisions
		regionEspecial = (ImageView) findViewById(R.id.regionEspecial);
		regionEspecial.setLongClickable(true);
		regionEspecial.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent ev) {
				Log.d("LC", ev.getAction() + "");
				if (ev.getAction() == MotionEvent.ACTION_DOWN) {
					startTime = ev.getEventTime();
					Log.d("LC", "IN DOWN");
					Log.d("LC", "start: " + startTime);
				} else if (ev.getAction() == MotionEvent.ACTION_UP) {
					endTime = ev.getEventTime();
					Log.d("LC", "IN UP");
					Log.d("LC", "end: " + endTime);
				} else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
					Log.d("LC", "IN move");
					Log.d("LC", "end: " + endTime);
				}
				if (endTime - startTime > 2000) {
					Log.d("LC", "time touched greater than 2000ms");
					if (menuIsVisible) {
						Toast.makeText(getBaseContext(), "Hidding Menu", Toast.LENGTH_SHORT).show();
						hideMenu();
					} else {
						Toast.makeText(getBaseContext(), "Showing Menu", Toast.LENGTH_SHORT).show();
						showMenu();
					}
					startTime = 0;
					endTime = 0;
					return true; // notify that you handled this event (do not
				}
				return false;// not handled this event yet, retur false then
			}
		});

		seekArcImage = (ImageView) findViewById(R.id.imagenSeckArk);

		seekArc = (SeekArc) findViewById(R.id.seekArc);
		seekArc.setTouchInSide(true);
		seekArc.setArcWidth(0);
		seekArc.setProgressWidth(0);
		seekArc.setArcWidth(0);
		seekArc.setProgress(50);

		seekArc.setOnSeekArcChangeListener(new OnSeekArcChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekArc seekArc) {
			}

			@Override
			public void onStartTrackingTouch(SeekArc seekArc) {
			}

			@Override
			public void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser) {
				log("Progress: " + progress);
				changePitch(progress);
				seekArcImage.setRotation(progress + (int) (progress * .2));
			}
		});

		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		actVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		volume = actVolume / maxVolume;
		this.setVolumeControlStream(AudioManager.STREAM_RING);

		playButton = (Button) findViewById(R.id.playButton);
		playButton.setVisibility(-1);
		playButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				play(arg0);
			}
		});

		pauseButton = (Button) findViewById(R.id.pauseButton);
		pauseButton.setVisibility(-1);
		pauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				pauseSound(arg0);
			}
		});
	}

	protected void showMenu() {
		try {
			// REQUIRES ROOT
			Build.VERSION_CODES vc = new Build.VERSION_CODES();
			Build.VERSION vr = new Build.VERSION();
			String ProcID = "79"; // HONEYCOMB AND OLDER

			// v.RELEASE //4.0.3
			if (vr.SDK_INT >= vc.ICE_CREAM_SANDWICH) {
				ProcID = "42"; // ICS AND NEWER
			}
			Process proc = Runtime.getRuntime().exec(
					"am startservice --user 0 -n com.android.systemui/.SystemUIService");
			proc.waitFor();
			menuIsVisible = true;
		} catch (Exception ex) {
			Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private void hideMenu() {
		try {
			Build.VERSION_CODES vc = new Build.VERSION_CODES();
			Build.VERSION vr = new Build.VERSION();
			String ProcID = "79"; // HONEYCOMB AND OLDER

			// v.RELEASE //4.0.3
			if (vr.SDK_INT >= vc.ICE_CREAM_SANDWICH) {
				ProcID = "42"; // ICS AND NEWER
			}

			// REQUIRES ROOT
			Process proc = Runtime.getRuntime().exec(
					new String[] { "su", "-c",
							"service call activity " + ProcID + " s16 com.android.systemui" }); // WAS
																								// 79
			proc.waitFor();
			menuIsVisible = false;
		} catch (Exception ex) {
			Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	SoundStreamAudioPlayer player;
	private Thread thread;
	private String path;
	private float currentTempo = 1f;
	private float currentSemiPitch;

	void init() {
		try {
			// String path = "/mnt/extSdCard/sound.data";
			path = Environment.getExternalStorageDirectory().getPath() + "/sound.data";
			log(path);
			player = new SoundStreamAudioPlayer(0, path, 1.0f, 0.0f);
			thread = new Thread(player);
			thread.start();
			player.start();
			// Looping the song:
			long end = player.getDuration() - player.getDuration() / 15;
			// long end = player.getDuration() - 1000;
			player.setLoopEnd(end);
			player.setLoopStart(0);
			log("player.getDuration()" + player.getDuration());
			log("loop from: 0 to:" + end + " " + player.isLooping());
		} catch (IOException e) {
			Toast.makeText(
					this,
					"File not found or incorrect format found: "
							+ path
							+ ". File format should be .mp3, 'cause of looping issues with other formats.",
					Toast.LENGTH_LONG).show();
			log("SoundStreamAudioPlayer errors: " + e.getMessage());
			// e.printStackTrace();
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		log("resultcode: " + resultCode);
		if (resultCode == 0)
			return;

		if (player != null) {
			player.stop();
		}

		Uri uri = Uri.parse(data.getDataString());
		try {
			// String path = "/mnt/extSdCard/03 - Beat It.mp3";
			player = new SoundStreamAudioPlayer(0, getRealPathFromURI(this, uri), 1.0f, 0.0f);
			thread = new Thread(player);
			thread.start();
			player.start();

			player.setPitchSemi(currentSemiPitch);
			player.setTempo(currentTempo);
			log(player.getDuration() + "");
			// player.setLoopStart(0);
			// player.setLoopEnd(100);

		} catch (IOException e) {
			log("SoundStreamAudioPlayer errors: " + e.getMessage());
			// e.printStackTrace();
		}

	}

	public String getRealPathFromURI(Context context, Uri contentUri) {
		Cursor cursor = null;
		try {
			String[] proj = { MediaStore.Images.Media.DATA };
			cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void changePitch(float progress) {
		if (null != player && player.isInitialized()) {
			// currentTempo = 1f + ((progress >= 50 ? progress : progress - 50)
			// / 100f);
			if (progress == 50) {
				currentTempo = 1f;
			} else if (progress > 50) {
				currentTempo = 1f + ((progress - 50) / 20f);
			} else {
				currentTempo = 1f + ((progress - 50) / 80f);
			}

			// cambiar para modificar rango de pitch
			float constantePitch = 25f;
			currentSemiPitch = (-.5f + (progress / 100f)) * constantePitch;

			player.setPitchSemi(currentSemiPitch);
			player.setTempo(currentTempo);
			Log.v("audiopitcher", "setPitchSemi: " + currentSemiPitch);
			Log.v("audiopitcher", "setTempo: " + currentTempo);
		} else {
			Log.v("audiopitcher", "player is not initialized!");
		}
	}

	public void play(View v) {
		Log.v("audiopitcher", "playLoop");
		if (player.isInitialized() & !running) {
			Log.v("player", "isInitialized");
			player.start();
			seekArc.setVisibility(0);
		} else if (player.isInitialized() && paused) {
			Log.v("audiopitcher", "resumed");
			paused = false;
			player.start();
			seekArc.setVisibility(0);
		}

	}

	public void pauseSound(View v) {
		Log.v("audiopitcher", "pauseSound");
		if (running && !paused) {
			player.pause();
			player.stop();
			Toast.makeText(this, "Paused", Toast.LENGTH_SHORT).show();
			paused = true;
			seekArc.setVisibility(-1);
		}
	}

	void log(String msg) {
		Log.v("audiopitcher", msg);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			openSoundPicker();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void openSoundPicker() {
		final Intent selectSound = new Intent(Intent.ACTION_GET_CONTENT);
		selectSound.setType("audio/*");
		startActivityForResult(selectSound, 1);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (null != player && player.isInitialized())
			player.stop();
		// thread.stop();
	}

}
