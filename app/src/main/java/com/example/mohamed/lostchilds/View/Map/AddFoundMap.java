package com.example.mohamed.lostchilds.View.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.example.mohamed.lostchilds.R;
import com.example.mohamed.lostchilds.model.Coordinates;
import com.example.mohamed.lostchilds.model.FoundModel;
import com.example.mohamed.lostchilds.model.IndividualLocation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.building.BuildingPlugin;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;


import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfConversion;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.ln;
import static com.mapbox.mapboxsdk.style.expressions.Expression.neq;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

/**
 * Activity with a Mapbox map and recyclerview to view various locations
 */
public class AddFoundMap extends AppCompatActivity implements
        MapboxMap.OnMapClickListener, PermissionsListener {


    private static final LatLngBounds LOCKED_MAP_CAMERA_BOUNDS = new LatLngBounds.Builder()
            .include(new LatLng(40.87096725853152, -74.08277394720501))
            .include(new LatLng(40.67035340371385,
                    -73.87063900287112)).build();

    private static final int MAPBOX_LOGO_OPACITY = 75;
    private static final int CAMERA_MOVEMENT_SPEED_IN_MILSECS = 1200;
    private static final float NAVIGATION_LINE_WIDTH = 9;
    private static final float BUILDING_EXTRUSION_OPACITY = .8f;
    private static final String PROPERTY_SELECTED = "selected";
    private static final String BUILDING_EXTRUSION_COLOR = "#c4dbed";
    private DirectionsRoute currentRoute;
    private FeatureCollection featureCollection;
    private MapboxMap mapboxMap;
    private MapView mapView;
    private ArrayList<IndividualLocation> listOfIndividualLocations;
    private CustomThemeManager customThemeManager;
    private int chosenTheme;
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    private static LatLng MOCK_DEVICE_LOCATION_LAT_LNG;
    Point mockCurrentLocation, selectedFeaturePoint;
    private FloatingActionButton Fab_Search;
    private int REQUEST_CODE_AUTOCOMPLETE=11;
    GeoJsonSource source;
    Marker marker;
    DatabaseReference databaseReference;
    FirebaseDatabase firebaseDatabase;
    Bundle bundle;
    @SuppressWarnings( {"MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);





        // Configure the Mapbox access token. Configuration can either be called in your application
        // class or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // Hide the status bar for the map to fill the entire screen
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Inflate the layout with the the MapView. Always inflate this after the Mapbox access token is configured.
        setContentView(R.layout.activity_map);

        initWidgets();

        actionWidgets();



        // Create a GeoJSON feature collection from the GeoJSON file in the assets folder.
        try {
            getFeatureCollectionFromJson();
        } catch (Exception exception) {
            Log.e("MapsActivity", "onCreate: " + exception);
            Toast.makeText(this,"failure_to_load_file", Toast.LENGTH_LONG).show();
        }

        // Initialize a list of IndividualLocation objects for future use with recyclerview
        listOfIndividualLocations = new ArrayList<>();

        // Initialize the theme that was selected in the previous activity. The blue theme is set as the backup default.
//        chosenTheme = getIntent().getIntExtra(StringConstants.SELECTED_THEME, R.style.AppTheme_Blue);
        chosenTheme = R.style.AppTheme_Neutral;


        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {

                // Initialize the custom class that handles marker icon creation and map styling based on the selected theme
                customThemeManager = new CustomThemeManager(chosenTheme, AddFoundMap.this);

                mapboxMap.setStyle(new Style.Builder().fromUrl(customThemeManager.getMapStyle()), new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {

                        // Setting the returned mapboxMap object (directly above) equal to the "globally declared" one
                        AddFoundMap.this.mapboxMap = mapboxMap;

                        // Adjust the opacity of the Mapbox logo in the lower left hand corner of the map
                        ImageView logo = mapView.findViewById(R.id.logoView);
                        logo.setAlpha(MAPBOX_LOGO_OPACITY);

                        // Set bounds for the map camera so that the user can't pan the map outside of the NYC area
//                        mapboxMap.setLatLngBoundsForCameraTarget(LOCKED_MAP_CAMERA_BOUNDS);

                        // Set up the SymbolLayer which will
                        // show the icons for each store location
                        enableLocationComponent(style);

                        GetData();




                    }

                });

            }
        });

    }


    public void initWidgets(){



        // Set up the Mapbox map
        mapView = findViewById(R.id.mapView);
        Fab_Search=findViewById(R.id.fab);

    }


    public void actionWidgets(){



        Fab_Search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(getString(R.string.access_token))
                        .build(AddFoundMap.this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
            }
        });







    }


    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);


            // Set the component's camera mode
             locationComponent.setCameraMode(CameraMode.TRACKING);



        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }


    private void showBuildingExtrusions() {
        // Use the Mapbox building plugin to display and customize the opacity/color of building extrusions
        BuildingPlugin buildingPlugin = new BuildingPlugin(mapView, mapboxMap, mapboxMap.getStyle());
        buildingPlugin.setVisibility(true);
        buildingPlugin.setOpacity(BUILDING_EXTRUSION_OPACITY);
        buildingPlugin.setColor(Color.parseColor(BUILDING_EXTRUSION_COLOR));
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        handleClickIcon(mapboxMap.getProjection().toScreenLocation(point));
        return true;
    }
    @SuppressWarnings( {"MissingPermission"})
    private boolean handleClickIcon(PointF screenPoint) {

        MOCK_DEVICE_LOCATION_LAT_LNG = new LatLng(locationComponent.getLastKnownLocation().getLongitude(),locationComponent.getLastKnownLocation().getLatitude());


        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, "store-location-layer-id");
        if (!features.isEmpty()) {
            String name = features.get(0).getStringProperty("name");
            List<Feature> featureList = featureCollection.features();
            for (int i = 0; i < featureList.size(); i++) {

                if (featureList.get(i).getStringProperty("name").equals(name)) {
                    selectedFeaturePoint = (Point) featureList.get(i).geometry();



                    if (featureSelectStatus(i)) {
                        setFeatureSelectState(featureList.get(i), false);
                    } else {
                        setSelected(i);
                    }
                    if (selectedFeaturePoint.latitude() != MOCK_DEVICE_LOCATION_LAT_LNG.getLatitude()) {
                        for (int x = 0; x < featureCollection.features().size(); x++) {

                            if (listOfIndividualLocations.get(x).getLocation().getLatitude() == selectedFeaturePoint.latitude()) {
                                // Scroll the recyclerview to the selected marker's card. It's "x-1" below because
                                // the mock device location marker is part of the marker list but doesn't have its own card
                                // in the actual recyclerview.




                                Intent intent = new Intent();
                                intent.putExtra("getLatitude", listOfIndividualLocations.get(x).getLocation().getLatitude());
                                intent.putExtra("getLongitude", listOfIndividualLocations.get(x).getLocation().getLongitude());

                                setResult(RESULT_OK, intent);
                                finish();


                                Toast.makeText(this, ""+listOfIndividualLocations.get(x).getName(), Toast.LENGTH_SHORT).show();




                                break;
                            }
                        }
                    }
                } else {
                    setFeatureSelectState(featureList.get(i), false);
                }
            }


            return true;
        } else {
            return false;
        }
    }





    /**
     * The LocationRecyclerViewAdapter's interface which listens to clicks on each location's card
     *
     * @param selectedPoint the clicked card's position/index in the overall list of cards
     */
    @SuppressWarnings( {"MissingPermission"})

    public void onItemClick(Point selectedPoint) {



        // Reposition the map camera target to the selected marker
        if (selectedPoint != null) {
            //repositionMapCamera(selectedPoint);
        }

        // Check for an internet connection before making the call to Mapbox Directions API
        if (deviceHasInternetConnection()) {
            // Start call to the Mapbox Directions API

            MOCK_DEVICE_LOCATION_LAT_LNG = new LatLng(locationComponent.getLastKnownLocation().getLongitude(),locationComponent.getLastKnownLocation().getLatitude());

            // Set up origin and destination coordinates for the call to the Mapbox Directions API
            mockCurrentLocation = Point.fromLngLat(MOCK_DEVICE_LOCATION_LAT_LNG.getLatitude(),
                    MOCK_DEVICE_LOCATION_LAT_LNG.getLongitude());



        } else {
            Toast.makeText(this, R.string.no_internet_message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Adds a SymbolLayer which will show all of the location's icons
     */
    private void initStoreLocationIconSymbolLayer() {
        try {


        Style style = mapboxMap.getStyle();
        if (style != null) {
            // Add the icon image to the map
            style.addImage("store-location-icon-id", customThemeManager.getUnselectedMarkerIcon());

            // Create and add the GeoJsonSource to the map
            GeoJsonSource storeLocationGeoJsonSource = new GeoJsonSource("store-location-source-id");
            style.addSource(storeLocationGeoJsonSource);

            // Create and add the store location icon SymbolLayer to the map
            SymbolLayer storeLocationSymbolLayer = new SymbolLayer("store-location-layer-id",
                    "store-location-source-id");
            storeLocationSymbolLayer.withProperties(
                    iconImage("store-location-icon-id"),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true)
            );
            style.addLayer(storeLocationSymbolLayer);

        } else {
            Log.d("StoreFinderActivity", "initStoreLocationIconSymbolLayer: Style isn't ready yet.");

//            throw new IllegalStateException("Style isn't ready yet.");
        }
        }catch (Exception e){
            finish();
        }
    }

    /**
     * Adds a SymbolLayer which will show the selected location's icon
     */
    private void initSelectedStoreSymbolLayer() {
        Style style = mapboxMap.getStyle();
        if (style != null) {

            // Add the icon image to the map
            style.addImage("selected-store-location-icon-id", customThemeManager.getSelectedMarkerIcon());

            // Create and add the store location icon SymbolLayer to the map
            SymbolLayer selectedStoreLocationSymbolLayer = new SymbolLayer("selected-store-location-layer-id",
                    "store-location-source-id");
            selectedStoreLocationSymbolLayer.withProperties(
                    iconImage("selected-store-location-icon-id"),
                    iconAllowOverlap(true)
            );
            selectedStoreLocationSymbolLayer.withFilter(eq((get(PROPERTY_SELECTED)), literal(true)));
            style.addLayer(selectedStoreLocationSymbolLayer);
        } else {
            Log.d("StoreFinderActivity", "initSelectedStoreSymbolLayer: Style isn't ready yet.");
//            throw new IllegalStateException("Style isn't ready yet.");
            finish();
        }
    }

    /**
     * Checks whether a Feature's boolean "selected" property is true or false
     *
     * @param index the specific Feature's index position in the FeatureCollection's list of Features.
     * @return true if "selected" is true. False if the boolean property is false.https://hassan-elkhadrawy.000webhostapp.com/list_of_locations.geojson
     */
    private boolean featureSelectStatus(int index) {
        if (featureCollection == null) {
            return false;
        }
        return featureCollection.features().get(index).getBooleanProperty(PROPERTY_SELECTED);
    }

    /**
     * Set a feature selected state.
     *
     * @param index the index of selected feature
     */
    private void setSelected(int index) {
        Feature feature = featureCollection.features().get(index);
        setFeatureSelectState(feature, true);
        refreshSource();
    }

    /**
     * Selects the state of a feature
     *
     * @param feature the feature to be selected.
     */
    private void setFeatureSelectState(Feature feature, boolean selectedState) {
        feature.properties().addProperty(PROPERTY_SELECTED, selectedState);
        refreshSource();
    }


    /**
     * Updates the display of data on the map after the FeatureCollection has been modified
     */
    private void refreshSource() {
        source = mapboxMap.getStyle().getSourceAs("store-location-source-id");
        if (source != null && featureCollection != null) {
            source.setGeoJson(featureCollection);
        }
    }

    private void getInformationFromDirectionsApi(Point destinationPoint,
                                                 final boolean fromMarkerClick, @Nullable final Integer listIndex) {
        // Set up origin and destination coordinates for the call to the Mapbox Directions API
        Point mockCurrentLocation = Point.fromLngLat(MOCK_DEVICE_LOCATION_LAT_LNG.getLongitude(),
                MOCK_DEVICE_LOCATION_LAT_LNG.getLatitude());

        Point destinationMarker = Point.fromLngLat(destinationPoint.longitude(), destinationPoint.latitude());

        // Initialize the directionsApiClient object for eventually drawing a navigation route on the map
        MapboxDirections directionsApiClient = MapboxDirections.builder()
                .origin(mockCurrentLocation)
                .destination(destinationMarker)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .accessToken(getString(R.string.access_token))
                .build();

        directionsApiClient.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                // Check that the response isn't null and that the response has a route
                if (response.body() == null) {
                    Log.e("MapsActivity", "No routes found, make sure you set the right user and access token.");
                } else if (response.body().routes().size() < 1) {
                    Log.e("MapsActivity", "No routes found");
                } else {
                    if (fromMarkerClick) {
                        // Retrieve and draw the navigation route on the map
                        currentRoute = response.body().routes().get(0);
                        drawNavigationPolylineRoute(currentRoute);
                    } else {
                        // Use Mapbox Turf helper method to convert meters to miles and then format the mileage number
                        DecimalFormat df = new DecimalFormat("#.#");
                        String finalConvertedFormattedDistance = String.valueOf(df.format(TurfConversion.convertLength(
                                response.body().routes().get(0).distance(), TurfConstants.UNIT_METERS,
                                TurfConstants.UNIT_MILES)));

                    }
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Toast.makeText(AddFoundMap.this, R.string.failure_to_retrieve, Toast.LENGTH_LONG).show();
            }
        });
    }








    private void repositionMapCamera(Point newTarget) {
        CameraPosition newCameraPosition = new CameraPosition.Builder()
                .target(new LatLng(newTarget.latitude(), newTarget.longitude()))
                .build();
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition), CAMERA_MOVEMENT_SPEED_IN_MILSECS);
    }
    @SuppressWarnings( {"MissingPermission"})
    private void addMockDeviceLocationMarkerToMap() {
        // Add the fake user location marker to the map
        Style style = mapboxMap.getStyle();
        if (style != null) {
            // Add the icon image to the map
            style.addImage("mock-device-location-icon-id", customThemeManager.getMockLocationIcon());

            // Point originPoint = Point.fromLngLat();
            style.addSource(new GeoJsonSource("mock-device-location-source-id", Feature.fromGeometry(
                    Point.fromLngLat(MOCK_DEVICE_LOCATION_LAT_LNG.getLongitude(), MOCK_DEVICE_LOCATION_LAT_LNG.getLatitude()))));

            style.addLayer(new SymbolLayer("mock-device-location-layer-id",
                    "mock-device-location-source-id").withProperties(
                    iconImage("mock-device-location-icon-id"),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true)
            ));
        } else {
//            throw new IllegalStateException("Style isn't ready yet.");
        }
    }

    private void getFeatureCollectionFromJson() throws IOException {
        try {
            // Use fromJson() method to convert the GeoJSON file into a usable FeatureCollection object
            featureCollection = FeatureCollection.fromJson(loadGeoJsonFromAsset("list_of_locations.geojson"));

        } catch (Exception exception) {
            Log.e("MapsActivity", "getFeatureCollectionFromJson: " + exception);
        }
    }

    private String loadGeoJsonFromAsset(String filename) {
        try {
            // Load the GeoJSON file from the local asset folder
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (Exception exception) {
            Log.e("MapsActivity", "Exception Loading GeoJSON: " + exception.toString());
            exception.printStackTrace();
            return null;
        }
    }


    private void drawNavigationPolylineRoute(DirectionsRoute route) {
        // Retrieve and update the source designated for showing the store location icons
        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("navigation-route-source-id");
        if (source != null) {
            source.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(
                    LineString.fromPolyline(route.geometry(), PRECISION_6))));
        }
    }

    private void initNavigationPolylineLineLayer() {
        // Create and add the GeoJsonSource to the map
        GeoJsonSource navigationLineLayerGeoJsonSource = new GeoJsonSource("navigation-route-source-id");
        try {
            mapboxMap.getStyle().addSource(navigationLineLayerGeoJsonSource);

        }catch (Exception e){

            finish();
        }

        // Create and add the LineLayer to the map to show the navigation route line
        LineLayer navigationRouteLineLayer = new LineLayer("navigation-route-layer-id",
                navigationLineLayerGeoJsonSource.getId());
        navigationRouteLineLayer.withProperties(
                lineColor(customThemeManager.getNavigationLineColor()),
                lineWidth(NAVIGATION_LINE_WIDTH)
        );
        mapboxMap.getStyle().addLayerBelow(navigationRouteLineLayer, "store-location-layer-id");
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "dcd", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, "jh", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // Add the mapView's lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        finish();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        finish();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
        finish();
    }

    private boolean deviceHasInternetConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getApplicationContext().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * Custom class which creates marker icons and colors based on the selected theme
     */
    class CustomThemeManager {
        private int selectedTheme;
        private Context context;
        private Bitmap unselectedMarkerIcon;
        private Bitmap selectedMarkerIcon;
        private Bitmap mockLocationIcon;
        private int navigationLineColor;
        private String mapStyle;

        CustomThemeManager(int selectedTheme, Context context) {
            this.selectedTheme = selectedTheme;
            this.context = context;
            initializeTheme();
        }

        private void initializeTheme() {
            switch (selectedTheme) {

                case R.style.AppTheme_Neutral:
                    mapStyle = Style.MAPBOX_STREETS;
                    navigationLineColor = getResources().getColor(R.color.navigationRouteLine_neutral);
                    unselectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.gray_selected_house);
                    selectedMarkerIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.gray_selected_house);
                    mockLocationIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.gray_selected_house);
                    break;

            }
        }

        public Bitmap getUnselectedMarkerIcon() {
            return unselectedMarkerIcon;
        }

        public Bitmap getMockLocationIcon() {
            return mockLocationIcon;
        }

        public Bitmap getSelectedMarkerIcon() {
            return selectedMarkerIcon;
        }

        int getNavigationLineColor() {
            return navigationLineColor;
        }

        public String getMapStyle() {
            return mapStyle;
        }
    }




    @SuppressWarnings( {"MissingPermission"})

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            CarmenFeature feature = PlaceAutocomplete.getPlace(data);
            String PLACE_NAME= PlaceAutocomplete.getPlace(data).placeName();

            Point singleLocationPosition = (Point) feature.geometry();

            Point destinationPoint = Point.fromLngLat(singleLocationPosition.longitude(), singleLocationPosition.latitude());
            Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                    locationComponent.getLastKnownLocation().getLatitude());



        }
    }



    private void GetData() {



        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {




                Object object = dataSnapshot.getValue(Object.class);
                String json = new Gson().toJson(object);
                Log.d("json",json);

                featureCollection = FeatureCollection.fromJson(json);

                // Set up the SymbolLayer which will show the icons for each store location
                initStoreLocationIconSymbolLayer();

                // Set up the SymbolLayer which will show the selected store icon
                initSelectedStoreSymbolLayer();

                // Set up the LineLayer which will show the navigation route line to a particular store location
                initNavigationPolylineLineLayer();

                // Create a list of features from the feature collection
                List<Feature> featureList = featureCollection.features();

                // Retrieve and update the source designated for showing the store location icons
                try {
                    GeoJsonSource source = mapboxMap.getStyle().getSourceAs("store-location-source-id");
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(featureList));
                    }
                }catch (Exception e){

                    finish();
                }









                if (featureList != null) {

                    for (int x = 0; x < featureList.size(); x++) {

                        Feature singleLocation = featureList.get(x);

                        // Get the single location's String properties to place in its map marker
                        String singleLocationName = singleLocation.getStringProperty("name");


                        String singleLocationHours = "1h 20m";
                        String singleLocationDescription ="Sea Street , Cairo Governorate";
                        String singleLocationPhoneNum = singleLocation.getStringProperty("phone");


                        // Add a boolean property to use for adjusting the icon of the selected store location
                        singleLocation.addBooleanProperty(PROPERTY_SELECTED, false);

                        // Get the single location's LatLng coordinates
                        Point singleLocationPosition = (Point) singleLocation.geometry();
//                    Toast.makeText(MapsActivity.this, ""+singleLocationPosition.latitude(), Toast.LENGTH_SHORT).show();

                        // Create a new LatLng object with the Position object created above
                        LatLng singleLocationLatLng = new LatLng(singleLocationPosition.latitude(),
                                singleLocationPosition.longitude());

                        // Add the location to the Arraylist of locations for later use in the recyclerview
                        listOfIndividualLocations.add(new IndividualLocation(
                                singleLocationName,
                                singleLocationDescription,
                                singleLocationHours,
                                singleLocationPhoneNum,
                                singleLocationLatLng
                        ));


                    }




                    mapboxMap.addOnMapClickListener(AddFoundMap.this);


                    // Show 3d buildings if the blue theme is being used
                    if (customThemeManager.getNavigationLineColor() == R.color.colorAccent) {
                        showBuildingExtrusions();
                    }
                }






            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Toast.makeText(AddFoundMap.this, "Failed to read value.", Toast.LENGTH_SHORT).show();
            }
        });




    }




}
