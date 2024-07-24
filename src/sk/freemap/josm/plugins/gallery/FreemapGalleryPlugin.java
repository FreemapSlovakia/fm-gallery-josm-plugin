package sk.freemap.josm.plugins.gallery;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ImageData;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.NavigatableComponent.ZoomChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Logging;
import java.awt.event.ActionEvent;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

public class FreemapGalleryPlugin extends Plugin implements LayerChangeListener, ZoomChangeListener, Runnable {

    private static final String API_URL_TEMPLATE = "https://backend.freemap.sk/gallery/pictures?by=bbox&bbox=%f,%f,%f,%f&fields=id&fields=user&fields=user&fields=takenAt&fields=title";

    private GeoImageLayer photoLayer;

    private Thread thread;

    public FreemapGalleryPlugin(PluginInformation info) {
        super(info);

        MainApplication.getLayerManager().addLayerChangeListener(this);

        addMenuEntry();
    }

    private void addMenuEntry() {
        JosmAction action = new JosmAction("Freemap Gallery", "icon.svg", "Add dynamic Freemap.sk photos layer (from zoom 14)", null, false, null, false) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (photoLayer != null) {
                    return;
                }

                photoLayer = new GeoImageLayer(new ArrayList<>(), null);

                MainApplication.getLayerManager().addLayer(photoLayer);

                new Thread(() -> {
                    fetchAndDisplayPhotos(MainApplication.getMap().mapView.getProjection()
                            .getLatLonBoundsBox(MainApplication.getMap().mapView.getProjectionBounds()));
                }).start();

                NavigatableComponent.addZoomChangeListener(FreemapGalleryPlugin.this);
            }
        };

        MainMenu.add(MainApplication.getMenu().imageryMenu, action);
    }

    @Override
    public synchronized void zoomChanged() {
        if (thread != null) {
            thread.interrupt();
        }

        thread = new Thread(this);

        thread.start();
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        if (e.getAddedLayer() instanceof GeoImageLayer) {
            photoLayer = (GeoImageLayer) e.getAddedLayer();
        }
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        if (photoLayer == e.getRemovedLayer()) {
            photoLayer = null;
        }
    }

    private void fetchAndDisplayPhotos(Bounds bounds) {
        var imageData = photoLayer.getImageData();

        if (MainApplication.getMap().mapView.getScale() > 10) {
            for (var image : new ArrayList<>(imageData.getImages())) {
                imageData.removeImage(image);
            }

            photoLayer.invalidate();

            return;
        }

        try {
            String url = String.format(Locale.US, API_URL_TEMPLATE, bounds.getMinLon(), bounds.getMinLat(),
                    bounds.getMaxLon(),
                    bounds.getMaxLat());

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

            connection.setRequestMethod("GET");

            try (JsonReader jsonReader = Json.createReader(new InputStreamReader(connection.getInputStream()))) {
                JsonArray photosArray = jsonReader.readArray();

                List<ImageEntry> photoEntries = new ArrayList<>();

                for (JsonObject photoObject : photosArray.getValuesAs(JsonObject.class)) {
                    var title = photoObject.get("title");

                    var takenAt = photoObject.get("takenAt");

                    photoEntries.add(new GalleryImageEntry(photoObject.getInt("id"),
                            title instanceof JsonString s ? s.getString() : null,
                            photoObject.getJsonString("user").getString(),
                            takenAt instanceof JsonNumber n ? n.longValue() : null,
                            new LatLon(photoObject.getJsonNumber("lat").doubleValue(),
                                    photoObject.getJsonNumber("lon").doubleValue())));
                }

                for (var image : new ArrayList<>(imageData.getImages())) {
                    if (!photoEntries.contains(image)) {
                        imageData.removeImage(image);
                    }
                }

                imageData.mergeFrom(new ImageData(photoEntries));

                photoLayer.invalidate();
            }
        } catch (Exception ex) {
            Logging.error(ex);
        }
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent arg0) {
        // nothing
    }

    @Override
    public void run() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            return;
        }

        fetchAndDisplayPhotos(MainApplication.getMap().mapView.getProjection()
                .getLatLonBoundsBox(MainApplication.getMap().mapView.getProjectionBounds()));

        thread = null;
    }
}
