package jp.dip.commonsense;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class serverDB extends SQLiteOpenHelper {
	public serverDB(Context context){
		super(context,"serverDB.db",null,1);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO 自動生成されたメソッド・スタブ
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO 自動生成されたメソッド・スタブ
		// データベースのアップグレード
		// ここでは、テーブルを作り直しをしています
		db.execSQL("drop table if exists " + "test");
		onCreate(db);
	}
}