package tbc.techbytecare.kk.uploadimagephp;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import tbc.techbytecare.kk.uploadimagephp.Remote.IUploadAPI;
import tbc.techbytecare.kk.uploadimagephp.Remote.RetrofitClient;
import tbc.techbytecare.kk.uploadimagephp.Remote.UploadCallbacks;
import tbc.techbytecare.kk.uploadimagephp.Utils.ProgressRequestBody;

public class MainActivity extends AppCompatActivity implements UploadCallbacks {

    public static final String BASE_URL = "http://10.0.2.2/";
    private static final int REQUEST_PERMISSION_CODE = 1000;
    private static final int PICK_FILE_REQUEST_CODE = 1001;

    IUploadAPI mService;
    Button btnUpload;
    ImageView imageView;

    Uri selectedFileUri;

    ProgressDialog dialog;

    private IUploadAPI getAPIUpload()   {

        return RetrofitClient.getClient(BASE_URL).create(IUploadAPI.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mService = getAPIUpload();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE
            },REQUEST_PERMISSION_CODE);
        }

        btnUpload = findViewById(R.id.btnUpload);
        imageView = findViewById(R.id.imageView);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFile();
            }
        });
    }

    private void uploadFile() {
        if (selectedFileUri != null)    {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMessage("Uploading....");
            dialog.setIndeterminate(false);
            dialog.setMax(100);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();

            File file = FileUtils.getFile(this,selectedFileUri);
            ProgressRequestBody requestFile = new ProgressRequestBody(file,this);

            final MultipartBody.Part body = MultipartBody.Part.createFormData("uploaded_file",file.getName(),requestFile);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    mService.uploadFile(body)
                            .enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, Response<String> response) {
                                    dialog.dismiss();
                                    Toast.makeText(MainActivity.this, "Uploaded...", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {
                                    dialog.dismiss();
                                    Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }).start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK)   {

            if (requestCode == PICK_FILE_REQUEST_CODE)  {

                if (data != null)   {

                    selectedFileUri = data.getData();

                    if (selectedFileUri != null && !selectedFileUri.getPath().isEmpty())    {
                        imageView.setImageURI(selectedFileUri);
                    }
                    else {
                        Toast.makeText(this, "Can't Upload image to server", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)    {

            case REQUEST_PERMISSION_CODE :
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)    {
                    Toast.makeText(this, "Permission Granted...", Toast.LENGTH_SHORT).show();
                }
                else    {
                    Toast.makeText(this, "Permission Denied...", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void chooseFile() {
        Intent intent = Intent.createChooser(FileUtils.createGetContentIntent(),"Select a file");
        startActivityForResult(intent,PICK_FILE_REQUEST_CODE);
    }

    @Override
    public void onProgressUpdate(int percentage) {
        dialog.setProgress(percentage);
    }
}
