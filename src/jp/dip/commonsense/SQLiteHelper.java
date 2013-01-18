package jp.dip.commonsense;

import java.io.File;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

public class SQLiteHelper {

	/* �f�[�^�x�[�X��u��SD��̃t�H���_�̃p�X */
    private static final String DB_DIRECTORY =
        Environment.getExternalStorageDirectory() +
        "/sqlitefile";
     
    /* �f�[�^�x�[�X�t�@�C���̃p�X */
    private static final String DB_NAME =
        DB_DIRECTORY + "/locationDB.db";

    
    /** �e�[�u���쐬��SQL�� */
    private String CREATE_TABLE_PROFILE =
        "create table profileDB (" +
        		"_id integer primary key autoincrement," + 
        		"Latitude text not null," + 
        		"Longitude text not null," + 
        		"name text not null," + 
        		"category text not null" + 
        		");";
    

    /* �R�l�N�V�����̐��� */																				
    public SQLiteDatabase openConnection() throws SQLException {

        if (new File(DB_DIRECTORY).exists() == false) {
            new File(DB_DIRECTORY).mkdir();
        }

        SQLiteDatabase db = null;
        db = SQLiteDatabase.openOrCreateDatabase(DB_NAME, null);
       
        try {
            db.execSQL(CREATE_TABLE_PROFILE);
        } catch (Exception e) {
            
        }
        return db;
    }

}
