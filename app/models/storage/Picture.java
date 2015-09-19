package models.storage;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.db.jpa.Model;
import play.exceptions.UnexpectedException;
import play.jobs.Job;
import play.libs.Images;
import play.modules.storage.StoragePlugin;
import play.modules.storage.impl.SObject;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

//@Entity
//@Table(name = "pictures", uniqueConstraints = {@UniqueConstraint(columnNames = {
//        "albumn", "path"})})
public class Picture extends Model {

    public String albumn;

    public String path;

    @Lob
    public byte[] thumbnail;

    public static Picture add(File file) throws IOException {
        return Picture.add(file, -1, -1);
    }

    public static Picture add(File file, int normalLen, int thumbnailLen)
            throws IOException {
        String schema = Play.configuration.getProperty("application.name");
        return add(schema, file, normalLen, thumbnailLen);
    }

    private transient Job removeJob_ = null;

    /**
     * Do the job to remove picture file from storage service The reason we need
     * a job to do this is to decouple it from \@PostRemove action as that
     * action is not guaranteed to be happen only on delete transaction
     * successful.
     */
    public void commitRemoveStorage() {
        if (null != removeJob_)
            removeJob_.now();
    }

    public void cancelRemoveStorage() {
        removeJob_ = null;
    }

    /**
     * Save the File into
     *
     * @param file the file to be added into gallery
     * @throws IOException
     */
    public static Picture add(String schema, File file, int normalLen,
                              int thumbnailLen) throws IOException {
        File nFile = resize_(file, normalLen);
        byte[] thumbnail = generateThumbnail_(file, thumbnailLen);

        String key = newKey_(schema);
        StoragePlugin.service.put(key, SObject.asSObject(key, nFile));
        nFile.delete();

        Picture pic = new Picture();
        pic.albumn = schema;
        pic.path = key;
        pic.thumbnail = thumbnail;
        pic.save();

        return pic;
    }

    private static class RemoveJob_ extends Job {
        String key_ = null;

        RemoveJob_(String key) {
            key_ = key;
        }

        public void doJob() throws Exception {
            StoragePlugin.service.remove(key_);
        }
    }

    @PostRemove
    public void removeFromStorage() {
        removeJob_ = new RemoveJob_(path);
    }

    private static File resize_(File file, int len) {
        if (len < 0) {
            //len = StoragePlugin.conf.getInt("picture.size.normal.length");
            len = 200;
        }

        try {
            BufferedImage img = ImageIO.read(file);
            int w = img.getWidth();
            int h = img.getHeight();
            if (Math.max(w, h) < len)
                return file;
            File nFile = File.createTempFile("pic_", null, Play.tmpDir);
            if (h < w)
                Images.resize(file, nFile, len, -1);
            else
                Images.resize(file, nFile, -1, len);
            return nFile;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    private static String newKey_(String schema) {
        return schema + File.separator + StoragePlugin.newKey();
    }

    private static byte[] generateThumbnail_(File file, int len) {
        if (len < 0) {
            //len = StoragePlugin.conf.getInt("picture.size.thumbnail.length");
            len = 100;
        }

        File nFile = resize_(file, len);

        try {
            return SObject.asSObject("", nFile).asByteArray();
        } catch (Exception e) {
            throw new UnexpectedException(e);
        } finally {
            if (null != nFile) {
                try {
                    nFile.delete();
                } catch (Exception e) {
                }
            }
        }
    }

    public static List<Picture> findOrphans() {
        return JPA.em().createNativeQuery("select * from v_picture_orphans",
                Picture.class).getResultList();
    }

    public static void cleanOrphans() {
        List<Picture> list = Picture.findOrphans();
        try {
            for (Picture pic : list) {
                pic.delete();
            }
            JPA.em().getTransaction().commit();
            for (Picture pic : list) {
                pic.commitRemoveStorage();
            }
            Logger.info("storage: orphan pictures cleaned");
        } catch (Exception e) {
        }
    }
}
