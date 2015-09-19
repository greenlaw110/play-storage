package play.modules.storage.impl;

import play.data.parsing.TempFilePlugin;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.modules.storage.ISObject;
import play.modules.storage.StoragePlugin;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public abstract class SObject implements ISObject {
    private String key;
    private Map<String, String> attrs = new HashMap<String, String>();
    private SObject(String key) {
        if (null == key) {
            throw new NullPointerException();
        }
        this.key = key;
    }

    public static ISObject getDumpObject(String key) {
        return asSObject(key, "");
    }
    
    public String getKey() {
        return key;
    }

    @Override
    public String getAttribute(String key) {
        return attrs.get(key);
    }

    @Override
    public void setAttribute(String key, String val) {
        attrs.put(key, val);
    }

    @Override
    public boolean hasAttribute() {
        return !attrs.isEmpty();
    }

    @Override
    public Map<String, String> getAttributes() {
        return new HashMap<String, String>(attrs);
    }

    @Override
    public void save() throws IOException {
        StoragePlugin.service.put(key, this);
    }

    public String getUrl() {
        return StoragePlugin.service.getUrl(getKey());
    }

    public static ISObject asSObject(String key, File f) {
        return new FileSObject(key, f);
    }

    public static ISObject asSObject(String key, InputStream is) {
        return new InputStreamSObject(key, is);
    }

    public static ISObject asSObject(String key, String s) {
        return new StringSObject(key, s);
    }

    public static ISObject asSObject(String key, byte[] buf) {
        return new ByteArraySObject(key, buf);
    }

    static File createTempFile() {
        File tmpDir = TempFilePlugin.createTempFolder();
        try {
            return File.createTempFile("sobj_", null, tmpDir);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    private static class FileSObject extends SObject {
        private File f_ = null;

        FileSObject(String key, File f) {
            super(key);
            if (null == f)
                throw new NullPointerException();
            f_ = f;
        }

        @Override
        public byte[] asByteArray() throws IOException {
            return IO.readContent(f_);
        }

        @Override
        public File asFile() {
            return f_;
        }

        @Override
        public InputStream asInputStream() throws IOException {
            return new FileInputStream(f_);
        }

        @Override
        public String asString() throws IOException {
            return IO.readContentAsString(f_);
        }

        @Override
        public long getLength() {
            return f_.length();
        }
    }

    private static class StringSObject extends SObject {
        private String s_ = null;

        StringSObject(String key, String s) {
            super(key);
            if (null == s)
                throw new NullPointerException();
            s_ = s;
        }

        @Override
        public byte[] asByteArray() {
            return s_.getBytes();
        }

        @Override
        public File asFile() throws IOException {
            File tmpFile = SObject.createTempFile();
            IO.writeContent(s_, tmpFile);
            return tmpFile;
        }

        @Override
        public InputStream asInputStream() {
            return new ByteArrayInputStream(asByteArray());
        }

        @Override
        public String asString() {
            return s_;
        }

        @Override
        public long getLength() {
            return s_.length();
        }
    }

    private static class InputStreamSObject extends SObject {
        private InputStream is_ = null;

        InputStreamSObject(String key, InputStream is) {
            super(key);
            if (null == is)
                throw new NullPointerException();
            is_ = is;
        }

        @Override
        public byte[] asByteArray() throws IOException {
            return IO.readContent(is_);
        }

        @Override
        public File asFile() throws IOException {
            File tmpFile = SObject.createTempFile();
            IO.write(is_, new BufferedOutputStream(
                    new FileOutputStream(tmpFile)));
            return tmpFile;
        }

        @Override
        public InputStream asInputStream() {
            return is_;
        }

        @Override
        public String asString() throws IOException {
            return IO.readContentAsString(is_);
        }

        @Override
        public long getLength() {
            try {
                return asByteArray().length;
            } catch (IOException e) {
                return -1;
            }
        }
    }

    private static class ByteArraySObject extends SObject {
        private byte[] buf_;

        ByteArraySObject(String key, byte[] buf) {
            super(key);
            buf_ = buf;
        }
        
        @Override
        public byte[] asByteArray() throws IOException {
            return buf_;
        }

        @Override
        public File asFile() throws IOException {
            File tmpFile = SObject.createTempFile();
            IO.write(buf_, tmpFile);
            return tmpFile;
        }

        @Override
        public InputStream asInputStream() {
            return new ByteArrayInputStream(buf_);
        }

        @Override
        public String asString() throws IOException {
            return new String(buf_, "utf-8");
        }

        @Override
        public long getLength() {
            return buf_.length;
        }
    }
    
}
