package jp.dip.commonsense;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class categoryDB extends SQLiteOpenHelper {
	public categoryDB(Context context){
		super(context,"categoryDB.db",null,1);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO �����������ꂽ���\�b�h�E�X�^�u
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO �����������ꂽ���\�b�h�E�X�^�u
		// �f�[�^�x�[�X�̃A�b�v�O���[�h
		// �����ł́A�e�[�u������蒼�������Ă��܂�
		db.execSQL("drop table if exists " + "categoryDB");
		onCreate(db);
	}
}
