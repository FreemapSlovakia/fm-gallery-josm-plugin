package sk.freemap.josm.plugins.gallery;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Objects;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.tools.JosmRuntimeException;

public class GalleryImageEntry extends ImageEntry {
    private static final String PHOTO_URL_TEMPLATE = "https://backend.freemap.sk/gallery/pictures/%d/image";

    private final int id;

    private String user;

    private Long takenAt;

    private String title;

    GalleryImageEntry(int id, String title, String user, Long takenAt, LatLon latLon) {
        this.id = id;
        this.title = title;
        this.user = user;
        this.takenAt = takenAt;

        setPos(latLon);
    }

    @Override
    protected URL getImageUrl() throws MalformedURLException {
        return new URL(String.format(PHOTO_URL_TEMPLATE, id));
    }

    @Override
    public URI getImageURI() {
        try {
            return new URI(String.format(PHOTO_URL_TEMPLATE, id));
        } catch (URISyntaxException e) {
            // This should never happen.
            throw new JosmRuntimeException(this.toString(), e);
        }
    }

    @Override
    public String getDisplayName() {
        return (title == null ? "Photo" : title) + " by " + user
                + (takenAt == null ? "" : (" taken at " + new Date(takenAt * 1000L)));
    }

    @Override
    public String toString() {
        return "Iamge:" + id;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj) || getClass() != obj.getClass()) {
            return false;
        }

        GalleryImageEntry other = (GalleryImageEntry) obj;

        return id == other.id;
    }

    @Override
    public File getFile() {
        return new File("/_josm_fm_/" + id);
    }
}
