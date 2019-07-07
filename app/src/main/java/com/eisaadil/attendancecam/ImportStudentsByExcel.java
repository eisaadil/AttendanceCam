package com.eisaadil.attendancecam;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.opencsv.CSVReader;
import com.pixplicity.easyprefs.library.Prefs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImportStudentsByExcel extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_students_by_excel);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(100);
        //ab.setSubtitle("Follow the steps");

        (findViewById(R.id.importCSV)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<StudentDataCSV> studentDataCSVList = new ArrayList<>();

                try{
                    CSVReader reader = new CSVReader(new FileReader(Environment.getExternalStorageDirectory()+"/StudentFaces.csv"));
                    List<String[]> myEntries = reader.readAll();
                    for (String[] s: myEntries){
                        Log.d("CSV data:",s[0]+" "+s[1]);

                        StudentDataCSV studentDataCSV = new StudentDataCSV(s[0],s[1]);
                        studentDataCSVList.add(studentDataCSV);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(ImportStudentsByExcel.this, "The specified file was not found", Toast.LENGTH_SHORT).show();
                }

                for (StudentDataCSV studentData: studentDataCSVList){
                    File imageSrc = new File(Environment.getExternalStorageDirectory()+"/CSVImages/"+studentData.regNo+".jpg");
                    if (imageSrc.exists())
                        new AddPersonTask().execute(Prefs.getString("courseId", ""), studentData.studentName, studentData.regNo);
                    else
                        Toast.makeText(ImportStudentsByExcel.this, "The image file could not be located for "+studentData.studentName, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ImportStudentsByExcel.this, AddStudent.class));
    }



    public class StudentDataCSV {
        public String studentName;
        public String regNo;

        public StudentDataCSV(String studentName, String regNo) {
            this.studentName = studentName;
            this.regNo = regNo;
        }
    }

    // Background task of adding a person to person group.
    class AddPersonTask extends AsyncTask<String, String, String> {

        String regNo;

        @Override
        protected String doInBackground(String... params) {
            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try{
                publishProgress("Syncing with server to add person...");
                Log.v("","Request: Creating Person in person group" + params[0]);

                // Start the request to creating person.
                CreatePersonResult createPersonResult = faceServiceClient.createPersonInLargePersonGroup(
                        params[0], //personGroupID - courseId
                        params[1], //studentName
                        params[2]); //userData or regNo
                regNo = params[2];
                return createPersonResult.personId.toString();

            } catch (Exception e) {
                publishProgress(e.getMessage());
                Log.v("",e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String personId) {

            if (personId != null) {
                Log.v("","Response: Success. Person " + personId + " created.");

                Toast.makeText(ImportStudentsByExcel.this, "Person with personId "+personId+" successfully created", Toast.LENGTH_SHORT).show();

                new AddFaceTask().execute(personId, regNo); //personId, regNo
            }
        }
    }

    class AddFaceTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try{
                Log.v("", "Adding face...");
                UUID personId = UUID.fromString(params[0]);
                String regNo = params[1];
                File image = new File(Environment.getExternalStorageDirectory()+"/CSVImages/"+regNo+".jpg");

                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(),bmOptions);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());

                AddPersistedFaceResult result = faceServiceClient.addPersonFaceInLargePersonGroup(
                        Prefs.getString("courseId", ""),
                        personId,
                        imageInputStream,
                        "",
                        null);


                File folder = new File(Environment.getExternalStorageDirectory(), "/Faces/");
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                File photo = new File(Environment.getExternalStorageDirectory(), "/Faces/"+result.persistedFaceId.toString()+".jpg");
                if (photo.exists()) {
                    photo.delete();
                }

                try {
                    FileOutputStream fos= new FileOutputStream(photo.getPath());

                    fos.write(stream.toByteArray());
                    fos.close();

                    Log.v("Store face in storage", "Face stored with name "+photo.getName()+" and path "+photo.getAbsolutePath());
                }
                catch (java.io.IOException e) {
                    Log.e("Store face in storage", "Exception in photoCallback", e);
                }

                return result.persistedFaceId.toString();

            } catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            }
        }

        @Override
        protected void onPostExecute(String persistedFaceId) {
            Log.v("", "Successfully added face with persistence id "+persistedFaceId);

            Toast.makeText(ImportStudentsByExcel.this, "Face with persistedFaceId "+persistedFaceId+" successfully created", Toast.LENGTH_SHORT).show();
        }
    }
}
