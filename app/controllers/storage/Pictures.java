package controllers.storage;

import models.storage.Picture;
import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.modules.storage.ISObject;
import play.modules.storage.StoragePlugin;
import play.mvc.Controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Pictures extends Controller {

    public static void get(String id) {
        if (StringUtils.isNumeric(id)) {
            Long l = Long.valueOf(id);
            getById_(l);
        } else {
            getByKey_(id);
        }
    }

    public static void getThumbnail(String id) {
        Picture pic;
        if (StringUtils.isNumeric(id)) {
            Long l = Long.valueOf(id);
            pic = Picture.findById(l);
        } else {
            pic = Picture.find("byKey", id).first();
        }

        byte[] data = pic.thumbnail;
        InputStream is = new ByteArrayInputStream(data);
        renderBinary(is);
    }

    private static void getById_(Long id) {
        Picture pic = Picture.findById(id);
        notFoundIfNull(pic);

        getByKey_(pic.path);
    }

    private static void getByKey_(String key) {
        ISObject sobj = StoragePlugin.service.get(key);
        InputStream is = null;
        try {
            is = new BufferedInputStream(sobj.asInputStream());
            response.contentType = "image";
            renderBinary(is);
        } catch (IOException e) {
            Logger.error(e, "Error retreive picture from storage by key [%1$s]", key);
            error(e);
        }
    }

    public static void listOrphans() {
        List<Picture> list = Picture.findOrphans();
        render(list);
    }
}