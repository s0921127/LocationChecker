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
	
	/* データベース利用の設定 */
	SQLiteDatabase locationDB = null;
	
	private String glat;          /* 経度 */
	private String glong;         /* 緯度 */
	private String ename;         /* 登録地名 */
	private String ecategory;     /* 登録カテゴリ */	
	private String name;          /* 地名 */
	private String category;      /* カテゴリ */	
	
	private LocationManager locationManager;
	
	private MenuItem MENU_SELECT_A; /* データベース出力用ボタン */
	
	private static final int DISTANCE_MIN = 25;
	
	private TextView logText;          /* 経度のテキストビュー */
	private TextView latText;          /* 緯度のテキストビュー */
	private TextView providerText;     /* プロバイダのテキストビュー */
	private TextView nameText;         /* 地名のテキストビュー */
	private TextView categoryText;     /* カテゴリのテキストビュー */
	
	private String bestProvider;
	
	private String[] columns_profile = { "_id","Latitude", "Longitude", "name", "category" };


	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry);
 
        Intent intent = getIntent();
        
        /* staticブロックなど、アプリケーション開始前に実行する。 */
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        
        /* DBコネクション生成	 */																						
        locationDB = new SQLiteHelper().openConnection();
        
        ((Button) findViewById(R.id.entry)).setOnClickListener(this);
        
        /* テキストビューの設定 */
		logText = (TextView) findViewById(R.id.longitude);
		latText = (TextView) findViewById(R.id.latitude);
		providerText = (TextView) findViewById(R.id.provider);
		nameText = (TextView) findViewById(R.id.name);
		categoryText = (TextView) findViewById(R.id.category);
        
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        Criteria criteria = new Criteria();
		// PowerRequirement は設定しないのがベストプラクティス
		// Accuracy は設定しないのがベストプラクティス
		// criteria.setAccuracy(Criteria.ACCURACY_FINE); ← Accuracy
		// で最もやってはいけないパターン
		// 以下は必要により
		criteria.setBearingRequired(false); // 方位不要
		criteria.setSpeedRequired(false); // 速度不要
		criteria.setAltitudeRequired(false); // 高度不要
		bestProvider = locationManager.getBestProvider(criteria, true);
		Location locate = locationManager.getLastKnownLocation(bestProvider);

		if(locate == null){  
			 // 現在地が取得できなかった場合，無線測位で取得してみる  
			 locate = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);  
			 bestProvider = LocationManager.NETWORK_PROVIDER;
		}
		
		if(locate == null){  
			// 現在地が取得できなかった場合，GPSで取得してみる  
			locate = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); 
			bestProvider = LocationManager.GPS_PROVIDER;
		} 

        if(intent != null){
        	
        	/* spinnerの設定・項目登録 */
    		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
    		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // アイテムを追加します
            adapter.add("Home");
            adapter.add("University");
            adapter.add("work");
            adapter.add("other");
            
            /* spinnerの表示 */
            Spinner spinner = (Spinner) findViewById(R.id.spinner);
    		spinner.setAdapter(adapter);
    		spinner.setPrompt("以下のリストより選択して下さい。");
    		//spinner.setSelection(1);

    		/* spinnerが選択されたときの処理 */
    		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    			@Override
    			public void onItemSelected(AdapterView<?> parent,View view, int position, long id) {
    				Spinner spinner = (Spinner) parent;
    				String item = (String) spinner.getSelectedItem();
    				ecategory = item;
    				//Toast.makeText(entryActivity.this,String.format("%sが選択されました。", item),Toast.LENGTH_SHORT).show();
    			}

    			@Override
    			public void onNothingSelected(AdapterView<?> parent) {
    				//Toast.makeText(entryActivity.this,"onNothingSelected", Toast.LENGTH_SHORT).show();
    			}
    		});
    		
    		locationManager.requestLocationUpdates(bestProvider, 0, 0, this);
			Log.e("データ取得", "debug7");

            //String out = intent.getStringExtra("jp.dip.commonsense.testString");
        }
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		
		/* 重要：requestLocationUpdatesしたままアプリを終了すると挙動がおかしくなる。 */
		locationManager.removeUpdates(this);
		locationDB.close();
	}
    
    @Override
	public void onClick(View v) {
		
		/* ボタン押下時にキーボードを非表示にする */
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		
		locationManager.removeUpdates(this);
		
		//providerText.setText("");
		
		if (v.getId() == R.id.entry) {
			// List<String> providers = locationManager.getProviders(true);
			/* 拡張for文を利用して配列の中身を一つずつ格納 */
			/*for (String provider : providers) {
				locationManager.requestLocationUpdates(provider, 3600, 100, this);
				Log.e(provider, "debug");
			}*/	
			
			Log.v("送信ボタン押下", "成功");
			
			EditText nameEdit = (EditText) findViewById(R.id.entry_name);
			ename = nameEdit.getText().toString();
			Log.v(ename, "地名");
			
			if(!ename.equals("")){
				
				/* データベース挿入処理 */
				locationDB.beginTransaction();
				ContentValues val = new ContentValues();
				val.put("latitude", glat);
				val.put("longitude", glong);
				val.put("name", ename);
				val.put("category", ecategory);
				locationDB.insert("profileDB", "", val);
				locationDB.setTransactionSuccessful();
				locationDB.endTransaction();

				Log.e("登録完了", "debug4");
				Toast.makeText(this, "登録完了", Toast.LENGTH_LONG).show();
				
			} else {
				Toast.makeText(this, "登録名を入力してください", Toast.LENGTH_LONG).show();
			}
						// resultsString = "成功";
		}
	}

    /* ロケーション情報が変更されたとき実行 */
    @Override
    public void onLocationChanged(Location location) {
    	
    	Log.e("位置情報取得", "debug");
    	
    	providerText.setText(String.valueOf(location.getProvider()));
    	Log.v(String.valueOf(location.getProvider()), "debug");
    	
    	glat = String.valueOf(location.getLatitude());
		glong = String.valueOf(location.getLongitude());
		latText.setText(glat);
		logText.setText(glong);
		
		double distance = 0;
		
		distance = distanceBetween();
		
		Log.v("距離", String.valueOf(distance));
		
		if(DISTANCE_MIN == distance){
			httpRequest();
		}
		
		/* テキストビューに出力 */
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
    
    /* 2点間の距離を計算 */
	public double distanceBetween() {
		
		double distance = DISTANCE_MIN;
		
		try {
			/* クエリの結果を取得 */
			Cursor cursor = locationDB.query("profileDB", columns_profile,
					null, null, null, null, "_id");

			/* 取得結果を文字列結合 */
			while (cursor.moveToNext()) {

				Log.v(String.valueOf(Double.valueOf(glat)), "現在の緯度");
				Log.v(String.valueOf(Double.valueOf(glong)), "現在の経度");
				Log.v(String.valueOf(cursor.getDouble(1)), "補正の緯度");
				Log.v(String.valueOf(cursor.getDouble(2)), "補正の経度");

				/* 2点間の距離を計算 */
				float[] results = new float[1];
				Location.distanceBetween(Double.valueOf(glat),
						Double.valueOf(glong), cursor.getDouble(1),
						cursor.getDouble(2), results);
				Log.v(String.valueOf(results[0]), "距離");

				if (distance > results[0]) {
					distance = results[0];
					name = cursor.getString(3);
					category = cursor.getString(4);
				}
			}
		} catch (Exception e) {

			/* エラー処理 */
			Log.e("ERROR", e.toString());
			//Toast.makeText(this, "失敗", Toast.LENGTH_LONG).show();
			/* text.setText("error!"); */
		}
		
		return distance;
	}
    
    /* foursquare APIにURLでリクエストし，地名とカテゴリを取得 */
	public void httpRequest() {

		/* android.text.format.Timeクラスでの現在日時 */
		Time time = new Time();
		time.setToNow();
		String tMonth = String.valueOf((time.month + 1));
		if (10 > time.month + 1) {
			tMonth = "0" + tMonth;
		}

		/* staticブロックなど、アプリケーション開始前に実行する。 */
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

		Log.e("URL作成", "debug2");

		HttpClient httpClient = new DefaultHttpClient();
		HttpParams params = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, 1000);
		HttpConnectionParams.setSoTimeout(params, 1000);

		HttpUriRequest httpRequest = new HttpGet(uri);

		HttpResponse httpResponse = null;

		try {
			Log.e("レスポンス作成開始", "debug11");
			httpResponse = httpClient.execute(httpRequest);
			if (httpResponse != null)
				Log.e("レスポンス作成", "debug10");
			else
				Log.e("レスポンス作成失敗", "debug10");
		} catch (ClientProtocolException e) {
			/* 例外処理 */
			Log.e("レスポンス作成失敗", "debug13");
		} catch (IOException e) {
			/* 例外処理 */
			Log.e("レスポンス作成失敗", "debug13");
		}

		String json = null;

		if (httpResponse != null
				&& httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			HttpEntity httpEntity = httpResponse.getEntity();
			try {
				Log.e("JSONデータの取得", "debug9");
				json = EntityUtils.toString(httpEntity);
			} catch (ParseException e) {
				/* 例外処理 */
			} catch (IOException e) {
				/* 例外処理 */
			} finally {
				try {
					httpEntity.consumeContent();
				} catch (IOException e) {
					/* 例外処理 */
				}
			}
		}

		httpClient.getConnectionManager().shutdown();

		Log.e("HTTP通信終了", "debug3");

		try {
			Log.e("導入", "debug8");
			JSONObject rootObject = new JSONObject(json);
			JSONObject responseObject = rootObject.getJSONObject("response");
			JSONArray venuesArray = responseObject.getJSONArray("venues");

			int id = 0; /* 訪れたユーザの数の一番多いvenueの順番 */
			int distance = 10000; /* 現在地からvenueまでの距離 */

			/* 取得したvenueリスト中のvenueの数をログに出力 */
			Log.e(String.valueOf(venuesArray.length()), "venue数");

			/* 取得したvenueリストのvenueの中で距離の一番近いvenueを検索 */
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

			Log.e("オブジェクトのさかのぼり", "debug5");
			JSONObject bookObject[] = new JSONObject[2];

			bookObject[0] = venuesArray.getJSONObject(id);
			bookObject[1] = categoriesArray.getJSONObject(0);

			Log.e("オブジェクトの代入", "debug6");
			/* 地名のデータを取得 */
			name = bookObject[0].getString("name");

			/* カテゴリのデータを取得 */
			category = bookObject[1].getString("shortName");

			Log.e("データ取得", "debug7");

			// resultsString = "成功";
		} catch (Exception e) {
			/* 例外処理 */
			// resultsString = "失敗";
			Log.e("ERROR", e.toString());
		}
	}
	
	/* データベース取得 */
	public void sDatabase() {
		String out = "";
		try {
			/* クエリの結果を取得 */
			Cursor cursor = locationDB.query("profileDB", columns_profile, null, null,
					null, null, "_id");

			/* 取得結果を文字列結合 */
			while (cursor.moveToNext()) {
				out += cursor.getInt(0) + ":Latitude = ";
				out += cursor.getString(1) + "\n" + "   Longitude = ";
				out += cursor.getString(2) + "\n" + "   name = ";
				out += cursor.getString(3) + "\n" + "   category = ";
				out += cursor.getString(4) + "\n\n";
			}

			/* データベースの内容を出力 */
			Intent intent = new Intent();
			intent.setClassName("jp.dip.commonsense",
					"jp.dip.commonsense.subActivity");
			intent.putExtra("jp.dip.commonsense.testString", out);

			startActivity(intent);
		} catch (Exception e) {

			/* エラー処理 */
			Log.e("ERROR", e.toString());
			Toast.makeText(this, "失敗", Toast.LENGTH_LONG).show();
			/* text.setText("error!"); */
		}
	}
	
	/**
	 * ボタンが押されたとき呼び出される メニューアイテムを作成する
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		/* オプションメニューを作成する */
		MENU_SELECT_A = menu.add(R.string.sDatabase);

		return true;
	}

	/**
	 * メニューが選択されたとき呼び出される
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item == MENU_SELECT_A) {
			sDatabase();
		} 
		return false;
	}
}
