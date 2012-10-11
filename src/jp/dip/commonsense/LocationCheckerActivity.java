package jp.dip.commonsense;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LocationCheckerActivity extends Activity implements
		LocationListener, GpsStatus.Listener, GpsStatus.NmeaListener,
		View.OnClickListener {
	// = 0 �̕����́A�K���Ȓl�ɕύX���Ă��������i�Ƃ肠���������ɂ͖��Ȃ��ł����j
	private static final int REQUEST_CODE = 1;
	LocationDB helper = null;
	SQLiteDatabase db = null;
	int count = 0;
	private MenuItem MENU_SELECT_A;
	private MenuItem MENU_SELECT_B;

	private static final int ID_LOCATION_PROVIDER_ENABLED = 0;
	private static final int ID_LOCATION_PROVIDER_STATUS = 1;
	private static final String PROVIDER_ENABLED = " ENABLED ";
	private static final String PROVIDER_DISABLED = " DISABLED ";

	private LocationManager locationManager;
	private Map<String, LinearLayout> layoutMap = new HashMap<String, LinearLayout>();

	private TextView elapsedTimeText;
	private TextView logText;
	private TextView latText;
	private TextView accText;
	private TextView providerText;
	private TextView locationTimeText;

	private long startTime = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		helper = new LocationDB(LocationCheckerActivity.this);

		logText = (TextView) findViewById(R.id.longitude);
		latText = (TextView) findViewById(R.id.latitude);
		accText = (TextView) findViewById(R.id.accuracy);
		providerText = (TextView) findViewById(R.id.provider);
		locationTimeText = (TextView) findViewById(R.id.time);
		elapsedTimeText = (TextView) findViewById(R.id.elapsed_time);
		((Button) findViewById(R.id.start)).setOnClickListener(this);
		((Button) findViewById(R.id.stop)).setOnClickListener(this);
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.addGpsStatusListener(this);
		locationManager.addNmeaListener(this);
		List<String> providers = locationManager.getAllProviders();
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		for (String provider : providers) {
			LinearLayout row = new LinearLayout(this);
			layoutMap.put(provider, row);
			TextView label = new TextView(this);
			TextView providerName = new TextView(this);
			TextView enabled = new TextView(this);
			enabled.setId(ID_LOCATION_PROVIDER_ENABLED);
			String e = locationManager.isProviderEnabled(provider) ? PROVIDER_ENABLED
					: PROVIDER_DISABLED;
			enabled.setText(e);
			TextView status = new TextView(this);
			status.setId(ID_LOCATION_PROVIDER_STATUS);
			status.setText("AVAILABLE");
			label.setText("Provider Status:");
			providerName.setText(provider);
			row.addView(label);
			row.addView(providerName);
			row.addView(enabled);
			row.addView(status);
			layout.addView(row);
		}
	}
	
	// �f�[�^�x�[�X�擾
	public void sDatabase() {
		db = helper.getReadableDatabase();
		String out = "";
		try {
			String[] columns = { "_id", "Latitude","Longitude" };
			// �N�G���̌��ʂ��擾
			Cursor cursor = db.query("test", columns, null, null, null, null, "_id");
			// �擾���ʂ𕶎��񌋍�
			while (cursor.moveToNext()) {
				out += cursor.getInt(0) + ":Latitude = ";
				out += cursor.getString(1) + ",Longitude = ";
				out += cursor.getString(2) + "\n";
			}
			// �f�[�^�x�[�X�̓��e���o��
			//text.setText(out);
						
			Intent intent=new Intent();
			intent.setClassName("jp.dip.commonsense","jp.dip.commonsense.subActivity");
			intent.putExtra("jp.dip.commonsense.testString", out);
				 
			startActivity(intent);
		} catch (Exception e) {
			// �G���[����
			Log.e("ERROR", e.toString());
			Toast.makeText(this, "���s", Toast.LENGTH_LONG).show();
			//text.setText("error!");
		}
	}
	
	// �f�[�^�x�[�X�폜
	public void dDatabase() {
		try {
			db = helper.getWritableDatabase();
			db.beginTransaction();
			//�f�[�^�x�[�X�폜��SQL��
			db.delete("test",null,null);
			db.execSQL("drop table test");
			db.setTransactionSuccessful();
			db.endTransaction();
			Toast.makeText(this, "����", Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Log.e("ERROR", e.toString());
			Toast.makeText(this, "���s", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// �d�v�FrequestLocationUpdates�����܂܃A�v�����I������Ƌ��������������Ȃ�B
		locationManager.removeUpdates(this);
		locationManager.removeNmeaListener(this);
		locationManager.removeGpsStatusListener(this);
	}

	@Override
	public void onClick(View v) {
		locationManager.removeUpdates(this);
		elapsedTimeText.setText("");
		logText.setText("");
		latText.setText("");
		accText.setText("");
		providerText.setText("");
		locationTimeText.setText("");
		if (v.getId() == R.id.start) {
			EditText timeEdit = (EditText) findViewById(R.id.min_time);
			EditText distanceEdit = (EditText) findViewById(R.id.min_distance);
			int time = Integer.valueOf(timeEdit.getText().toString()) * 1000;
			int distance = Integer.valueOf(distanceEdit.getText().toString());
			List<String> providers = locationManager.getProviders(true);
			for (String provider : providers) {
				locationManager.requestLocationUpdates(provider, time,
						distance, this);
				// ��1����=�v���o�C�_
				// ��2����=�ʒm�̂��߂̍ŏ����ԊԊu
				// ��3����=�ʒm�̂��߂̍ŏ������Ԋu
				// ��4����=�ʒu��񃊃X�i�[
			}
			startTime = System.currentTimeMillis();
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		elapsedTimeText
				.setText(String.valueOf((System.currentTimeMillis() - startTime)));
		logText.setText(String.valueOf(location.getLongitude()));
		latText.setText(String.valueOf(location.getLatitude()));
		accText.setText(String.valueOf(location.getAccuracy()));
		providerText.setText(String.valueOf(location.getProvider()));
		Date d = new Date(location.getTime());
		locationTimeText.setText(d.toLocaleString());
		startTime = System.currentTimeMillis();
		try{
			// �e�[�u���쐬
			String sql = "create table test("
					+ "_id integer primary key autoincrement,"
					+ "Latitude text not null,"
					+ "Longitude text not null" +")";
			db.execSQL(sql);
		}catch(Exception e){
			// �e�[�u���쐬���s�����łɂ���Ƃ�
			// Log.e("ERROR",e.toString());
			// Toast.makeText(this, "�e�[�u���쐬���s", Toast.LENGTH_LONG).show();        	
		}
		if(count == 0){
			String resultsString = "";
			db = helper.getWritableDatabase();
			try{
				// �f�[�^�x�[�X�}������
				db.beginTransaction();
				ContentValues val = new ContentValues();
				val.put("latitude", location.getLatitude());
				val.put("longitude", location.getLongitude());
				db.insert("test","",val);	
				db.setTransactionSuccessful();
				db.endTransaction();
			
				resultsString = "����";
        	}catch(Exception e){
        		resultsString = "���s";
        		Log.e("ERROR",e.toString());
        	}
		}
		count += 1;
		if(count == 2){
			count = 0;
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		LinearLayout row = layoutMap.get(provider);
		TextView tv = (TextView) row.findViewById(ID_LOCATION_PROVIDER_ENABLED);
		tv.setText(PROVIDER_DISABLED);
	}

	@Override
	public void onProviderEnabled(String provider) {
		LinearLayout row = layoutMap.get(provider);
		TextView tv = (TextView) row.findViewById(ID_LOCATION_PROVIDER_ENABLED);
		tv.setText(PROVIDER_ENABLED);
	}

	/**
	 * ���̃��\�b�h�́A�v���o�C�_�̏ꏊ���擾���邱�Ƃ��ł��Ȃ��ꍇ�A �܂��͍ŋߎg�p�s�\�̊��Ԍ�ɗ��p�\�ƂȂ��Ă���ꍇ�ɌĂяo����܂��B
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		LinearLayout row = layoutMap.get(provider);
		TextView tv = (TextView) row.findViewById(ID_LOCATION_PROVIDER_STATUS);
		String statusString = "Unknown";
		if (status == LocationProvider.AVAILABLE) {
			statusString = "AVAILABLE";
		} else if (status == LocationProvider.OUT_OF_SERVICE) {
			statusString = "OUT OF SERVICE";
		} else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
			statusString = "TEMP UNAVAILABLE";
		}
		tv.setText(statusString);
	}

	@Override
	public void onGpsStatusChanged(int event) {
		TextView tv = (TextView) findViewById(R.id.gps_status);
		String status = "";
		if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
			status = "FIRST FIX";
		} else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
			status = "SATELLITE STATUS";
		} else if (event == GpsStatus.GPS_EVENT_STARTED) {
			status = "STARTED";
		} else if (event == GpsStatus.GPS_EVENT_STOPPED) {
			status = "STOPPED";
		}
		tv.setText(status);
	}

	@Override
	public void onNmeaReceived(long timestamp, String nmea) {
		/*
		 * Date d = new Date(timestamp); String[] data = nmea.split(","); //
		 * GPGSV,GPGSA,GPRMC,GPVTG,GPGGA if (data[0].equals("$GPGSA")) {
		 * TextView tv = (TextView)findViewById(R.id.nmea_gpgsa);
		 * tv.setText(nmea.trim()); } else if (data[0].equals("$GPRMC")) {
		 * TextView tv = (TextView)findViewById(R.id.nmea_gprmc);
		 * tv.setText(nmea.trim()); } else if (data[0].equals("$GPVTG")) {
		 * TextView tv = (TextView)findViewById(R.id.nmea_gpvtg);
		 * tv.setText(nmea.trim()); } else if (data[0].equals("$GPGGA")) {
		 * TextView tv = (TextView)findViewById(R.id.nmea_gpgga);
		 * tv.setText(nmea.trim()); } else if (data[0].equals("$GPGSV")) {
		 * LinearLayout layout =
		 * (LinearLayout)findViewById(R.id.nmea_gpgsv_layout); int messageNum =
		 * Integer.valueOf(data[2]); TextView tv =
		 * (TextView)layout.findViewById(messageNum); if (tv == null) { tv = new
		 * TextView(this); layout.addView(tv); tv.setId(messageNum); }
		 * tv.setText(nmea.trim()); }
		 */
	}
	/**
	 * ���j���[���I�����ꂽ�Ƃ��Ăяo�����
	 */
	public boolean onOptionsItemSelected(MenuItem item){
		if(item == MENU_SELECT_A){
			// ���j���[���I�΂ꂽ��AsetCenterToNowLocation�Œn�}���X�N���[������
			sDatabase();
		}else if(item == MENU_SELECT_B){
			dDatabase();
		}
		return false;
	}
	/**
	 * �{�^���������ꂽ�Ƃ��Ăяo�����
	 * ���j���[�A�C�e�����쐬����
	 */
	public boolean onCreateOptionsMenu(Menu menu){
		// �I�v�V�������j���[���쐬����
		MENU_SELECT_A = menu.add(R.string.sDatabase);
		MENU_SELECT_B = menu.add(R.string.dDatabase);
		return true;
	}
	
}
