package com.fmm.checkapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fmm.checkapp.model.Event;
import com.fmm.checkapp.model.MyRecyclerViewAdapter;
import com.fmm.checkapp.firebasemodel.Events;
import com.fmm.checkapp.firebasemodel.Keys;
import com.fmm.checkapp.firebasemodel.Professores;
import com.fmm.checkapp.firebasemodel.Students;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HomeActivity extends Activity {

    private static final int TIME_EMERGENCY = 4 ;
    RecyclerView recyclerViewEvents;
    MyRecyclerViewAdapter eventsAdapter;
    ImageButton btInfo;
    LinearLayout msgNoEvents;
    FirebaseUser firebaseUser;
    DatabaseReference dataBase;
    String userUid;
    ImageView imgNoEvents;
    ProgressBar progressBar;
    DatabaseReference teacherBase;
    Thread th;
    String minH;
    String classStudent;
    int countKey;
    boolean stop;
    boolean appHidden, firstTime, checkinChecked;
    boolean runningThread;
    public static Event CURRENT_EVENT;
    final static String CHANNEL_ID = "simplified_coding";
    static public String TAG = "HomeScreen";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Log.d("AQUI","Iniciou em Home Activity");
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        userUid = firebaseUser.getUid();
        DatabaseReference dataStudent = FirebaseDatabase.getInstance().getReference();

        Log.d("AQUI","Começou a configurar as variáveis");
        btInfo = findViewById(R.id.activity_home_bt_about_us);
        imgNoEvents = findViewById(R.id.activity_home_img_no_events);
        msgNoEvents = findViewById(R.id.msg_no_events);
        th = null;
        dataBase = FirebaseDatabase.getInstance().getReference();
        teacherBase = dataBase.child("professores");
        progressBar = findViewById(R.id.activity_home_progressBar);
        appHidden = false;
        firstTime = true;
        runningThread = false;
        countKey=0;
        Date hora = new Date();
        minH = Integer.toString(hora.getHours());
        minH = (hora.getMinutes() >= 0 && hora.getMinutes() <= 9 ? "0" + minH : minH);


        CURRENT_EVENT = null;
        createNotificationChannel(getApplicationContext());
        createNotificationChannelFIREBASE(getApplicationContext());


        btInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popUpMoreOption();
            }
        });

        btInfo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(HomeActivity.this, "Mais opções", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        Log.d("AQUI","BUSCARÁ A TURMA");
        dataStudent.child("salas").orderByChild(userUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("AQUI","No OnDataChange");
                if(dataSnapshot.exists()){

                    Log.d("AQUI","DataSnapshot existe");
                    Log.d("AQUI","UID: "+userUid);
                    for(DataSnapshot dados : dataSnapshot.getChildren()){

                        classStudent=dados.getKey();
                    }
                    Log.d("AQUI","Class: "+classStudent);

                        FirebaseMessaging.getInstance().subscribeToTopic(classStudent)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        String msg = "Foi inscrito " + classStudent;
                                        if (!task.isSuccessful()) {
                                            msg = "Nao foi inscrito " + classStudent;
                                        }
                                        Log.d(TAG, msg);
                                    }
                                });


                   getCurrentUserEvents(classStudent);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        teacherBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                getCurrentUserEvents(classStudent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



    }

    //Visiveis
    @Override
    protected void onStart() {
        super.onStart();

        appHidden = false;

    }

    @Override
    protected void onResume() {
        super.onResume();

        appHidden = false;

    }

    //Oculto - Segundo Plano
    @Override
    protected void onPause() {
        super.onPause();
        appHidden = true;

    }

    @Override
    protected void onStop() {
        super.onStop();

        appHidden = true;


    }

    private void getCurrentUserEvents(final String turma) {

        teacherBase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<Events> firebaseEvents = new ArrayList<Events>();
                    List<Event> eventList = new ArrayList<Event>();

                    for (final DataSnapshot dados : dataSnapshot.getChildren()) {
                        Professores profs = dados.getValue(Professores.class);
                        HashMap<String, Events> events = profs.getEvents().get(turma);
                        if (events != null) {
                            int j = 0;
                            Date time = new Date();
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
                            String fullDate = sdf.format(time);
                            for (final Map.Entry<String, Events> m : events.entrySet()) {
                                String checkin = "", checkout = "";
                                if (!m.getKey().equals("evento0") && m.getValue().getDate().equals(fullDate)) {
                                    firebaseEvents.add(m.getValue());
                                    HashMap<String, Keys> keys = m.getValue().getKeys();
                                    final List<Keys> keysTemp = new ArrayList<Keys>();
                                    if (keys != null) {
                                        int i = 0;
                                        for (Map.Entry<String, Keys> k : keys.entrySet()) {
                                            Keys keyTemp = new Keys(k.getValue().getKey(), k.getValue().getTime());
                                            keysTemp.add(keyTemp);
                                            i++;
                                        }
                                    }
                                    HashMap<String, Students> students = m.getValue().getStudents();
                                    if (students != null) {
                                        for (Map.Entry<String, Students> s : students.entrySet()) {
                                            if (s.getKey().equals(userUid)) {
                                                checkin = (s.getValue().getCheckin());
                                                checkout = (s.getValue().getCheckout());
                                                if (!checkin.equals("") && checkout.equals(""))
                                                {
                                                    final Handler handle = new Handler();
                                                    final String checkinF = checkin;
                                                    final String checkoutF = checkout;
                                                    final Events ev_th = m.getValue();
                                                    final String uidEv = m.getKey();
                                                    final String uidTeacher = dados.getKey();

                                                    teacherBase.child(uidTeacher).child("events").child(classStudent)
                                                            .child(uidEv).child("students").child(userUid).child("keys").addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            if(dataSnapshot.exists()){
                                                                Log.d("AQUI","No OnDataChange Current Event");
                                                                countKey=0;
                                                                if(dataSnapshot.child("key1").getValue().toString().equals("ok")||dataSnapshot.child("key1").getValue().toString().equals("err")||isAfterKey(keysTemp.get(0).getTime()))countKey++;
                                                                if(dataSnapshot.child("key2").getValue().toString().equals("ok")||dataSnapshot.child("key2").getValue().toString().equals("err")||isAfterKey(keysTemp.get(1).getTime()))countKey++;
                                                                if(dataSnapshot.child("key3").getValue().toString().equals("ok")||dataSnapshot.child("key3").getValue().toString().equals("err")||isAfterKey(keysTemp.get(2).getTime()))countKey++;
                                                                Log.d("AQUI","Count Key: "+countKey);
                                                                continueThreadCurrent(handle,checkinF,checkoutF,ev_th,uidEv,uidTeacher,keysTemp);
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });


                                                }

                                            }
                                        }
                                    }
                                    eventList.add(new Event(m.getValue(), m.getKey(), dados.getKey(), checkin, checkout, keysTemp));
                                    j++;
                                }
                            }

                        }
                    }
                    getCheckedEvents(eventList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private boolean isAfterKey(String hourKey){
        if(hourKey.equals(""))return false;
        int horaKeyFinal = Integer.parseInt(hourKey.substring(0, 2));//Pegar a hora do Evento --> HHhMMmin
        int minKeyFinal = Integer.parseInt(hourKey.substring(3, 5));//Pegar o minuto do Evento --> HHhMMmin
        Date dCell = new Date();
        int horaNow = dCell.getHours();
        int minNow = dCell.getMinutes();
        int horaEmMinutosKey = horaKeyFinal * 60 + minKeyFinal;
        int horaEmMinutosNow = horaNow * 60 + minNow;
        boolean b=(  horaEmMinutosNow-horaEmMinutosKey <= (1+TIME_EMERGENCY) && horaEmMinutosNow-horaEmMinutosKey >= 0);//Verifica se a key está entre o período de apertar o botão - Valor entre 0 e TIME_EMERGENCY
        Log.d("AQUI","Está entre o período de colocar a key: "+(b));
        Log.d("AQUI", "Diferença das horas em minutos: " + (horaEmMinutosNow - horaEmMinutosKey));
        if(b)return false;
        if(horaEmMinutosNow-horaEmMinutosKey > (1+TIME_EMERGENCY)&&!b)return true;//Passou a hora da key
        return false;
    }

    private void continueThreadCurrent(final Handler handle,final String checkinF,final String checkoutF, final Events ev_th, final String uidEv, final String uidTeacher, final List<Keys> keysTemp  ){
        CURRENT_EVENT = new Event(ev_th, uidEv, uidTeacher, checkinF, checkoutF, keysTemp);



        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                while (!stop) {
                    Log.d("AQUI", "Na Thread Current Events.....");
                    synchronized (this) {
                        try {
                            wait(500);
                            handle.post(new Runnable() {
                                @Override
                                public void run() {
                                    runningThread = true;
                                    Date time = new Date();
                                    String hora = Integer.toString(time.getHours());
                                    String min = Integer.toString(time.getMinutes());
                                    min = (time.getMinutes() >= 0 && time.getMinutes() <= 9 ? "0" + min : min);
                                    hora = (time.getHours() >= 0 && time.getHours() <= 9 ? "0" + hora : hora);
                                    String fullHour = hora + "h" + min + "min";
                                    Log.d("AQUI", "Hora atual, no celular: " + fullHour);
                                    if (!minH.equals(min) || firstTime) {

                                        Log.d("AQUI", "Mudou o Minuto, novo horário, no celular: " + fullHour);

                                        Log.d("AQUI", "Verificando se lança a key......");

                                        givePop(fullHour,CURRENT_EVENT);

                                        minH = min;

                                    }


                                }
                            });

                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                runningThread = false;

            }
        };
        th = new Thread(runnable);
        th.start();
    }

    private void getCheckedEvents(List<Event> events) {

        if (events.size() > 0) {
            msgNoEvents.setVisibility(View.INVISIBLE);
        } else {
            msgNoEvents.setVisibility(View.VISIBLE);
            imgNoEvents.setVisibility(View.VISIBLE);

        }
        buildRecyclerView(events);
    }

    public void setCheckInTime(final List<Event> events, final int position) {

                if (!runningThread) {
                    Date time = new Date();
                    String hora = Integer.toString(time.getHours());
                    String min = Integer.toString(time.getMinutes());
                    min = (time.getMinutes() >= 0 && time.getMinutes() <= 9 ? "0" + min : min);
                    hora = (time.getHours() >= 0 && time.getHours() <= 9 ? "0" + hora : hora);


                    //Start Time of Event
                    final int horaEvent = Integer.parseInt(events.get(position).getStartTime().substring(0, 2));//Pegar a hora do Evento --> HHhMMmin
                    final int minEvent = Integer.parseInt(events.get(position).getStartTime().substring(3, 5));//Pegar o minuto do Evento --> HHhMMmin
                    final Date dInicio = new Date();
                    dInicio.setHours(horaEvent);
                    dInicio.setMinutes(minEvent);
                    Log.d("AQUI", "Hora de inicio do Evento: " + horaEvent + "h" + minEvent);
                    //Finish Time of Event
                    final int horaEventFinal = Integer.parseInt(events.get(position).getEndTime().substring(0, 2));//Pegar a hora do Evento --> HHhMMmin
                    final int minEventFinal = Integer.parseInt(events.get(position).getEndTime().substring(3, 5));//Pegar o minuto do Evento --> HHhMMmin
                    final Date dFinal = new Date();
                    dFinal.setHours(horaEventFinal);
                    dFinal.setMinutes(minEventFinal);
                    Log.d("AQUI", "Hora de fim do Evento: " + dFinal.getHours() + "h" + dFinal.getMinutes());
                    int horaNow = Integer.parseInt(hora);
                    int minNow = Integer.parseInt(min);
                    Date dCell = new Date();
                    dCell.setHours(horaNow);
                    dCell.setMinutes(minNow);
                    int horaEmMinutosEvent = horaEvent * 60 + minEvent;
                    int horaEmMinutosNow = horaNow * 60 + minNow;
                    //10 minutos antes ou entre o período do evento
                    Log.d("AQUI", "Hora do Celular está depois do inicio: " + dCell.after(dInicio) + "   Hora do Celular está antes do Final: " + dCell.before(dFinal));
                    if (((minEvent - minNow) <= 10 && (minEvent - minNow) >= 0 && horaNow == horaEvent) || (horaNow != horaEvent && (horaEmMinutosEvent - horaEmMinutosNow) <= 10 && (horaEmMinutosEvent - horaEmMinutosNow) >= 0) || (dCell.after(dInicio) && dCell.before(dFinal))) {
                        checkinChecked = true;
                    } else {
                        checkinChecked = false;
                    }
                    if (checkinChecked) {
                        if (events.get(position).getCheckInTime() == null || events.get(position).getCheckInTime().isEmpty()) {

                            teacherBase.child(events.get(position).getuIdTeacher()).child("events").child(classStudent)
                                    .child(events.get(position).getUid()).child("students").child(userUid).child("checkin")
                                    .setValue(hora + "h" + min);

                            events.get(position).setCheckInTime(hora + "h" + min);

                            stop = false;
                            countKey=0;
                            CURRENT_EVENT = events.get(position);
                            ComponentName componentName = new ComponentName(HomeActivity.this, NotificationServiceScheduler.class);
                            JobInfo info = new JobInfo.Builder(123, componentName)
                                    .setRequiresCharging(false)
                                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                                    .setPersisted(true)
                                    .setPeriodic(15 * 60 * 100)
                                    .build();
                            JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
                            int resultCode = scheduler.schedule(info);
                            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                                Log.d("AQUI", "Job scheduled");
                            } else {
                                Log.d("AQUI", "Job scheduled failed");
                            }
                            eventsAdapter.notifyItemChanged(position);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Você pode entrar com 10 minutos antes de inciar a aula ou durante o evento", Toast.LENGTH_LONG).show();
                    }



                } else if (runningThread && events.get(position).getCheckInTime().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Aperte em checkout no último evento que você entrou", Toast.LENGTH_SHORT).show();
                }




    }

    public void setCheckOutTime(final List<Event> events, final int position) {

                if (events.get(position).getCheckInTime() != null && !events.get(position).getCheckInTime().isEmpty()) {
                    if (!events.get(position).isCheckOutDone()) {
                        Date time = new Date();
                        String hora = Integer.toString(time.getHours());
                        String min = Integer.toString(time.getMinutes());
                        min = (time.getMinutes() >= 0 && time.getMinutes() <= 9 ? "0" + min : min);
                        hora = (time.getHours() >= 0 && time.getHours() <= 9 ? "0" + hora : hora);


                        teacherBase.child(events.get(position).getuIdTeacher()).child("events").child(classStudent)
                                .child(events.get(position).getUid()).child("students").child(userUid).child("checkout")
                                .setValue(hora + "h" + min);

                        events.get(position).setCheckInTime(hora + "h" + min);
                        events.get(position).setCheckOutTime(hora + "h" + min);



                        stop = true;
                        countKey=0;
                        CURRENT_EVENT = null;
                        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
                        scheduler.cancel(123);
                        Log.d("AQUI", "Job Schedular Cancelled");
                        eventsAdapter.notifyItemChanged(position);
                    }
                }


    }

    public void buildRecyclerView(final List<Event> eventsList) {

        Collections.sort(eventsList);

        recyclerViewEvents = findViewById(R.id.home_recycler_view_events);
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
        eventsAdapter = new MyRecyclerViewAdapter(eventsList);

        if (eventsList != null && eventsList.size() > 0) {
            recyclerViewEvents.setAdapter(eventsAdapter);
            progressBar.setVisibility(View.GONE);
        }

        eventsAdapter.setOnItemClickListener(new MyRecyclerViewAdapter.OnItemClickListener() {

            @Override
            public void onCheckInClick(int position) {
                setCheckInTime(eventsList, position);
            }

            @Override
            public void onCheckOutClick(int position) {
                setCheckOutTime(eventsList, position);

            }

            @Override
            public void onKeyButtonClick(int position) {
                givePopEmergency(eventsList.get(position));
            }

            @Override
            public void onGoLiveClick(int position) {
                Uri uri = Uri.parse(eventsList.get(position).getUrl());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        eventsAdapter.setOnLongClickListener(new MyRecyclerViewAdapter.OnLongClickListener() {
            @Override
            public void onLongCheckInClick(int position) {

            }

            @Override
            public void onLongCheckOutClick(int position) {

            }

            @Override
            public void onLongGoLiveClick(int position) {
                copyLinkMeet(eventsList.get(position).getUrl());
            }

            @Override
            public void onLongKeyButtonClick(int position) {
                Toast.makeText(HomeActivity.this, "Verifique se ainda pode colocar a key", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void givePopEmergency(final Event events) {

        if(events.isCheckInDone()&&!events.isCheckOutDone()&&runningThread) {

            Date time = new Date();
            String hora = Integer.toString(time.getHours());
            String min = Integer.toString(time.getMinutes());
            min = (time.getMinutes() >= 0 && time.getMinutes() <= 9 ? "0" + min : min);
            hora = (time.getHours() >= 0 && time.getHours() <= 9 ? "0" + hora : hora);
            int minTemp = Integer.parseInt(min);
            int horaTemp = Integer.parseInt(hora);
            boolean verify = false;
            Log.d("AQUI", Integer.toString(events.getKeys().size()));

            for (int i = 0; i < events.getKeys().size(); i++) {
                if (!events.getKeys().get(i).getTime().isEmpty() && !events.getKeys().get(i).getKey().isEmpty()) {
                    int horaKey = Integer.parseInt(events.getKeys().get(i).getTime().substring(0, 2));
                    int minKey = Integer.parseInt(events.getKeys().get(i).getTime().substring(3, 5));// HHhMMmin
                    int horaKeyMin = horaKey * 60 + minKey;
                    int horaTempMin = horaTemp * 60 + minTemp;

                    Log.d("AQUI", "Hora em minutos de Manaus: " + horaTempMin + "   Hora em minutos da Key: " + horaKeyMin);
                    Log.d("AQUI", "Diferença das horas em minutos: " + (horaTempMin - horaKeyMin));
                    Log.d("AQUI","CountKey: "+countKey);
                    if ((horaTempMin - horaKeyMin <= (1+TIME_EMERGENCY) && horaTempMin - horaKeyMin >= 0)&&countKey==i) {
                        Log.d("AQUI", "Diferença entre "+TIME_EMERGENCY+"min e 0min");
                        popUp(events, i);
                        verify = true;
                        break;
                    }
                }
            }
            if (!verify) {

                Toast.makeText(getApplicationContext(), "Não se preocupe! Não há palavra-passe no momento!", Toast.LENGTH_SHORT).show();
                CURRENT_EVENT = events;

                firstTime = false;

            }



        }else if(!events.isCheckInDone()&&!events.isCheckOutDone()&&runningThread){
            Toast.makeText(getApplicationContext(), "Aperte em checkout no último evento que você entrou", Toast.LENGTH_SHORT).show();
        }else if(events.isCheckInDone()&&events.isCheckOutDone()){
            Toast.makeText(getApplicationContext(), "Você já saiu desse evento", Toast.LENGTH_SHORT).show();
        }


    }

    private void givePop(String fullHour,final Event events) {


                if (fullHour.equals(events.getKeys().get(0).getTime())) {
                    Log.d("AQUI", "Vai soltar o POP-UP - 1");
                    popUp(events, 0);


                } else if (fullHour.equals(events.getKeys().get(1).getTime())) {

                    Log.d("AQUI", "Vai soltar o POP-UP - 2");
                    popUp(events, 1);



                } else if (fullHour.equals(events.getKeys().get(2).getTime())) {

                    Log.d("AQUI", "Vai soltar o POP-UP - 3");
                    popUp(events, 2);


                } else {
                    CURRENT_EVENT = events;
                }
                firstTime = false;




    }

    private void popUp(final Event events, final int keyPosition) {

        MediaPlayer popup = MediaPlayer.create(this, R.raw.popup);
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(HomeActivity.this);
        mBuilder.setCancelable(false);
        View mView = getLayoutInflater().inflate(R.layout.dialog_teacher_key_word, null);
        final EditText edtPassword = (EditText) mView.findViewById(R.id.dialog_key_word_edt_password);
        TextView messageKey = mView.findViewById(R.id.number_key_word);
        Button btnConfirma = (Button) mView.findViewById(R.id.dialog_key_word_bt_confirma);
        messageKey.setText("Insira a " + (keyPosition + 1) + "ª" + " palavra-passe fornecida pelo(a) professor(a).");
        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnConfirma.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!edtPassword.getText().toString().equals("") || !edtPassword.getText().toString().isEmpty()) {
                    if (edtPassword.getText().toString().trim().equalsIgnoreCase(events.getKeys().get(keyPosition).getKey().trim())) {
                        teacherBase.child(events.getuIdTeacher()).child("events").child(classStudent)
                                .child(events.getUid()).child("students").child(userUid).child("keys").child("key" + Integer.toString(keyPosition + 1))
                                .setValue("ok");
                        dialog.dismiss();

                        Toast.makeText(HomeActivity.this, "Palavra-passe inserida com sucesso", Toast.LENGTH_SHORT).show();


                    } else {
                        teacherBase.child(events.getuIdTeacher()).child("events").child(classStudent)
                                .child(events.getUid()).child("students").child(userUid).child("keys").child("key" + Integer.toString(keyPosition + 1))
                                .setValue("err");
                        dialog.dismiss();

                        Toast.makeText(HomeActivity.this, "Palavra-passe inserida incorretamente, preste mais atenção na aula", Toast.LENGTH_SHORT).show();


                    }
                    NotificationManagerCompat mNotificationMgr = NotificationManagerCompat.from(getApplicationContext());
                    mNotificationMgr.cancel(1);
                } else {
                    Toast.makeText(HomeActivity.this, "PREENCHA O CAMPO", Toast.LENGTH_SHORT).show();
                }

            }
        });
        if (!firstTime || appHidden)
            displayNotification("Frequência FMM", "Olá, como está a aula? Você deve inserir a palavra-passe para notificar o professor que você está acompanhando a aula!!!");
        popup.start();
        dialog.show();
        Log.d("AQUI", "POP-UP Lançado!!!!");
        CURRENT_EVENT=events;
    }

    public void displayNotification(String title, String body) {


        Log.d("AQUI", "Entrou pra lançar notificação......");

        Intent it = new Intent(getApplicationContext(), HomeActivity.class);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext(), "simplified_coding")
                        .setTicker("Frequência FMM")
                        .setSmallIcon(R.drawable.logo_main)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setVibrate(new long[]{150, 300, 150, 300, 150})
                        .setShowWhen(true)
                        .setAutoCancel(true)
                        .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, it, PendingIntent.FLAG_UPDATE_CURRENT))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        Log.d("AQUI", "Criou o Builder......");

        NotificationManagerCompat mNotificationMgr = NotificationManagerCompat.from(getApplicationContext());
        mNotificationMgr.notify(1, mBuilder.build());

        Log.d("AQUI", "Lançou a notificação......");


    }

    public static void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Frequência FMM";
            String description = "Notificação APP";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void createNotificationChannelFIREBASE(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Serviços - Frequência FMM";
            String description = "Serviços - Notificação APP";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("simplified_coding_2", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void logOut() {
        stop = true;
        CURRENT_EVENT = null;
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        scheduler.cancel(123);
        Log.d("AQUI", "Job Schedular Cancelled");
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signOut();
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        finish();
    }

    private void screenAbout() {
        startActivity(new Intent(getApplicationContext(), AboutActivity.class));
    }

    private void popUpMoreOption() {

        MediaPlayer popup = MediaPlayer.create(this, R.raw.popup);
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(HomeActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_options, null);
        Button btnAboutUs = mView.findViewById(R.id.dialog_options_bt_about_us);
        Button btnLogOut = mView.findViewById(R.id.dialog_options_bt_logout);
        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnAboutUs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                screenAbout();
            }
        });

        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                logOut();

            }
        });
        popup.start();
        dialog.show();
    }

    private void copyLinkMeet(String link) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            ClipData clipData = ClipData.newPlainText(link, link);
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(getApplicationContext(), "Link do Google Meet copiado com sucesso", Toast.LENGTH_SHORT).show();
        }

    }

    interface OnDateTimeReceived {
        void dateTimeReceivedListener(String hora, String min);
    }
}
