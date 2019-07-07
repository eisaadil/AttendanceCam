package com.eisaadil.attendancecam;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eisaadil.attendancecam.DB.AppDatabase;
import com.eisaadil.attendancecam.DB.Student;
import com.eisaadil.attendancecam.Utilities.ImagePicker;
import com.google.gson.Gson;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.pixplicity.easyprefs.library.Prefs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class TakeAttendance extends AppCompatActivity {

    private static final int PICK_IMAGE_ID = 200;
    private ImageView takenImage;

    TextView resultText;
    String personGroupId;

    ListView identifiedStudentsListView;

    boolean isFirstAttendance = true;
    List<Student> identifiedStudents;

    boolean imageSelected = false;

    List<String> studentIdAttendanceIncremented = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_attendance);

        if (android.os.Build.VERSION.SDK_INT > 9){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //ab.setSubtitle("Click on the picture to keep adding students");
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(100);

        resultText = findViewById(R.id.resultText);

        takenImage = findViewById(R.id.takenImage);
        (findViewById(R.id.takenImage)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.takeAttendanceProgress).setVisibility(View.VISIBLE);
                identifiedStudentsListView.setVisibility(View.GONE);

                Intent chooseImageIntent = ImagePicker.getPickImageIntent(getApplicationContext(), getString(R.string.pick_image_intent_text));
                startActivityForResult(chooseImageIntent, PICK_IMAGE_ID);
            }
        });

        identifiedStudentsListView = findViewById(R.id.identifiedStudentsListView);

    }

    @Override
    protected void onStart() {
        super.onStart();

        personGroupId = Prefs.getString("courseId", "");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case PICK_IMAGE_ID:
                final Bitmap bitmap = ImagePicker.getImageFromResult(this, resultCode, data);
                if (bitmap!=null) {
                    imageSelected = true;

                    if (isFirstAttendance)
                        identifiedStudents = new ArrayList<>();

                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

                    takenImage.setImageBitmap(bitmap);

                    new DetectionTask().execute(inputStream);


                    isFirstAttendance = false;
                }
                else{
                    imageSelected = false;
                    takenImage.setImageDrawable(getDrawable(R.drawable.attendance_logo));

                    findViewById(R.id.takeAttendanceProgress).setVisibility(View.GONE);
                    identifiedStudentsListView.setVisibility(View.VISIBLE);

                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    List<UUID> faceIds;

    private class DetectionTask extends AsyncTask<InputStream, Void, Face[]> {
        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try{

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        null);
            }  catch (Exception e) {
                e.printStackTrace();
                Log.d("LOOK",e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Face[] faces) {

            if (faces != null) {
                if (faces.length == 0) {
                    Log.d("","No faces detected!");
                    Toast.makeText(TakeAttendance.this, "No faces detected in the picture", Toast.LENGTH_SHORT).show();

                    findViewById(R.id.takeAttendanceProgress).setVisibility(View.GONE);
                    identifiedStudentsListView.setVisibility(View.VISIBLE);
                    takenImage.setImageDrawable(getDrawable(R.drawable.attendance_logo));
                } else {
                    faceIds = new ArrayList<>();
                    for (Face face:  faces) {
                        faceIds.add(face.faceId);
                    }

                    new TrainPersonGroupTask().execute(personGroupId);
                }
            } else {
                Toast.makeText(TakeAttendance.this, "No faces detected in the picture", Toast.LENGTH_SHORT).show();

                findViewById(R.id.takeAttendanceProgress).setVisibility(View.GONE);
                identifiedStudentsListView.setVisibility(View.VISIBLE);
                takenImage.setImageDrawable(getDrawable(R.drawable.attendance_logo));
            }
        }
    }

    class TrainPersonGroupTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {

            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try {
                publishProgress("Training person group...");

                faceServiceClient.trainLargePersonGroup(params[0]);
                return params[0];
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Train", e.toString()+" "+e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (s==null){
                findViewById(R.id.takeAttendanceProgress).setVisibility(View.GONE);
                Toast.makeText(TakeAttendance.this, "The Person Group could not be trained", Toast.LENGTH_SHORT).show();
                takenImage.setImageDrawable(getDrawable(R.drawable.attendance_logo));
            }else {
                new IdentificationTask().execute(faceIds.toArray(new UUID[faceIds.size()]));
            }
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(TakeAttendance.this, Menu.class));
    }

    StudentListAdapter studentListAdapter;

    private class IdentificationTask extends AsyncTask<UUID, Void, IdentifyResult[]> {
        @Override
        protected IdentifyResult[] doInBackground(UUID... params) {
            Log.d("", "Request: Identifying faces ");

            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try{

                TrainingStatus trainingStatus = faceServiceClient.getLargePersonGroupTrainingStatus(personGroupId);

                if (!trainingStatus.status.toString().equals("Succeeded")) {
                    return null;
                }
                System.out.println("PERSON GROUP ID: "+personGroupId);
                return faceServiceClient.identityInLargePersonGroup(
                        personGroupId,     /* personGroupId */
                        params,                  /* faceIds */
                        1);                      /* maxNumOfCandidatesReturned */
            }  catch (Exception e) {
                Log.d("",e.getMessage());
                e.printStackTrace();
                System.out.println("Identification exception"+e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(IdentifyResult[] identifyResults) {
            takenImage.setImageDrawable(getDrawable(R.drawable.attendance_logo));
            if (identifyResults!=null) {
                String logString = "Response: Success. ";
                List<String> personIdsOfIdentified = new ArrayList<>();

                int numberOfUnidentifiedFaces = 0;

                for (IdentifyResult identifyResult : identifyResults) {
                    if (!identifyResult.candidates.isEmpty())
                        personIdsOfIdentified.add(identifyResult.candidates.get(0).personId.toString());

                    if (identifyResult.candidates.size() == 0){
                        numberOfUnidentifiedFaces++;
                    }

                    logString += "Face " + identifyResult.faceId.toString() + " is identified as "
                            + (identifyResult.candidates.size() > 0
                            ? identifyResult.candidates.get(0).personId.toString()
                            : "Unknown Person")
                            + ". ";
                }

                if (numberOfUnidentifiedFaces > 0)
                    Toast.makeText(TakeAttendance.this, numberOfUnidentifiedFaces+" face(s) cannot be recognized", Toast.LENGTH_SHORT).show();

                Log.d("", logString);


                AppDatabase db = AppDatabase.getAppDatabase(getApplicationContext());
                for (String personId : personIdsOfIdentified) {
                    identifiedStudents.add(db.studentDao().getStudentFromId(personId));
                }

                Set<Student> hs = new HashSet<>(identifiedStudents);
                identifiedStudents.clear();
                identifiedStudents.addAll(hs);

                for (Student identifiedStudent: identifiedStudents){
                    if (identifiedStudent.studentId==null) continue;
                    if (!studentIdAttendanceIncremented.contains(identifiedStudent.studentId))
                        db.attendanceDao().incrementAttendance(identifiedStudent.courseId, identifiedStudent.regNo);
                    studentIdAttendanceIncremented.add(identifiedStudent.studentId);
                }

                studentListAdapter = new StudentListAdapter(TakeAttendance.this, R.layout.list_identified_students_row, identifiedStudents);


                identifiedStudentsListView.setAdapter(studentListAdapter);

                findViewById(R.id.takeAttendanceProgress).setVisibility(View.GONE);
                identifiedStudentsListView.setVisibility(View.VISIBLE);
            }
            else{
                Toast.makeText(TakeAttendance.this, "No faces found in the picture. Try Again.", Toast.LENGTH_SHORT).show();

                findViewById(R.id.takeAttendanceProgress).setVisibility(View.GONE);
                identifiedStudentsListView.setVisibility(View.VISIBLE);
                takenImage.setImageDrawable(getDrawable(R.drawable.attendance_logo));
            }

        }
    }

    public class StudentListAdapter extends ArrayAdapter<Student> {

        private Context context;

        public StudentListAdapter(Activity context, int resource, List<Student> students) {
            super(context, resource, students);
            this.context = context;
        }


        @Override
        public long getItemId(int i) {
            return 0;
        }


        public View getView(int position, View convertView, ViewGroup parent) {

            final Student student = getItem(position);

            if (convertView == null) {

                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_identified_students_row, parent, false);
            }


            TextView studentName = convertView.findViewById(R.id.studentName);
            CircleImageView studentFaceImage = convertView.findViewById(R.id.studentFaceImage);
            TextView studentRegNo = convertView.findViewById(R.id.studentRegNo);

            final TextView attendanceText = convertView.findViewById(R.id.attendanceText);
            TextView maxAttendanceText = convertView.findViewById(R.id.maxAttendanceText);
            final CircleImageView decrementAttendanceButton = convertView.findViewById(R.id.decrementAttendanceButton);

            assert student != null;
            studentName.setText(student.studentName);
            studentRegNo.setText(student.regNo);

            AppDatabase db = AppDatabase.getAppDatabase(getApplicationContext());
            final int attendanceNumber = db.attendanceDao().getAttendance(student.courseId, student.regNo).attendanceNumber;
            int maxAttendance = db.courseDao().getNumberOfClasses(student.courseId);

            attendanceText.setText(""+attendanceNumber);
            maxAttendanceText.setText("/"+maxAttendance);

            decrementAttendanceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AppDatabase db = AppDatabase.getAppDatabase(getApplicationContext());
                    db.attendanceDao().decrementAttendance(student.courseId, student.regNo);

                    attendanceText.setText(""+ (attendanceNumber-1));
                    attendanceText.setTextColor(Color.RED);
                    decrementAttendanceButton.setVisibility(View.INVISIBLE);
                }
            });

            String[] faceIDs = (new Gson()).fromJson(student.faceArrayJson, String[].class);

            if (faceIDs.length != 0) {
                String photoPath = Environment.getExternalStorageDirectory() + "/Faces/" + faceIDs[0] + ".jpg"; //take first faceId image /storage/emulated/0/7a677caf-1ece-47af-a771-857f979cd241.jpg
                if (!(new File(photoPath).exists())){
                    studentFaceImage.setImageResource(R.drawable.person_icon);
                }
                else{
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 8;
                    final Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);
                    studentFaceImage.setImageBitmap(bitmap);
                }
            }

            return convertView;

        }
    }


}
