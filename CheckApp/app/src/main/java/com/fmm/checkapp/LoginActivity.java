package com.fmm.checkapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    Button btLogin;
    TextView tvIrTelaCadastro, tvEsqueceuSenha;
    EditText edtEmail;
    EditText edtSenha;
    private FirebaseAuth auth;
    FirebaseUser firebaseUser;
    ProgressBar progressBar;
    static Usuario user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setObjects(); //Set all objects from LoginActivity Class
        btLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user.setEmail(edtEmail.getText().toString());
                user.setSenha(edtSenha.getText().toString());

                if (user.getEmail().isEmpty() || user.getSenha().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Os campos de email e senha são obrigatórios",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String email = user.getEmail();
                    String senha = user.getSenha();
                    progressBar.setVisibility(View.VISIBLE);
                    login(email, senha);
                }
            }
        });

        tvIrTelaCadastro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        tvEsqueceuSenha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(LoginActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.dialog_password_recovery, null);
                final EditText edtEmail = (EditText) mView.findViewById(R.id.dialog_password_recovery_edt_email);
                Button btnConfirma = (Button) mView.findViewById(R.id.dialog_password_recovery_bt_confirma);
                mBuilder.setView(mView);
                final AlertDialog dialog = mBuilder.create();
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


                btnConfirma.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!edtEmail.getText().toString().isEmpty() && edtEmail.getText().toString().contains("@")){
                            auth.sendPasswordResetEmail(edtEmail.getText().toString())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(getApplicationContext(), "Email para redefinição enviado!(Confira a caixa de spam)", Toast.LENGTH_LONG).show();
                                                dialog.dismiss();
                                            } else
                                                Toast.makeText(getApplicationContext(), "Informe um email válido", Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }

                    }
                });

                dialog.show();

            }
        });
    }

    private void setObjects() {
        user = new Usuario();
        btLogin = findViewById(R.id.activity_login_bt_login);
        tvEsqueceuSenha = findViewById(R.id.activity_login_tv_esqueceu_senha);
        tvIrTelaCadastro = findViewById(R.id.activity_login_tv_cadastrar);
        edtEmail = findViewById(R.id.activity_login_edt_email);
        edtSenha = findViewById(R.id.activity_login_edt_senha);
        progressBar = findViewById(R.id.activity_login_progressBar);
        auth = FirebaseAuth.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void login(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            progressBar.setVisibility(View.GONE);
                            finish();
                        } else {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(LoginActivity.this, "Email e/ou senha incorretos",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /*
    @Override
    protected void onStart() {                  FUNÇÃO PARA PEGAR O USUÁRIO JÁ LOGADO!
        if(firebaseUser != null){
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
        }
        super.onStart();
    }*/
}
