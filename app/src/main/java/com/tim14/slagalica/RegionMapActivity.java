package com.tim14.slagalica;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.tim14.slagalica.model.RegionMapPoint;
import com.tim14.slagalica.model.Region;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

public class RegionMapActivity extends AppCompatActivity {

    private MapView osmRegionMapView;
    private LinearLayout regionSummaryContainer;
    private TextView selectedRegionText;
    private TextView mapStatusText;
    private TextView backToRankingButton;
    private TextView resetMonthlyCycleButton;

    private FirestoreRepository firestoreRepository;
    private String currentUserRegion;
    private final Map<String, Integer> regionCounts = new HashMap<>();
    private final Map<String, Integer> activeRegionCounts = new HashMap<>();
    private final Map<String, Region> regionsByName = new HashMap<>();
    private final List<Region> currentRegions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_region_map);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        firestoreRepository = new FirestoreRepository(this);
        osmRegionMapView = findViewById(R.id.osmRegionMapView);
        regionSummaryContainer = findViewById(R.id.regionSummaryContainer);
        selectedRegionText = findViewById(R.id.selectedRegionText);
        mapStatusText = findViewById(R.id.mapStatusText);
        backToRankingButton = findViewById(R.id.backToRankingButton);
        resetMonthlyCycleButton = findViewById(R.id.resetMonthlyCycleButton);

        findViewById(R.id.regionBackButton).setOnClickListener(v -> finish());
        backToRankingButton.setOnClickListener(v -> renderRegionRanking());
        resetMonthlyCycleButton.setOnClickListener(v -> confirmMonthlyCycleReset());
        setupOpenStreetMap();

        markCurrentUserActiveAndLoadRegions();
    }

    private void markCurrentUserActiveAndLoadRegions() {
        firestoreRepository.markCurrentUserActive(new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadRegionData();
            }

            @Override
            public void onError(String error) {
                loadRegionData();
            }
        });
    }

    private void setupOpenStreetMap() {
        osmRegionMapView.setTileSource(TileSourceFactory.MAPNIK);
        osmRegionMapView.setMultiTouchControls(true);
        osmRegionMapView.getController().setZoom(6.7);
        osmRegionMapView.getController().setCenter(new GeoPoint(44.05, 20.85));
    }

    private void loadRegionData() {
        mapStatusText.setText(R.string.regions_loading);

        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                currentUserRegion = FirestoreRepository.canonicalRegionName(user.region);
                loadRegionRankingAndPlayers();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(RegionMapActivity.this, error, Toast.LENGTH_SHORT).show();
                loadRegionRankingAndPlayers();
            }
        });
    }

    private void loadRegionRankingAndPlayers() {
        firestoreRepository.getRegions(new FirebaseCallback<List<Region>>() {
            @Override
            public void onSuccess(List<Region> regions) {
                currentRegions.clear();
                currentRegions.addAll(regions);
                regionsByName.clear();

                for (Region region : regions) {
                    regionsByName.put(region.name, region);
                }

                loadPlayers();
            }

            @Override
            public void onError(String error) {
                mapStatusText.setText(R.string.regions_load_failed);
                Toast.makeText(RegionMapActivity.this, error, Toast.LENGTH_SHORT).show();
                loadPlayers();
            }
        });
    }

    private void loadPlayers() {
        firestoreRepository.getAllUsers(new FirebaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                List<RegionMapPoint> points = new ArrayList<>();
                regionCounts.clear();
                activeRegionCounts.clear();

                for (User user : users) {
                    if (TextUtils.isEmpty(user.region)) {
                        continue;
                    }

                    String canonicalRegion = FirestoreRepository.canonicalRegionName(user.region);
                    if (TextUtils.isEmpty(canonicalRegion)) {
                        continue;
                    }

                    regionCounts.put(canonicalRegion, regionCounts.getOrDefault(canonicalRegion, 0) + 1);
                    if (FirestoreRepository.isRecentlyActive(user)) {
                        activeRegionCounts.put(
                                canonicalRegion,
                                activeRegionCounts.getOrDefault(canonicalRegion, 0) + 1
                        );
                    }

                    RegionMapPoint point = createPointForUser(user, canonicalRegion);
                    if (point != null) {
                        points.add(point);
                    }
                }

                renderOpenStreetMap(points);
                applyPlayerCountsToRegions();
                syncPlayerCountsToFirestore();
                renderRegionRanking();
                mapStatusText.setText(getString(R.string.regions_loaded_format, points.size()));
            }

            @Override
            public void onError(String error) {
                mapStatusText.setText(R.string.regions_load_failed);
                Toast.makeText(RegionMapActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private RegionMapPoint createPointForUser(User user, String canonicalRegion) {
        RegionShape regionShape = findRegionShape(canonicalRegion);
        if (regionShape == null) {
            return null;
        }

        Random random = new Random(stableSeed(user));
        for (int attempt = 0; attempt < 80; attempt++) {
            float x = randomInRange(random, minLon(regionShape), maxLon(regionShape));
            float y = randomInRange(random, minLat(regionShape), maxLat(regionShape));
            if (contains(regionShape.points, y, x)) {
                return new RegionMapPoint(user.id, user.username, canonicalRegion, x, y);
            }
        }

        return null;
    }

    private void confirmMonthlyCycleReset() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.regions_reset_confirm_title)
                .setMessage(R.string.regions_reset_confirm_message)
                .setPositiveButton(R.string.regions_reset_confirm_action,
                        (dialog, which) -> resetMonthlyCycleForTest())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void resetMonthlyCycleForTest() {
        resetMonthlyCycleButton.setEnabled(false);
        firestoreRepository.resetMonthlyRegionRanking(new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(RegionMapActivity.this, R.string.regions_reset_success, Toast.LENGTH_SHORT).show();
                resetMonthlyCycleButton.setEnabled(true);
                loadRegionData();
            }

            @Override
            public void onError(String error) {
                resetMonthlyCycleButton.setEnabled(true);
                Toast.makeText(RegionMapActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderOpenStreetMap(List<RegionMapPoint> playerPoints) {
        osmRegionMapView.getOverlays().clear();

        for (RegionShape regionShape : createRegionShapes()) {
            Polygon polygon = new Polygon(osmRegionMapView);
            polygon.setPoints(regionShape.points);
            polygon.setTitle(regionShape.name);
            polygon.getFillPaint().setColor(regionShape.name.equals(currentUserRegion)
                    ? 0x55FFD43B
                    : 0x335C9ED8);
            polygon.getOutlinePaint().setColor(0xFFFFFFFF);
            polygon.getOutlinePaint().setStrokeWidth(dp(2));
            polygon.setOnClickListener((poly, mapView, eventPos) -> {
                showSelectedRegion(regionShape.name);
                return true;
            });
            osmRegionMapView.getOverlays().add(polygon);

            Marker label = new Marker(osmRegionMapView);
            label.setPosition(regionShape.center);
            label.setTitle(iconForRegion(regionShape.name) + " " + regionShape.name);
            label.setTextLabelBackgroundColor(0xCCFFFFFF);
            label.setTextLabelForegroundColor(0xFF071B4D);
            label.setTextLabelFontSize(dp(11));
            label.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            label.setOnMarkerClickListener((marker, mapView) -> {
                showSelectedRegion(regionShape.name);
                return true;
            });
            osmRegionMapView.getOverlays().add(label);
        }

        for (RegionMapPoint playerPoint : playerPoints) {
            Marker marker = new Marker(osmRegionMapView);
            marker.setPosition(new GeoPoint(playerPoint.y, playerPoint.x));
            marker.setTitle(playerPoint.username);
            marker.setSubDescription(playerPoint.region);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            osmRegionMapView.getOverlays().add(marker);
        }

        osmRegionMapView.invalidate();
    }

    private void showSelectedRegion(String regionName) {
        if (TextUtils.isEmpty(regionName)) {
            selectedRegionText.setText(R.string.regions_selected_empty);
            return;
        }

        Region region = regionsByName.get(regionName);
        if (region == null) {
            region = new Region(FirestoreRepository.regionCodeForName(regionName), regionName, iconForRegion(regionName));
            region.registeredPlayers = regionCounts.getOrDefault(regionName, 0);
            region.activePlayers = activeRegionCounts.getOrDefault(regionName, 0);
        }

        String marker = regionName.equals(currentUserRegion)
                ? getString(R.string.regions_your_region_suffix)
                : "";
        selectedRegionText.setText(getString(
                R.string.regions_detail_format,
                region.icon,
                region.name,
                marker,
                region.monthlyStars,
                region.firstPlaces,
                region.secondPlaces,
                region.thirdPlaces,
                region.activePlayers,
                region.registeredPlayers
        ));
        backToRankingButton.setVisibility(View.VISIBLE);
        resetMonthlyCycleButton.setVisibility(View.GONE);
        regionSummaryContainer.removeAllViews();
    }

    private void renderRegionRanking() {
        regionSummaryContainer.removeAllViews();
        backToRankingButton.setVisibility(View.GONE);
        resetMonthlyCycleButton.setVisibility(View.VISIBLE);
        selectedRegionText.setText(R.string.regions_monthly_ranking_title);

        List<Region> sortedRegions = new ArrayList<>(currentRegions);
        sortedRegions.sort((left, right) -> Integer.compare(right.monthlyStars, left.monthlyStars));

        for (int index = 0; index < sortedRegions.size(); index++) {
            Region region = sortedRegions.get(index);
            TextView row = new TextView(this);
            row.setTextColor(getResources().getColor(R.color.slagalica_dark_blue, getTheme()));
            row.setTextSize(13);
            row.setPadding(dp(8), dp(8), dp(8), dp(8));
            row.setBackgroundColor(region.name.equals(currentUserRegion)
                    ? getResources().getColor(R.color.slagalica_yellow, getTheme())
                    : getResources().getColor(R.color.white, getTheme()));
            String rankPrefix = rankPrefix(index + 1);
            String yourRegion = region.name.equals(currentUserRegion)
                    ? getString(R.string.regions_your_region_suffix)
                    : "";
            row.setText(getString(
                    R.string.regions_ranking_row_format,
                    rankPrefix,
                    region.icon,
                    region.name,
                    region.monthlyStars,
                    yourRegion
            ));
            row.setOnClickListener(v -> {
                showSelectedRegion(region.name);
            });
            regionSummaryContainer.addView(row);
        }
    }

    private void applyPlayerCountsToRegions() {
        for (Region region : currentRegions) {
            int countedPlayers = regionCounts.getOrDefault(region.name, 0);
            if (countedPlayers > region.registeredPlayers) {
                region.registeredPlayers = countedPlayers;
            }

            region.activePlayers = activeRegionCounts.getOrDefault(region.name, 0);
        }
    }

    private void syncPlayerCountsToFirestore() {
        firestoreRepository.updateRegionPlayerCounts(regionCounts, activeRegionCounts, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Counts are already reflected in the local UI model.
            }

            @Override
            public void onError(String error) {
                Toast.makeText(RegionMapActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String rankPrefix(int rank) {
        if (rank == 1) {
            return "1.";
        }
        if (rank == 2) {
            return "2.";
        }
        if (rank == 3) {
            return "3.";
        }
        return String.format(Locale.getDefault(), "%d.", rank);
    }

    private String iconForRegion(String regionName) {
        switch (regionName) {
            case "Belgrade":
                return "BG";
            case "Vojvodina":
                return "VO";
            case "Sumadija and Western Serbia":
                return "SW";
            case "Southern and Eastern Serbia":
                return "SE";
            case "Kosovo i Metohija":
                return "KM";
            default:
                return "--";
        }
    }

    private long stableSeed(User user) {
        String key = !TextUtils.isEmpty(user.id) ? user.id : user.username + user.region;
        return key != null ? key.hashCode() : System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (osmRegionMapView != null) {
            osmRegionMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (osmRegionMapView != null) {
            osmRegionMapView.onPause();
        }
        super.onPause();
    }

    private float minLon(RegionShape shape) {
        float value = 180f;
        for (GeoPoint point : shape.points) {
            value = Math.min(value, (float) point.getLongitude());
        }
        return value;
    }

    private float maxLon(RegionShape shape) {
        float value = -180f;
        for (GeoPoint point : shape.points) {
            value = Math.max(value, (float) point.getLongitude());
        }
        return value;
    }

    private float minLat(RegionShape shape) {
        float value = 90f;
        for (GeoPoint point : shape.points) {
            value = Math.min(value, (float) point.getLatitude());
        }
        return value;
    }

    private float maxLat(RegionShape shape) {
        float value = -90f;
        for (GeoPoint point : shape.points) {
            value = Math.max(value, (float) point.getLatitude());
        }
        return value;
    }

    private float randomInRange(Random random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private RegionShape findRegionShape(String regionName) {
        for (RegionShape shape : createRegionShapes()) {
            if (shape.name.equals(regionName)) {
                return shape;
            }
        }
        return null;
    }

    private boolean contains(List<GeoPoint> polygon, float latitude, float longitude) {
        boolean inside = false;
        int j = polygon.size() - 1;

        for (int i = 0; i < polygon.size(); i++) {
            GeoPoint pi = polygon.get(i);
            GeoPoint pj = polygon.get(j);
            double piLat = pi.getLatitude();
            double pjLat = pj.getLatitude();
            double piLon = pi.getLongitude();
            double pjLon = pj.getLongitude();

            boolean intersects = ((piLat > latitude) != (pjLat > latitude))
                    && (longitude < (pjLon - piLon) * (latitude - piLat) / (pjLat - piLat) + piLon);
            if (intersects) {
                inside = !inside;
            }
            j = i;
        }

        return inside;
    }

    private List<RegionShape> createRegionShapes() {
        return Arrays.asList(
                new RegionShape(
                        "Vojvodina",
                        new GeoPoint(45.35, 19.85),
                        new GeoPoint(46.17, 18.82),
                        new GeoPoint(46.18, 21.45),
                        new GeoPoint(45.40, 21.15),
                        new GeoPoint(44.65, 20.55),
                        new GeoPoint(44.65, 19.10)
                ),
                new RegionShape(
                        "Belgrade",
                        new GeoPoint(44.82, 20.46),
                        new GeoPoint(45.05, 20.12),
                        new GeoPoint(45.03, 20.82),
                        new GeoPoint(44.55, 20.78),
                        new GeoPoint(44.48, 20.25)
                ),
                new RegionShape(
                        "Sumadija and Western Serbia",
                        new GeoPoint(43.85, 20.10),
                        new GeoPoint(44.70, 18.90),
                        new GeoPoint(44.55, 20.25),
                        new GeoPoint(43.85, 21.05),
                        new GeoPoint(42.95, 20.65),
                        new GeoPoint(42.90, 19.20)
                ),
                new RegionShape(
                        "Southern and Eastern Serbia",
                        new GeoPoint(43.45, 22.10),
                        new GeoPoint(44.65, 20.55),
                        new GeoPoint(44.40, 22.85),
                        new GeoPoint(42.35, 23.05),
                        new GeoPoint(42.25, 21.15),
                        new GeoPoint(42.95, 20.65)
                ),
                new RegionShape(
                        "Kosovo i Metohija",
                        new GeoPoint(42.62, 20.90),
                        new GeoPoint(43.25, 20.25),
                        new GeoPoint(42.95, 21.65),
                        new GeoPoint(41.85, 21.45),
                        new GeoPoint(41.85, 20.05)
                )
        );
    }

    private static class RegionShape {
        final String name;
        final GeoPoint center;
        final List<GeoPoint> points;

        RegionShape(String name, GeoPoint center, GeoPoint... points) {
            this.name = name;
            this.center = center;
            this.points = Arrays.asList(points);
        }
    }
}
