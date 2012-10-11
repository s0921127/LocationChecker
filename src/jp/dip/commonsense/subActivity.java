package jp.dip.commonsense;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class subActivity extends Activity {
	 /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sub);
 
        Intent intent = getIntent();
        if(intent != null){
        	TextView text = (TextView) findViewById(R.id.text);
        	
            String out = intent.getStringExtra("jp.dip.commonsense.testString");
            // データベースの内容を出力
			text.setText(out);
        }
    }
}
