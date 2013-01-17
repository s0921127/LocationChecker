package jp.dip.commonsense;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class entryActivity extends Activity implements LocationListener, View.OnClickListener {
	
	/* �f�[�^�x�[�X���p�̐ݒ� */
	SQLiteDatabase locationDB = null;
	
	String glat;         /* �o�x */
	String glong;        /* �ܓx */
	String name;         /* �n�� */
	String category;     /* �J�e�S�� */
	
	private LocationManager locationManager;
	private String bestProvider;

	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry);
 
        Intent intent = getIntent();
        
        /* DB�R�l�N�V��������	 */																						
        locationDB = new SQLiteHelper().openConnection();
        
        ((Button) findViewById(R.id.entry)).setOnClickListener(this);
        
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
        bestProvider = locationManager.getBestProvider(criteria, true);
        

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
    				category = item;
    				Toast.makeText(entryActivity.this,String.format("%s���I������܂����B", item),Toast.LENGTH_SHORT).show();
    			}

    			@Override
    			public void onNothingSelected(AdapterView<?> parent) {
    				Toast.makeText(entryActivity.this,"onNothingSelected", Toast.LENGTH_SHORT).show();
    			}
    		});

            //String out = intent.getStringExtra("jp.dip.commonsense.testString");
        }
    }
    
    @Override
	public void onClick(View v) {
		
		/* �{�^���������ɃL�[�{�[�h���\���ɂ��� */
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		
		locationManager.removeUpdates(this);
		
		if (v.getId() == R.id.entry) {
			locationManager.requestLocationUpdates(bestProvider, 0, 0, this);

			Log.e("�f�[�^�擾", "debug7");
			// resultsString = "����";
		}
	}

    /* ���P�[�V������񂪕ύX���ꂽ�Ƃ����s */
    @Override
    public void onLocationChanged(Location location) {
    	
    	Log.e("�ʒu���擾", "debug");
    	
    	glat = String.valueOf(location.getLatitude());
		glong = String.valueOf(location.getLongitude());
		
		EditText nameEdit = (EditText) findViewById(R.id.name);
		String name = nameEdit.getText().toString();
		
		/* �f�[�^�x�[�X�}������ */
		locationDB.beginTransaction();
		ContentValues val = new ContentValues();
		val.put("latitude", glat);
		val.put("longitude", glong);
		val.put("name", name);
		val.put("category", category);
		locationDB.insert("profileDB", "", val);
		locationDB.setTransactionSuccessful();
		locationDB.endTransaction();

		Log.e("�f�[�^�x�[�X�i�[", "debug4");
		
        Log.v("----------", "----------");
        Log.v("Latitude", String.valueOf(location.getLatitude()));
        Log.v("Longitude", String.valueOf(location.getLongitude()));
        Log.v("Accuracy", String.valueOf(location.getAccuracy()));
        Log.v("Altitude", String.valueOf(location.getAltitude()));
        Log.v("Time", String.valueOf(location.getTime()));
        Log.v("Speed", String.valueOf(location.getSpeed()));
        Log.v("Bearing", String.valueOf(location.getBearing()));
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
}
