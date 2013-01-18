package jp.dip.commonsense;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ParseException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LocationCheckerActivity extends Activity implements
		LocationListener, GpsStatus.Listener, View.OnClickListener {
	/* = 0 �̕����́A�K���Ȓl�ɕύX���Ă��������i�Ƃ肠���������ɂ͖��Ȃ��ł����j */
	private static final int REQUEST_CODE = 1;

	/* �f�[�^�x�[�X���p�̐ݒ� */
	SQLiteDatabase locationDB = null;

	int count = 0; /* onLocationChanged�̎��s�񐔃J�E���g�p */
	String glat; /* �o�x */
	String glong; /* �ܓx */
	String name; /* �n�� */
	String category; /* �J�e�S�� */

	private MenuItem MENU_SELECT_A; /* �f�[�^�x�[�X�o�͗p�{�^�� */
	private MenuItem MENU_SELECT_B; /* �f�[�^�x�[�X���͗p�{�^�� */
	private MenuItem MENU_SELECT_C; /* CSV�o�͗p�{�^�� */
	private MenuItem MENU_SELECT_D; /* �o�^�{�^�� */

	private static final int ID_LOCATION_PROVIDER_ENABLED = 0;
	private static final int ID_LOCATION_PROVIDER_STATUS = 1;
	private static final String PROVIDER_ENABLED = " ENABLED ";
	private static final String PROVIDER_DISABLED = " DISABLED ";
	private static final int DISTANCE_MIN = 25;

	private LocationManager locationManager;
	private String bestProvider;
	private Map<String, LinearLayout> layoutMap = new HashMap<String, LinearLayout>();

	private TextView logText; /* �o�x�̃e�L�X�g�r���[ */
	private TextView latText; /* �ܓx�̃e�L�X�g�r���[ */
	private TextView providerText; /* �v���o�C�_�̃e�L�X�g�r���[ */
	private TextView locationTimeText; /* ���ݎ��Ԃ̃e�L�X�g�r���[ */
	private TextView nameText; /* �o�x�̃e�L�X�g�r���[ */
	private TextView categoryText; /* �o�x�̃e�L�X�g�r���[ */
	
	/* �e�[�u���쐬 */
	private String CREATE_TABLE_LOCATION = "create table locationDB("
			+ "_id integer primary key autoincrement,"
			+ "Latitude text not null,"
			+ "Longitude text not null," + "name text not null,"
			+ "category text not null," + "year text not null,"
			+ "month text not null," + "monthDay text not null,"
			+ "hour text not null," + "minute text not null,"
			+ "second text not null" + ")";
	
	private String[] columns_location = { "_id", "Latitude", "Longitude", "name",
			"category", "year", "month", "monthDay", "hour", "minute",
			"second" };
	
	private String[] columns_profile = { "Latitude", "Longitude", "name", "category" };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* �A�v���N�����ɃL�[�{�[�h��\�����Ȃ� */
		this.getWindow().setSoftInputMode(
				LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		setContentView(R.layout.main);

		/* DB�R�l�N�V�������� */
		locationDB = new SQLiteHelper().openConnection();

		/* �e�L�X�g�r���[�̐ݒ� */
		logText = (TextView) findViewById(R.id.longitude);
		latText = (TextView) findViewById(R.id.latitude);
		providerText = (TextView) findViewById(R.id.provider);
		locationTimeText = (TextView) findViewById(R.id.time);
		nameText = (TextView) findViewById(R.id.name);
		categoryText = (TextView) findViewById(R.id.category);

		((Button) findViewById(R.id.start)).setOnClickListener(this);
		((Button) findViewById(R.id.stop)).setOnClickListener(this);

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		// PowerRequirement �͐ݒ肵�Ȃ��̂��x�X�g�v���N�e�B�X
		// Accuracy �͐ݒ肵�Ȃ��̂��x�X�g�v���N�e�B�X
		// criteria.setAccuracy(Criteria.ACCURACY_FINE); �� Accuracy
		// �ōł�����Ă͂����Ȃ��p�^�[��
		// �ȉ��͕K�v�ɂ��
		criteria.setBearingRequired(false); // ���ʕs�v
		criteria.setSpeedRequired(false); // ���x�s�v
		criteria.setAltitudeRequired(false); // ���x�s�v
		bestProvider = locationManager.getBestProvider(criteria, true);
		Location locate = locationManager.getLastKnownLocation(bestProvider);

		if(locate == null){  
			 // ���ݒn���擾�ł��Ȃ������ꍇ�C�������ʂŎ擾���Ă݂�  
			 locate = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);  
			 bestProvider = LocationManager.NETWORK_PROVIDER;
		}
		
		if(locate == null){  
			// ���ݒn���擾�ł��Ȃ������ꍇ�CGPS�Ŏ擾���Ă݂�  
			locate = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); 
			bestProvider = LocationManager.GPS_PROVIDER;
		}  

		locationManager.addGpsStatusListener(this);
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		LinearLayout row = new LinearLayout(this);
		layoutMap.put(bestProvider, row);
		TextView label = new TextView(this);
		TextView providerName = new TextView(this);
		TextView enabled = new TextView(this);
		enabled.setId(ID_LOCATION_PROVIDER_ENABLED);
		String e = locationManager.isProviderEnabled(bestProvider) ? PROVIDER_ENABLED
				: PROVIDER_DISABLED;
		enabled.setText(e);
		TextView status = new TextView(this);
		status.setId(ID_LOCATION_PROVIDER_STATUS);
		status.setText("AVAILABLE");
		label.setText("Provider Status:");
		providerName.setText(bestProvider);
		row.addView(label);
		row.addView(providerName);
		row.addView(enabled);
		row.addView(status);
		layout.addView(row);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		/* �d�v�FrequestLocationUpdates�����܂܃A�v�����I������Ƌ��������������Ȃ�B */
		locationManager.removeUpdates(this);
		locationManager.removeGpsStatusListener(this);
		locationDB.close();
	}

	@Override
	public void onClick(View v) {

		/* �{�^���������ɃL�[�{�[�h���\���ɂ��� */
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

		locationManager.removeUpdates(this);
		logText.setText("");
		latText.setText("");
		nameText.setText("");
		categoryText.setText("");
		providerText.setText("");
		locationTimeText.setText("");
		if (v.getId() == R.id.start) {
			EditText timeEdit = (EditText) findViewById(R.id.min_time);
			EditText distanceEdit = (EditText) findViewById(R.id.min_distance);
			int time = Integer.valueOf(timeEdit.getText().toString()) * 1000;
			int distance = Integer.valueOf(distanceEdit.getText().toString());
			locationManager.requestLocationUpdates(bestProvider, time,
					distance, this);
			/*
			 * List<String> providers = locationManager.getProviders(true); for
			 * (String provider : providers) {
			 * locationManager.requestLocationUpdates(provider, time,distance,
			 * this); ��1����=�v���o�C�_ ��2����=�ʒm�̂��߂̍ŏ����ԊԊu(�~���b) ��3����=�ʒm�̂��߂̍ŏ������Ԋu(���[�g��)
			 * ��4����=�ʒu��񃊃X�i�[ }
			 */

		}
	}

	/* ���P�[�V������񂪕ύX���ꂽ�Ƃ����s */
	@Override
	public void onLocationChanged(Location location) {

		glat = String.valueOf(location.getLatitude());
		glong = String.valueOf(location.getLongitude());
		
		double distance = 0;

		/* android.text.format.Time�N���X�ł̌��ݓ��� */
		Time time = new Time();
		time.setToNow();
		String tmp = time.year + "/" + (time.month + 1) + "/" + time.monthDay
				+ " " + time.hour + ":" + time.minute + ":" + time.second;

		logText.setText(glat);
		latText.setText(glong);
		providerText.setText(String.valueOf(location.getProvider()));
		locationTimeText.setText(tmp);

		try {
			locationDB.execSQL(CREATE_TABLE_LOCATION);
		} catch (Exception e) {
			/* �e�[�u���쐬���s�����łɂ���Ƃ� */
			// Log.e("ERROR",e.toString());
			// Toast.makeText(this, "�e�[�u���쐬���s", Toast.LENGTH_LONG).show();
		}

		Log.e("�e�[�u���쐬", "debug1");

		// String resultsString = "";

		try {
			
			distance = distanceBetween();
			if(DISTANCE_MIN == distance){
				httpRequest();
			}
			
			nameText.setText(name);
			categoryText.setText(category);
		
			/* �f�[�^�x�[�X�}������ */
			locationDB.beginTransaction();
			ContentValues val = new ContentValues();
			val.put("latitude", glat);
			val.put("longitude", glong);
			val.put("name", name);
			val.put("category", category);
			val.put("year", time.year);
			val.put("month", time.month + 1);
			val.put("monthDay", time.monthDay);
			val.put("hour", time.hour);
			val.put("minute", time.minute);
			val.put("second", time.second);
			locationDB.insert("locationDB", "", val);
			locationDB.setTransactionSuccessful();
			locationDB.endTransaction();

			Log.e("�f�[�^�x�[�X�i�[", "debug4");

			// resultsString = "����";
		} catch (Exception e) {
			/* ��O���� */
			// resultsString = "���s";
			Log.v("ERROR", e.toString());
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
	
	/* 2�_�Ԃ̋������v�Z */
	public double distanceBetween() {
		
		double distance = DISTANCE_MIN;
		
		try {
			/* �N�G���̌��ʂ��擾 */
			Cursor cursor = locationDB.query("profileDB", columns_profile,
					null, null, null, null, "_id");

			/* �擾���ʂ𕶎��񌋍� */
			while (cursor.moveToNext()) {

				Log.v(String.valueOf(Double.valueOf(glat)), "���݂̈ܓx");
				Log.v(String.valueOf(Double.valueOf(glong)), "���݂̌o�x");
				Log.v(String.valueOf(cursor.getDouble(0)), "�␳�̈ܓx");
				Log.v(String.valueOf(cursor.getDouble(1)), "�␳�̌o�x");

				/* 2�_�Ԃ̋������v�Z */
				float[] results = new float[1];
				Location.distanceBetween(Double.valueOf(glat),
						Double.valueOf(glong), cursor.getDouble(0),
						cursor.getDouble(1), results);
				Log.v(String.valueOf(results[0]), "�����̋���");

				if (distance > results[0]) {
					distance = results[0];
					name = cursor.getString(2);
					category = cursor.getString(3);
				}
			}
		} catch (Exception e) {

			/* �G���[���� */
			Log.e("ERROR", e.toString());
			//Toast.makeText(this, "���s", Toast.LENGTH_LONG).show();
			/* text.setText("error!"); */
		}
		
		return distance;
	}

	/* foursquare API��URL�Ń��N�G�X�g���C�n���ƃJ�e�S�����擾 */
	public void httpRequest() {

		/* android.text.format.Time�N���X�ł̌��ݓ��� */
		Time time = new Time();
		time.setToNow();
		String tMonth = String.valueOf((time.month + 1));
		if (10 > time.month + 1) {
			tMonth = "0" + tMonth;
		}

		/* static�u���b�N�ȂǁA�A�v���P�[�V�����J�n�O�Ɏ��s����B */
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.permitAll().build());

		String scheme = "https";
		String authority = "api.foursquare.com";
		String path = "/v2/venues/search";

		String ll = glat + ", " + glong;
		String oauth_token = "G4RUWUY4YAKSUWLEWLXPHRBQJZATY4XPTIAI4OT02CXMLMM3";
		String v = time.year + tMonth + time.monthDay;
		Uri.Builder uriBuilder = new Uri.Builder();

		uriBuilder.scheme(scheme);
		uriBuilder.authority(authority);
		uriBuilder.path(path);
		uriBuilder.appendQueryParameter("ll", ll);
		uriBuilder.appendQueryParameter("oauth_token", oauth_token);
		uriBuilder.appendQueryParameter("v", v);

		String uri = uriBuilder.toString();

		Log.e("URL�쐬", "debug2");

		HttpClient httpClient = new DefaultHttpClient();
		HttpParams params = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, 1000);
		HttpConnectionParams.setSoTimeout(params, 1000);

		HttpUriRequest httpRequest = new HttpGet(uri);

		HttpResponse httpResponse = null;

		try {
			Log.e("���X�|���X�쐬�J�n", "debug11");
			httpResponse = httpClient.execute(httpRequest);
			if (httpResponse != null)
				Log.e("���X�|���X�쐬", "debug10");
			else
				Log.e("���X�|���X�쐬���s", "debug10");
		} catch (ClientProtocolException e) {
			/* ��O���� */
			Log.e("���X�|���X�쐬���s", "debug13");
		} catch (IOException e) {
			/* ��O���� */
			Log.e("���X�|���X�쐬���s", "debug13");
		}

		String json = null;

		if (httpResponse != null
				&& httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			HttpEntity httpEntity = httpResponse.getEntity();
			try {
				Log.e("JSON�f�[�^�̎擾", "debug9");
				json = EntityUtils.toString(httpEntity);
			} catch (ParseException e) {
				/* ��O���� */
			} catch (IOException e) {
				/* ��O���� */
			} finally {
				try {
					httpEntity.consumeContent();
				} catch (IOException e) {
					/* ��O���� */
				}
			}
		}

		httpClient.getConnectionManager().shutdown();

		Log.e("HTTP�ʐM�I��", "debug3");

		try {
			Log.e("����", "debug8");
			JSONObject rootObject = new JSONObject(json);
			JSONObject responseObject = rootObject.getJSONObject("response");
			JSONArray venuesArray = responseObject.getJSONArray("venues");

			int id = 0; /* �K�ꂽ���[�U�̐��̈�ԑ���venue�̏��� */
			int distance = 10000; /* ���ݒn����venue�܂ł̋��� */

			/* �擾����venue���X�g����venue�̐������O�ɏo�� */
			Log.e(String.valueOf(venuesArray.length()), "venue��");

			/* �擾����venue���X�g��venue�̒��ŋ����̈�ԋ߂�venue������ */
			for (int i = 0; i < venuesArray.length(); i++) {
				JSONObject locationObject = venuesArray.getJSONObject(i)
						.getJSONObject("location");
				if (distance > Integer.parseInt(locationObject
						.getString("distance"))) {
					distance = Integer.parseInt(locationObject
							.getString("distance"));
					id = i;
				}
			}

			JSONArray categoriesArray = venuesArray.getJSONObject(id)
					.getJSONArray("categories");

			Log.e("�I�u�W�F�N�g�̂����̂ڂ�", "debug5");
			JSONObject bookObject[] = new JSONObject[2];

			bookObject[0] = venuesArray.getJSONObject(id);
			bookObject[1] = categoriesArray.getJSONObject(0);

			Log.e("�I�u�W�F�N�g�̑��", "debug6");
			/* �n���̃f�[�^���擾 */
			name = bookObject[0].getString("name");

			/* �J�e�S���̃f�[�^���擾 */
			category = bookObject[1].getString("shortName");

			Log.e("�f�[�^�擾", "debug7");

			// resultsString = "����";
		} catch (Exception e) {
			/* ��O���� */
			// resultsString = "���s";
			Log.e("ERROR", e.toString());
		}
	}

	/* �f�[�^�x�[�X�擾 */
	public void sDatabase() {
		String out = "";
		try {
			/* �N�G���̌��ʂ��擾 */
			Cursor cursor = locationDB.query("locationDB", columns_location, null, null,
					null, null, "_id");

			/* �擾���ʂ𕶎��񌋍� */
			while (cursor.moveToNext()) {
				out += cursor.getInt(0) + ":Latitude = ";
				out += cursor.getString(1) + "\n" + "   Longitude = ";
				out += cursor.getString(2) + "\n" + "   name = ";
				out += cursor.getString(3) + "\n" + "   category = ";
				out += cursor.getString(4) + "\n" + "   time = ";
				out += cursor.getString(5) + "/";
				out += cursor.getString(6) + "/";
				out += cursor.getString(7) + " ";
				out += cursor.getString(8) + ":";
				out += cursor.getString(9) + ":";
				out += cursor.getString(10) + "\n\n";
			}

			/* �f�[�^�x�[�X�̓��e���o�� */
			Intent intent = new Intent();
			intent.setClassName("jp.dip.commonsense",
					"jp.dip.commonsense.subActivity");
			intent.putExtra("jp.dip.commonsense.testString", out);

			startActivity(intent);
		} catch (Exception e) {

			/* �G���[���� */
			Log.e("ERROR", e.toString());
			Toast.makeText(this, "���s", Toast.LENGTH_LONG).show();
			/* text.setText("error!"); */
		}
	}

	/* �f�[�^�x�[�X�폜 */
	public void dDatabase() {
		try {

			locationDB.beginTransaction();
			// �f�[�^�x�[�X�폜��SQL��
			locationDB.delete("locationDB", null, null);
			locationDB.execSQL("drop table locationDB");
			locationDB.setTransactionSuccessful();
			locationDB.endTransaction();
			Toast.makeText(this, "����", Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Log.e("ERROR", e.toString());
			Toast.makeText(this, "���s", Toast.LENGTH_LONG).show();
		}
	}

	/* CSV�o��(.txt�`��) */
	public void csvExport() {
		try {
			CsvUtil writer = new CsvUtil(CsvUtil.WRITE_MODE);

			/* �N�G���̌��ʂ��擾 */
			Cursor csr = locationDB.query("locationDB", columns_location, null, null,
					null, null, "_id");

			/* �f�[�^�x�[�X�̒��g��1�s�������o�� */
			while (csr.moveToNext()) {
				writer.add(createCsvLine(csr));
			}

			/* .txt�`����SD�J�[�h�ɏo�� */
			writer.close();
			Log.d("CSV", "CSV Export!");

			Toast.makeText(this, "����", Toast.LENGTH_LONG).show();
		} catch (Exception e) {

			Log.e("ERROR", e.toString());
			Toast.makeText(this, "���s", Toast.LENGTH_LONG).show();
		}
	}

	/* �f�[�^�x�[�X��1�f�[�^�̒��g��,�i�J���}�j����؂�Ŕ����o�� */
	private String createCsvLine(Cursor csr) {
		StringBuilder builder = new StringBuilder();
		builder.append(csr.getInt(0)).append(",").append(csr.getString(1))
				.append(",").append(csr.getString(2)).append(",")
				.append(csr.getString(3)).append(",").append(csr.getString(4))
				.append(",").append(csr.getString(5)).append("/")
				.append(csr.getString(6)).append("/").append(csr.getString(7))
				.append(" ").append(csr.getString(8)).append(":")
				.append(csr.getString(9)).append(":").append(csr.getString(10))
				.append(",");

		return builder.toString();
	}

	/* .txt�`���ŏo�͂��邽�߂̒��g */
	public class CsvUtil {

		public static final String WRITE_MODE = "WRITE";

		PrintWriter pw;
		File file;
		private String mode; /* ���[�h�̎w�� */

		/* .txt�t�@�C���̏o�͐� */
		private String path = Environment.getExternalStorageDirectory()
				.getPath() + "/sqlitefile/locationDB.txt";

		public CsvUtil(String code) {

			/* �����Ŏw�肳�ꂽ���[�h���i�[ */
			mode = code;

			/* �t�@�C���̍쐬 */
			checkFile();

			/* �w�肳�ꂽ���[�h�Ŏ��s */
			try {

				/* WRITE_MODE�Ȃ�t�@�C���ɕ������ǉ��ł���悤�ɂ��� */
				if (code.equals(WRITE_MODE)) {
					pw = new PrintWriter(new BufferedWriter(
							new FileWriter(file)));
				}
			} catch (Exception e) {

				e.printStackTrace();
			}
		}

		/* �t�@�C�����w�肳�ꂽ�p�X�ɍ쐬 */
		private void checkFile() {
			file = new File(path);
			try {
				if (!file.exists()) {
					file.createNewFile();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/* �V�����쐬�����t�@�C���Ɉ����Ŏw�肳�ꂽ�������ǉ� */
		public void add(String str) {
			if (mode.equals(WRITE_MODE))
				pw.println(str);
		}

		/* �I������ */
		public void close() {
			if (mode.equals(WRITE_MODE))
				pw.close();
		}

	}

	/* ����A��w�Ȃǂ̓o�^ */
	public void entry() {

		/* �o�^���W���[�����o�� */
		Intent intent = new Intent();
		intent.setClassName("jp.dip.commonsense",
				"jp.dip.commonsense.entryActivity");

		startActivity(intent);
	}

	/**
	 * �{�^���������ꂽ�Ƃ��Ăяo����� ���j���[�A�C�e�����쐬����
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		/* �I�v�V�������j���[���쐬���� */
		MENU_SELECT_A = menu.add(R.string.sDatabase);
		MENU_SELECT_B = menu.add(R.string.dDatabase);
		MENU_SELECT_C = menu.add(R.string.csvExport);
		MENU_SELECT_D = menu.add(R.string.entry);
		return true;
	}

	/**
	 * ���j���[���I�����ꂽ�Ƃ��Ăяo�����
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item == MENU_SELECT_A) {
			sDatabase();
		} else if (item == MENU_SELECT_B) {
			dDatabase();
		} else if (item == MENU_SELECT_C) {
			csvExport();
		} else if (item == MENU_SELECT_D) {
			entry();
		}
		return false;
	}

}
