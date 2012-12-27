package jp.dip.commonsense;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
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
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
		LocationListener, GpsStatus.Listener, View.OnClickListener {
	// = 0 �̕����́A�K���Ȓl�ɕύX���Ă��������i�Ƃ肠���������ɂ͖��Ȃ��ł����j
	private static final int REQUEST_CODE = 1;

	/* �f�[�^�x�[�X���p�̐ݒ� */
	locationDB helper = null;
	SQLiteDatabase locationDB = null;

	int count = 0; /* onLocationChanged�̎��s�񐔃J�E���g�p */

	private MenuItem MENU_SELECT_A; /* �f�[�^�x�[�X�o�͗p�{�^�� */
	private MenuItem MENU_SELECT_B; /* �f�[�^�x�[�X���͗p�{�^�� */
	private MenuItem MENU_SELECT_C; /* CSV�o�͗p�{�^�� */

	private static final int ID_LOCATION_PROVIDER_ENABLED = 0;
	private static final int ID_LOCATION_PROVIDER_STATUS = 1;
	private static final String PROVIDER_ENABLED = " ENABLED ";
	private static final String PROVIDER_DISABLED = " DISABLED ";

	private LocationManager locationManager;
	private Map<String, LinearLayout> layoutMap = new HashMap<String, LinearLayout>();

	private TextView elapsedTimeText; /* �o�ߎ��Ԃ̃e�L�X�g�r���[ */
	private TextView logText; /* �o�x�̃e�L�X�g�r���[ */
	private TextView latText; /* �ܓx�̃e�L�X�g�r���[ */
	private TextView providerText; /* �v���o�C�_�̃e�L�X�g�r���[ */
	private TextView locationTimeText; /* ���ݎ��Ԃ̃e�L�X�g�r���[ */

	private long startTime = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		helper = new locationDB(LocationCheckerActivity.this);

		/* �e�L�X�g�r���[�̐ݒ� */
		logText = (TextView) findViewById(R.id.longitude);
		latText = (TextView) findViewById(R.id.latitude);
		providerText = (TextView) findViewById(R.id.provider);
		locationTimeText = (TextView) findViewById(R.id.time);
		elapsedTimeText = (TextView) findViewById(R.id.elapsed_time);

		((Button) findViewById(R.id.start)).setOnClickListener(this);
		((Button) findViewById(R.id.stop)).setOnClickListener(this);

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.addGpsStatusListener(this);
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// �d�v�FrequestLocationUpdates�����܂܃A�v�����I������Ƌ��������������Ȃ�B
		locationManager.removeUpdates(this);
		locationManager.removeGpsStatusListener(this);
	}

	@Override
	public void onClick(View v) {
		locationManager.removeUpdates(this);
		elapsedTimeText.setText("");
		logText.setText("");
		latText.setText("");
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
				// ��2����=�ʒm�̂��߂̍ŏ����ԊԊu(�~���b)
				// ��3����=�ʒm�̂��߂̍ŏ������Ԋu(���[�g��)
				// ��4����=�ʒu��񃊃X�i�[
			}
			startTime = System.currentTimeMillis();
		}
	}

	@Override
	public void onLocationChanged(Location location) {

		// static�u���b�N�ȂǁA�A�v���P�[�V�����J�n�O�Ɏ��s����B
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.permitAll().build());

		String scheme = "https";
		String authority = "api.foursquare.com";
		String path = "/v2/venues/search";

		String ll = location.getLatitude() + ", " + location.getLongitude();
		String oauth_token = "G4RUWUY4YAKSUWLEWLXPHRBQJZATY4XPTIAI4OT02CXMLMM3";
		String v = "20121017";
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
			// ��O����
			Log.e("���X�|���X�쐬���s", "debug13");
		} catch (IOException e) {
			// ��O����
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
				// ��O����
			} catch (IOException e) {
				// ��O����
			} finally {
				try {
					httpEntity.consumeContent();
				} catch (IOException e) {
					// ��O����
				}
			}
		}

		httpClient.getConnectionManager().shutdown();

		Log.e("HTTP�ʐM�I��", "debug3");

		elapsedTimeText
				.setText(String.valueOf((System.currentTimeMillis() - startTime)));
		logText.setText(String.valueOf(location.getLongitude()));
		latText.setText(String.valueOf(location.getLatitude()));
		providerText.setText(String.valueOf(location.getProvider()));
		Date date = new Date(location.getTime());
		locationTimeText.setText(date.toLocaleString());
		startTime = System.currentTimeMillis();

		try {
			// �e�[�u���쐬
			String sql = "create table locationDB("
					+ "_id integer primary key autoincrement,"
					+ "Latitude text not null," + "Longitude text not null,"
					+ "name text not null," + "category text not null,"
					+ "time text not null" + ")";
			locationDB.execSQL(sql);
		} catch (Exception e) {
			// �e�[�u���쐬���s�����łɂ���Ƃ�
			// Log.e("ERROR",e.toString());
			// Toast.makeText(this, "�e�[�u���쐬���s", Toast.LENGTH_LONG).show();
		}

		Log.e("�e�[�u���쐬", "debug1");

		if (count == 0) {
			// String resultsString = "";
			locationDB = helper.getWritableDatabase();

			try {
				Log.e("����", "debug8");
				JSONObject rootObject = new JSONObject(json);
				JSONObject responseObject = rootObject
						.getJSONObject("response");
				JSONArray venuesArray = responseObject.getJSONArray("venues");
				JSONArray categoriesArray = venuesArray.getJSONObject(0)
						.getJSONArray("categories");

				Log.e("�I�u�W�F�N�g�̂����̂ڂ�", "debug5");
				JSONObject bookObject[] = new JSONObject[2];

				bookObject[0] = venuesArray.getJSONObject(0);
				bookObject[1] = categoriesArray.getJSONObject(0);

				Log.e("�I�u�W�F�N�g�̑��", "debug6");
				// �n���̃f�[�^���擾
				String name = bookObject[0].getString("name");

				// �@�J�e�S���̃f�[�^���擾
				String category = bookObject[1].getString("shortName");

				Log.e("�f�[�^�擾", "debug7");
				// �f�[�^�x�[�X�}������
				locationDB.beginTransaction();
				ContentValues val = new ContentValues();
				val.put("latitude", location.getLatitude());
				val.put("longitude", location.getLongitude());
				val.put("name", name);
				val.put("category", category);
				val.put("time", date.toLocaleString());
				locationDB.insert("locationDB", "", val);
				locationDB.setTransactionSuccessful();
				locationDB.endTransaction();

				Log.e("�f�[�^�x�[�X�i�[", "debug4");

				// resultsString = "����";
			} catch (Exception e) {
				// ��O����
				// resultsString = "���s";
				Log.e("ERROR", e.toString());
			}
		}
		count += 1;
		if (count == 2) {
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

	// �f�[�^�x�[�X�擾
	public void sDatabase() {
		locationDB = helper.getReadableDatabase();
		String out = "";
		try {
			String[] columns = { "_id", "Latitude", "Longitude", "name",
					"category", "time" };

			/* �N�G���̌��ʂ��擾 */
			Cursor cursor = locationDB.query("locationDB", columns, null, null,
					null, null, "_id");

			/* �擾���ʂ𕶎��񌋍� */
			while (cursor.moveToNext()) {
				out += cursor.getInt(0) + ":Latitude = ";
				out += cursor.getString(1) + "\n" + "   Longitude = ";
				out += cursor.getString(2) + "\n" + "   name = ";
				out += cursor.getString(3) + "\n" + "   category = ";
				out += cursor.getString(4) + "\n" + "   time = ";
				out += cursor.getString(5) + "\n\n";
			}

			/* �f�[�^�x�[�X�̓��e���o�� */
			/* text.setText(out); */

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
			locationDB = helper.getWritableDatabase();
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
		locationDB = helper.getReadableDatabase();
		try {
			String[] columns = { "_id", "Latitude", "Longitude", "name",
					"category", "time" };

			CsvUtil writer = new CsvUtil(CsvUtil.WRITE_MODE);
			
			/* �N�G���̌��ʂ��擾 */
			Cursor csr = locationDB.query("locationDB", columns, null, null,
					null, null, "_id");
			
			/* �f�[�^�x�[�X�̒��g��1�s�������o�� */
			while(csr.moveToNext()){
				writer.add(createCsvLine(csr));
			}
			
			/* .txt�`����SD�J�[�h�ɏo�� */
			writer.close();
			Log.d("CSV","CSV Export!");

			Toast.makeText(this, "����", Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			
			Log.e("ERROR", e.toString());
			Toast.makeText(this, "���s", Toast.LENGTH_LONG).show();
		}
	}
	
	/* �f�[�^�x�[�X��1�f�[�^�̒��g��,�i�J���}�j����؂�Ŕ����o�� */
	private String createCsvLine(Cursor csr){
		StringBuilder builder = new StringBuilder();
		builder.append(csr.getInt(0)).append(",")
		.append(csr.getString(1)).append(",")
		.append(csr.getString(2)).append(",")
		.append(csr.getString(3)).append(",")
		.append(csr.getString(4)).append(",")
		.append(csr.getString(5)).append(",");
		return builder.toString();
	}
	
	/* .txt�`���ŏo�͂��邽�߂̒��g */
	public class CsvUtil {
		
		public static final String WRITE_MODE = "WRITE";
		
		PrintWriter pw;
		File file;
		private String mode;     /* ���[�h�̎w�� */
		
		/* .txt�t�@�C���̏o�͐� */
		private String path = Environment.getExternalStorageDirectory().getPath() + "/location/locationDB.txt";
		
		public CsvUtil(String code){
			
			/* �����Ŏw�肳�ꂽ���[�h���i�[ */
			mode = code;
			
			/* �t�@�C���̍쐬 */ 
			checkFile();
			
			/* �w�肳�ꂽ���[�h�Ŏ��s */
			try{
				
				/* WRITE_MODE�Ȃ�t�@�C���ɕ������ǉ��ł���悤�ɂ��� */
				if(code.equals(WRITE_MODE)){
					pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));			
				}
			}catch (Exception e) {
				
				e.printStackTrace();
			}
		}

		/* �t�@�C�����w�肳�ꂽ�p�X�ɍ쐬 */
		private void checkFile(){
			file = new File(path);
			try{
				if(!file.exists()){
					file.createNewFile();
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		/* �V�����쐬�����t�@�C���Ɉ����Ŏw�肳�ꂽ�������ǉ� */
		public void add(String str){
			if(mode.equals(WRITE_MODE))
				pw.println(str);
		}
		
		/* �I������ */
		public void close(){
			if(mode.equals(WRITE_MODE))pw.close();
		}
		
	}

	/**
	 * �{�^���������ꂽ�Ƃ��Ăяo����� ���j���[�A�C�e�����쐬����
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		// �I�v�V�������j���[���쐬����
		MENU_SELECT_A = menu.add(R.string.sDatabase);
		MENU_SELECT_B = menu.add(R.string.dDatabase);
		MENU_SELECT_C = menu.add(R.string.csvExport);
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
		}
		return false;
	}

}
