package jp.dip.commonsense;

import java.io.IOException;

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
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ParseException;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class entryActivity extends Activity implements LocationListener, View.OnClickListener {
	
	/* �f�[�^�x�[�X���p�̐ݒ� */
	SQLiteDatabase locationDB = null;
	
	private String glat;          /* �o�x */
	private String glong;         /* �ܓx */
	private String ename;         /* �o�^�n�� */
	private String ecategory;     /* �o�^�J�e�S�� */	
	private String name;                 /* ���O�Ɋi�[����n�� */
	private String category;             /* ���O�Ɋi�[����n�� */
	private String pName;                /* �L�ӈʒu�̒n�� */
	private String pCategory;            /* �L�ӈʒu�̃J�e�S�� */
	private String hName;                /* venue�̒n�� */
	private String hCategory;            /* venue�̃J�e�S�� */
	private float pDistance = 10000;     /* �L�ӈʒu�Ƃ̋��� */
	private float hDistance = 10000;     /* venue�Ƃ̋��� */	
	
	private LocationManager locationManager;
	
	private MenuItem MENU_SELECT_A; /* �f�[�^�x�[�X�o�͗p�{�^�� */
	
	private TextView logText;          /* �o�x�̃e�L�X�g�r���[ */
	private TextView latText;          /* �ܓx�̃e�L�X�g�r���[ */
	private TextView providerText;     /* �v���o�C�_�̃e�L�X�g�r���[ */
	private TextView nameText;         /* �n���̃e�L�X�g�r���[ */
	private TextView categoryText;     /* �J�e�S���̃e�L�X�g�r���[ */
	
	private String bestProvider;
	
	private String[] columns_profile = { "_id","Latitude", "Longitude", "name", "category" };


	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry);
 
        Intent intent = getIntent();
        
        /* static�u���b�N�ȂǁA�A�v���P�[�V�����J�n�O�Ɏ��s����B */
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        
        /* DB�R�l�N�V��������	 */																						
        locationDB = new SQLiteHelper().openConnection();
        
        ((Button) findViewById(R.id.entry)).setOnClickListener(this);
        
        /* �e�L�X�g�r���[�̐ݒ� */
		logText = (TextView) findViewById(R.id.longitude);
		latText = (TextView) findViewById(R.id.latitude);
		providerText = (TextView) findViewById(R.id.provider);
		nameText = (TextView) findViewById(R.id.name);
		categoryText = (TextView) findViewById(R.id.category);
        
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

        if(intent != null){
        	
        	/* spinner�̐ݒ�E���ړo�^ */
    		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
    		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // �A�C�e����ǉ����܂�
            adapter.add("Home");
            adapter.add("University");
            adapter.add("work");
            adapter.add("other");
            
            /* spinner�̕\�� */
            Spinner spinner = (Spinner) findViewById(R.id.spinner);
    		spinner.setAdapter(adapter);
    		spinner.setPrompt("�ȉ��̃��X�g���I�����ĉ������B");
    		//spinner.setSelection(1);

    		/* spinner���I�����ꂽ�Ƃ��̏��� */
    		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    			@Override
    			public void onItemSelected(AdapterView<?> parent,View view, int position, long id) {
    				Spinner spinner = (Spinner) parent;
    				String item = (String) spinner.getSelectedItem();
    				ecategory = item;
    				//Toast.makeText(entryActivity.this,String.format("%s���I������܂����B", item),Toast.LENGTH_SHORT).show();
    			}

    			@Override
    			public void onNothingSelected(AdapterView<?> parent) {
    				//Toast.makeText(entryActivity.this,"onNothingSelected", Toast.LENGTH_SHORT).show();
    			}
    		});
    		
    		locationManager.requestLocationUpdates(bestProvider, 0, 0, this);
			Log.e("�f�[�^�擾", "debug7");

            //String out = intent.getStringExtra("jp.dip.commonsense.testString");
        }
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		
		/* �d�v�FrequestLocationUpdates�����܂܃A�v�����I������Ƌ��������������Ȃ�B */
		locationManager.removeUpdates(this);
		locationDB.close();
	}
    
    @Override
	public void onClick(View v) {
		
		/* �{�^���������ɃL�[�{�[�h���\���ɂ��� */
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		
		locationManager.removeUpdates(this);
		
		//providerText.setText("");
		
		if (v.getId() == R.id.entry) {
			// List<String> providers = locationManager.getProviders(true);
			/* �g��for���𗘗p���Ĕz��̒��g������i�[ */
			/*for (String provider : providers) {
				locationManager.requestLocationUpdates(provider, 3600, 100, this);
				Log.e(provider, "debug");
			}*/	
			
			Log.v("���M�{�^������", "����");
			
			EditText nameEdit = (EditText) findViewById(R.id.entry_name);
			ename = nameEdit.getText().toString();
			Log.v(ename, "�n��");
			
			if(!ename.equals("")){
				
				/* �f�[�^�x�[�X�}������ */
				locationDB.beginTransaction();
				ContentValues val = new ContentValues();
				val.put("latitude", glat);
				val.put("longitude", glong);
				val.put("name", ename);
				val.put("category", ecategory);
				locationDB.insert("profileDB", "", val);
				locationDB.setTransactionSuccessful();
				locationDB.endTransaction();

				Log.e("�o�^����", "debug4");
				Toast.makeText(this, "�o�^����", Toast.LENGTH_LONG).show();
				
			} else {
				Toast.makeText(this, "�o�^������͂��Ă�������", Toast.LENGTH_LONG).show();
			}
						// resultsString = "����";
		}
	}

    /* ���P�[�V������񂪕ύX���ꂽ�Ƃ����s */
    @Override
    public void onLocationChanged(Location location) {
    	
    	Log.e("�ʒu���擾", "debug");
    	
    	providerText.setText(String.valueOf(location.getProvider()));
    	Log.v(String.valueOf(location.getProvider()), "debug");
    	
    	glat = String.valueOf(location.getLatitude());
		glong = String.valueOf(location.getLongitude());
		latText.setText(glat);
		logText.setText(glong);
		
		
		distanceBetween();
		httpRequest();
		
		Log.v("�L�ӈʒu�ł̍ŒZ����", String.valueOf(pDistance));
		Log.v("foursquare�ł̍ŒZ����", String.valueOf(hDistance));
		
		if (pDistance <= hDistance) {
			
			/* �L�ӈʒu�̒n���ƃJ�e�S�����i�[���� */
			name = pName;
			category = pCategory;
		} else {
			
			/* foursquare����擾�����n���ƃJ�e�S�����i�[ */
			name = hName;
			category = hCategory;
		}

		
		/* �e�L�X�g�r���[�ɏo�� */
		nameText.setText(name);
		categoryText.setText(category);
		
		locationManager.removeUpdates(this);
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
        case LocationProvider.AVAILABLE:
            Log.v("Status", "AVAILABLE");
            break;
        case LocationProvider.OUT_OF_SERVICE:
            Log.v("Status", "OUT_OF_SERVICE");
            break;
        case LocationProvider.TEMPORARILY_UNAVAILABLE:
            Log.v("Status", "TEMPORARILY_UNAVAILABLE");
            break;
        }
    }
    
    /* 2�_�Ԃ̋������v�Z */
	public void distanceBetween() {
		
		
		try {
			/* �N�G���̌��ʂ��擾 */
			Cursor cursor = locationDB.query("profileDB", columns_profile,
					null, null, null, null, "_id");

			/* �擾���ʂ𕶎��񌋍� */
			while (cursor.moveToNext()) {

				/* 2�_�Ԃ̋������v�Z */
				float[] results = new float[1];
				Location.distanceBetween(Double.valueOf(glat),
						Double.valueOf(glong), cursor.getDouble(1),
						cursor.getDouble(2), results);
				Log.v(String.valueOf(results[0]), "����");

				if (pDistance > results[0]) {
					pDistance = results[0];
					pName = cursor.getString(3);
					pCategory = cursor.getString(4);
				}
			}
		} catch (Exception e) {

			/* �G���[���� */
			Log.e("ERROR", e.toString());
			//Toast.makeText(this, "���s", Toast.LENGTH_LONG).show();
			/* text.setText("error!"); */
		}
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

			/* �擾����venue���X�g����venue�̐������O�ɏo�� */
			Log.e(String.valueOf(venuesArray.length()), "venue��");
			
			for (int i = 0; i < venuesArray.length(); i++) {
				JSONObject locationObject = venuesArray.getJSONObject(i).getJSONObject("location");
				JSONArray categoriesArray = venuesArray.getJSONObject(i).getJSONArray("categories");

				JSONObject bookObject[] = new JSONObject[2];

				try {
					bookObject[0] = venuesArray.getJSONObject(i);
					bookObject[1] = categoriesArray.getJSONObject(0);
					
					//Log.e("venue���X�g�{����", String.valueOf(i));
					if (hDistance > Integer.parseInt(locationObject.getString("distance"))) {
						hDistance = Integer.parseInt(locationObject.getString("distance"));
						id = i;
					}
				} catch (JSONException e) {
					// ��O����
					Log.e("ERROR", e.toString());
				}								
			}

			Log.e("�I������venue�̏���", String.valueOf(id));
			
			JSONArray categoriesArray = venuesArray.getJSONObject(id).getJSONArray("categories");

			Log.e("�I�u�W�F�N�g�̂����̂ڂ�", "debug5");
			JSONObject bookObject[] = new JSONObject[2];

			bookObject[0] = venuesArray.getJSONObject(id);
			bookObject[1] = categoriesArray.getJSONObject(0);

			Log.e("�I�u�W�F�N�g�̑��", "debug6");
			/* �n���̃f�[�^���擾 */
			hName = bookObject[0].getString("name");

			/* �J�e�S���̃f�[�^���擾 */
			hCategory = bookObject[1].getString("shortName");
			
			Log.e("�I������venue�̒n��", hName);
			Log.e("�I������venue�̃J�e�S��", hCategory);

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
			Cursor cursor = locationDB.query("profileDB", columns_profile, null, null,
					null, null, "_id");

			/* �擾���ʂ𕶎��񌋍� */
			while (cursor.moveToNext()) {
				out += cursor.getInt(0) + ":Latitude = ";
				out += cursor.getString(1) + "\n" + "   Longitude = ";
				out += cursor.getString(2) + "\n" + "   name = ";
				out += cursor.getString(3) + "\n" + "   category = ";
				out += cursor.getString(4) + "\n\n";
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
	
	/**
	 * �{�^���������ꂽ�Ƃ��Ăяo����� ���j���[�A�C�e�����쐬����
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		/* �I�v�V�������j���[���쐬���� */
		MENU_SELECT_A = menu.add(R.string.sDatabase);

		return true;
	}

	/**
	 * ���j���[���I�����ꂽ�Ƃ��Ăяo�����
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item == MENU_SELECT_A) {
			sDatabase();
		} 
		return false;
	}
}
