package play.modules.storage.impl;

import play.Play;
import play.libs.Files;
import play.libs.IO;
import play.modules.storage.ISObject;
import play.modules.storage.IStorageService;
import play.modules.storage.StoragePlugin;
import play.vfs.VirtualFile;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FileStorageService implements IStorageService {

    public static final String CONF_HOME = "storage.file.dir";

    private VirtualFile root_ = null;
    private String urlRoot_ = null;

    public FileStorageService() {
    }

    public void configure(Map<String, String> conf) {
        if (null == conf) throw new NullPointerException();

        String s = conf.get("storage.file.dir");
        VirtualFile vf = VirtualFile.open(Play.applicationPath);
        root_ = vf.child(s);
        if (!root_.exists())
            root_.getRealFile().mkdir();
        else if (!root_.isDirectory()) throw new RuntimeException("cannot create root dir for file storage");
        urlRoot_ = conf.get("storage.url.root").replace('\\', '/');
        if (!urlRoot_.endsWith("/")) {
            urlRoot_ = urlRoot_ + '/';
        }
    }

    public FileStorageService(Map<String, String> conf) {
        configure(conf);
    }

    @Override
    public String getUrl(String key) {
        return urlRoot_ + key;
    }

    @Override
    public ISObject get(String key) {
        key = key.replace('\\', '/');
        String[] path = key.split("/");
        int l = path.length;
        VirtualFile vf = root_;
        for (int i = 0; i < l; ++i) {
            vf = vf.child(path[i]);
        }
        return SObject.asSObject(key, vf.getRealFile());
    }

    @Override
    public void put(String key, ISObject stuff) throws IOException {
        key = key.replace('\\', '/');
        String[] path = key.split("/");
        int l = path.length;
        VirtualFile vf = root_;
        for (int i = 0; i < l - 1; ++i) {
            vf = vf.child(path[i]);
            if (!vf.exists()) {
                vf.getRealFile().mkdir();
            } else {
                if (!vf.isDirectory()) {
                    throw new IOException("cannot store the object into storage: " + vf.getName() + " is not a directory");
                }
            }
        }
        VirtualFile vfObj = vf.child(path[l - 1]);
        OutputStream os = new BufferedOutputStream(vfObj.outputstream());
        IO.write(new BufferedInputStream(stuff.asInputStream()), os);
        os.close();
        
        if (stuff.hasAttribute()) {
            VirtualFile vfAttr = vf.child(path[l - 1] + ".attr");
            os = new BufferedOutputStream(vfAttr.outputstream());
            Properties p = new Properties();
            p.putAll(stuff.getAttributes());
            p.store(os, "");
            os.close();
        }
    }

    @Override
    public ISObject remove(String key) {
        key = key.replace('\\', '/');
        String[] path = key.split("/");
        int l = path.length;
        VirtualFile vf = root_;
        for (int i = 0; i < l - 1; ++i) {
            vf = vf.child(path[i]);
        }
        VirtualFile vfObj = vf.child(path[l - 1]);
        if (!vfObj.exists()) {
            return null;
        }
        File tmpFile = SObject.createTempFile();
        Files.copy(vfObj.getRealFile(), tmpFile);
        vfObj.getRealFile().delete();
        VirtualFile vfAttr = vf.child(path[l - 1] + ".attr");
        if (vfAttr.exists()) {
            vfAttr.getRealFile().delete();
        }
        return SObject.asSObject(key, tmpFile);
    }

    public static void main(String[] args) throws Exception {
        if (null == Play.applicationPath) {
            Play.applicationPath = new File("C:\\");
        }

        Map<String, String> conf = new HashMap<String, String>();
        
        conf.put("dir", "storage");
        FileStorageService fss = new FileStorageService(conf);
        String key = StoragePlugin.newKey();
        ISObject obj = SObject.asSObject(key, "Hello World");
        fss.put("hello.world", obj);
        obj = fss.get("hello.world");
        IO.write(obj.asInputStream(), System.out);
        System.out.println();

        File f = new File("t:\\tmp\\src\\play.plugins");
        key = "src" + File.separator + "play.plugins";
        fss.put(key, SObject.asSObject(key, f));
        obj = fss.get(key);
        IO.write(obj.asInputStream(), System.out);
        System.out.println();

        obj = fss.remove(key);
        IO.write(obj.asInputStream(), System.out);
        System.out.println();
    }

}
