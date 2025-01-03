package com.example.assignment;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    GoogleMap gMap;
    MapFragment mapFrag;
    Button btnPrev, btnNext, searchButton, btnFavorite; // 즐겨찾기 버튼 추가
    EditText searchBar;
    List<String> lines = new ArrayList<>();
    List<String> favoriteLocations = new ArrayList<>(); // 즐겨찾기 리스트 추가
    int currentMenu = 1; // 현재 선택된 메뉴
    int currentIndex = 0; // 현재 위치 인덱스

    DatePicker dp;
    EditText edtDiary;
    Button btnWrite;
    String fileName;

    //앱 실행 시 호출되는 메서드로, 초기화 작업을 수행
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("경기도 앱");
        mapFrag = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        searchBar = findViewById(R.id.searchBar);
        searchButton = findViewById(R.id.searchButton);
        btnFavorite = findViewById(R.id.favoriteButton); // 즐겨찾기 버튼 초기화

        // 기본 CSV 파일 읽기 (관광 정보)
        readCSV(R.raw.tourist);

        // 버튼 클릭 이벤트 설정
        btnPrev.setOnClickListener(v -> moveToPrevLocation());
        btnNext.setOnClickListener(v -> moveToNextLocation());
        searchButton.setOnClickListener(v -> searchLocation(searchBar.getText().toString()));

        // 즐겨찾기 버튼 클릭 이벤트 설정
        btnFavorite.setOnClickListener(v -> addFavoriteLocation());

        dp = (DatePicker) findViewById(R.id.datePicker1);
        edtDiary = (EditText) findViewById(R.id.edtDiary);
        btnWrite = (Button) findViewById(R.id.btnWrite);

        Calendar cal = Calendar.getInstance();
        int cYear = cal.get(Calendar.YEAR);
        int cMonth = cal.get(Calendar.MONTH);
        int cDay = cal.get(Calendar.DAY_OF_MONTH);

        dp.init(cYear, cMonth, cDay, new DatePicker.OnDateChangedListener() {
            public void onDateChanged(DatePicker view, int year,
                                      int monthOfYear, int dayOfMonth) {
                fileName = Integer.toString(year) + "_"
                        + Integer.toString(monthOfYear + 1) + "_"
                        + Integer.toString(dayOfMonth) + ".txt";
                String str = readDiary(fileName);
                edtDiary.setText(str);
                btnWrite.setEnabled(true);
            }
        });


        btnWrite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    FileOutputStream outFs = openFileOutput(fileName,
                            Context.MODE_PRIVATE);
                    String str = edtDiary.getText().toString();
                    outFs.write(str.getBytes());
                    outFs.close();
                    Toast.makeText(getApplicationContext(),
                            fileName + " 이 저장됨", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                }
            }
        });
    }

    //선택된 날짜에 해당하는 파일에서 일기 내용을 읽어오는 메서드
    String readDiary(String fName) {
        String diaryStr = null;
        FileInputStream inFs;
        try {
            inFs = openFileInput(fName);
            byte[] txt = new byte[500];
            inFs.read(txt);
            inFs.close();
            diaryStr = (new String(txt)).trim();
            btnWrite.setText("한줄평 수정");
        } catch (IOException e) {
            edtDiary.setHint("한줄평 없음");
            btnWrite.setText("한줄평 저장");
        }
        return diaryStr;
    }

    //GoogleMap 초기화 작업을 수행
    @Override
    public void onMapReady(GoogleMap map) {
        gMap = map;
        gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.568256, 126.897240), 13));
        gMap.getUiSettings().setZoomControlsEnabled(true);

        // 초기 지도 업데이트
        updateMap();
    }

    //리소스의 CSV 파일을 읽어 lines 리스트에 저장
    public void readCSV(int resourceId) {
        lines.clear();
        InputStream inputStream = getResources().openRawResource(resourceId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        try {
            String line;
            reader.readLine(); // 헤더 스킵
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //옵션 메뉴를 생성하는 메서드
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 1, 0, "관광");
        menu.add(0, 2, 0, "문화/여가");
        menu.add(0, 3, 0, "즐겨찾기");
        return true;
    }

    //사용자가 옵션 메뉴를 선택했을 때 호출
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case 1: // 관광
                    currentMenu = 1;
                    readCSV(R.raw.tourist);
                    updateMap();
                    return true;
                case 2: // 문화/여가
                    currentMenu = 2;
                    readCSV(R.raw.culture);
                    updateMap();
                    return true;
                case 3: // 즐겨찾기
                    currentMenu = 3;
                    showFavorites(); // 즐겨찾기 목록을 지도에 표시
                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    //아이콘 리소스를 특정 크기로 조정하여 BitmapDescriptor로 반환
    private BitmapDescriptor resizeIcon(int resourceId, int width, int height) {
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), resourceId);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }

    //현재 선택된 메뉴에 따라 지도에 마커를 추가
    private void updateMap() {
        gMap.clear();
        if (currentMenu == 3) {
            // 즐겨찾기 메뉴일 때만 즐겨찾기 목록을 표시
            showFavorites();
        } else if (!lines.isEmpty()) {
            for (String line : lines) {
                try {
                    String[] tokens = line.split(",");
                    if (tokens.length < 3) continue;

                    double lat = Double.parseDouble(tokens[0]);
                    double lon = Double.parseDouble(tokens[1]);
                    String placeName = tokens[2];

                    LatLng point = new LatLng(lat, lon);
                    int iconResource = R.drawable.icon1;

                    if (currentMenu == 2) {
                        iconResource = R.drawable.icon2;
                    } else if (currentMenu == 3) {
                        iconResource = R.drawable.icon3;
                    }

                    BitmapDescriptor resizedIcon = resizeIcon(iconResource, 100, 146);

                    gMap.addMarker(new MarkerOptions()
                            .position(point)
                            .title(placeName)
                            .icon(resizedIcon));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //이전 장소로 이동
    private void moveToPrevLocation() {
        if (lines.isEmpty()) return;

        currentIndex = (currentIndex - 1 + lines.size()) % lines.size();
        moveToLocation(currentIndex);
    }

    //다음 장소로 이동
    private void moveToNextLocation() {
        if (lines.isEmpty()) return;

        currentIndex = (currentIndex + 1) % lines.size();
        moveToLocation(currentIndex);
    }

    //특정 인덱스의 장소로 카메라를 이동
    private void moveToLocation(int index) {
        try {
            String[] tokens = lines.get(index).split(",");
            double lat = Double.parseDouble(tokens[0]);
            double lon = Double.parseDouble(tokens[1]);
            String placeName = tokens[2];

            LatLng point = new LatLng(lat, lon);
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //현재 선택된 장소를 즐겨찾기에 추가
    public void addFavoriteLocation() {
        if (lines.isEmpty()) return;

        String selectedLine = lines.get(currentIndex);
        if (!favoriteLocations.contains(selectedLine)) {
            favoriteLocations.add(selectedLine); // 즐겨찾기 추가
            saveFavoritesToFile(); // 즐겨찾기 저장
            Toast.makeText(this, "즐겨찾기에 추가되었습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "이미 즐겨찾기에 추가된 장소입니다.", Toast.LENGTH_SHORT).show();
        }
    }

    //저장된 즐겨찾기 목록을 불러와 지도에 표시
    private void showFavorites() {
        gMap.clear();

        try {
            // 파일에서 즐겨찾기 데이터 읽기
            InputStream inputStream = openFileInput("favorites.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            reader.close();

            // JSON 파싱
            JSONArray favoritesArray = new JSONArray(stringBuilder.toString());

            // JSON 배열의 각 항목을 지도에 마커로 표시
            for (int i = 0; i < favoritesArray.length(); i++) {
                JSONObject favorite = favoritesArray.getJSONObject(i);
                double lat = favorite.getDouble("lat");
                double lon = favorite.getDouble("lon");
                String placeName = favorite.getString("placeName");

                LatLng point = new LatLng(lat, lon);
                BitmapDescriptor resizedIcon = resizeIcon(R.drawable.icon3, 100, 146); // 즐겨찾기 아이콘

                gMap.addMarker(new MarkerOptions()
                        .position(point)
                        .title(placeName)
                        .icon(resizedIcon));
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "즐겨찾기 로딩에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    //즐겨찾기 데이터를 JSON 형식으로 내부 저장소 파일에 저장
    private void saveFavoritesToFile() {
        try {
            // 내부 저장소에 파일에 저장
            FileOutputStream fos = openFileOutput("favorites.json", Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            // JSON 배열 생성
            JSONArray jsonArray = new JSONArray();
            for (String location : favoriteLocations) {
                String[] tokens = location.split(",");
                if (tokens.length >= 3) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("lat", tokens[0]);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        jsonObject.put("lon", tokens[1]);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        jsonObject.put("placeName", tokens[2]);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    jsonArray.put(jsonObject);
                }
            }

            writer.write(jsonArray.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //입력된 키워드를 기반으로 장소를 검색
    private void searchLocation(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            Toast.makeText(this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean found = false; // 검색 결과가 있는지 확인하는 플래그
        gMap.clear(); // 검색 전에 지도 초기화

        for (String line : lines) {
            String[] tokens = line.split(",");
            if (tokens.length < 3) continue;

            String placeName = tokens[2];
            if (placeName.contains(keyword)) {
                try {
                    double lat = Double.parseDouble(tokens[0]);
                    double lon = Double.parseDouble(tokens[1]);
                    LatLng point = new LatLng(lat, lon);

                    // 아이콘 설정 (현재 메뉴에 맞는 아이콘 사용)
                    int iconResource = R.drawable.icon1; // 기본 아이콘 (관광)
                    if (currentMenu == 2) {
                        iconResource = R.drawable.icon2; // 문화/여가 아이콘
                    } else if (currentMenu == 3) {
                        iconResource = R.drawable.icon3; // 즐겨찾기 아이콘
                    }

                    // 아이콘 리사이징
                    BitmapDescriptor resizedIcon = resizeIcon(iconResource, 100, 146);

                    // 검색 결과 마커 추가
                    gMap.addMarker(new MarkerOptions()
                            .position(point)
                            .title(placeName)
                            .icon(resizedIcon)); // 아이콘을 추가
                    found = true; // 검색된 항목이 하나라도 있으면 플래그를 true로 설정
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 검색된 결과가 없을 경우
        if (!found) {
            Toast.makeText(this, "결과가 없습니다.", Toast.LENGTH_SHORT).show();
        } else {
            // 여러 마커를 추가했으면, 첫 번째 마커로 지도 줌
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.568256, 126.897240), 13));
        }
    }


}