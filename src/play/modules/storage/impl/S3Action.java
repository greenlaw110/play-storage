package play.modules.storage.impl;

import play.libs.F;
import play.libs.WS;
import play.modules.storage.ISObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The base class for S3 actions
 */
public abstract class S3Action {
    private static String getName(ISObject sobj) {
        return sobj.getAttribute(ISObject.ATTR_FILE_NAME);
    }
    private static String getContentType(ISObject sobj) {
        return sobj.getAttribute(ISObject.ATTR_CONTENT_TYPE);
    }
    private static class Header extends F.T2<String, String> implements Comparable<Header> {
        Header(String key, String val) {
            super(key, val);
        }
        
        String key() {
            return _1;
        }
        
        String val() {  
            return _2;
        }
        
        Header canonical() {
            return valueOf(key().toLowerCase(), val());
        }

        Header merge(Header header) {
            if (header.key().equalsIgnoreCase(key())) {
                String val = val();
                String val2 = header.val();
                if (!val.contains(val2)) {
                    val = val + "," + val2;
                    return valueOf(key(), val);
                }
            }
            return this;
        }

        @Override
        public String toString() {
            return key() + ":" + val();
        }

        @Override
        public int compareTo(Header o) {
            return key().toLowerCase().compareTo(o.key().toLowerCase());
        }

        static Header valueOf(String key, String val) {
            return new Header(key.trim(), val.trim());
        }

        static List<Header> valueOf(Map<String, String> map) {
            List<Header> l = new ArrayList<Header>();
            for (String k : map.keySet()) {
                l.add(valueOf(k, map.get(k)));
            }
            return l;
        }
        
        static Header findByKey(String key, List<Header> headers) {
            for (Header h : headers) {
                if (h.key().toLowerCase().equalsIgnoreCase(key.toLowerCase())) {
                    return h;
                }
            }
            return null;
        }

        static List<Header> canonical(List<Header> headers) {
            List<Header> l = new ArrayList<Header>();
            for (Header h : headers) {
                String k = h.key().toLowerCase();
                if (!k.startsWith("x-amz") && !k.equals("host")) {
                    continue;
                }
                Header found = findByKey(h.key(), l);
                if (null == found) {
                    l.add(h.canonical());
                } else {
                    l.remove(found);
                    l.add(found.merge(h));
                }
            }
            Collections.sort(l);
            return l;
        }
    }
    
    private S3Action(String key, S3Service s3) {
        this.s3 = s3;
        this.key = key;
        this.scls = s3.defStorageClass;
    } 
    
    private String key;
    protected Map<String, String> headers = new HashMap<String, String>();
    protected S3Service.StorageClass scls;
    
    protected abstract String getHttpVerb();
    
    private S3Service s3;

    private Date date = new Date();
    
    private SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
    
    protected String getContentMD5() {
        return "";
    };
    
    protected String getContentType() {
        return "";
    }
    
    protected final String getDate() {
        return sdf.format(date);
    }
    
    protected final String getCanonicalizedAmzHeaders() {
        if (headers.isEmpty()) return "";
        List<Header> hl = Header.canonical(Header.valueOf(headers));
        StringBuilder sb = new StringBuilder();
        int len = hl.size();
        for (int i = 0; i < len; ++i) {
            sb.append(hl.get(i).toString()).append("\n");
        }
        return sb.toString();
    }
    
    protected final String getCanonicalizedResource() {
        StringBuilder sb = new StringBuilder("/");
        sb.append(s3.bucket).append("/").append(key);
        return sb.toString();
    }
    
    private String getStringToSign() {
        StringBuilder sb = new StringBuilder();
        sb.append(getHttpVerb()).append('\n')
            .append("/\n")
            .append(getContentMD5()).append('\n')
            .append(getContentType()).append('\n')
            //.append(getDate()).append('\n')
            .append(getCanonicalizedAmzHeaders())
            .append(getCanonicalizedResource());
        return sb.toString();
    }
    
    protected void prepareHeaders() {
    }
    
    public final WS.HttpResponse doIt() {
        prepareHeaders();
        String date = getDate();
        headers.put("Date", date);
        headers.put("X-Amz-Date", date);
        String sts = getStringToSign();
        String auth = s3.authStr(sts);
        headers.put("Authorization", auth);

        String url = String.format("http://%s.s3.amazonaws.com/%s", s3.bucket, key);
        WS.WSRequest req = WS.url(url);
        req.headers(headers);
        return submit(req);
    }

    protected abstract WS.HttpResponse submit(WS.WSRequest request);

    public static Get get(String key, S3Service s3) {
        return new Get(key, s3);
    }

    public static Put put(ISObject sobj, String key, S3Service s3) {
        return new Put(sobj, key, s3);
    }

    public static Delete delete(String key, S3Service s3) {
        return new Delete(key, s3);
    }
    
    private static class Get extends S3Action {
    
        public Get(String key, S3Service s3) {
            super(key, s3);
        }
    
        @Override
        protected String getHttpVerb() {
            return "GET";
        }

        @Override
        protected WS.HttpResponse submit(WS.WSRequest request) {
            return request.get();
        }
    }
    
    
    private static class Put extends S3Action {
        private ISObject sobj;
        private String contentType;
        
        public Put(ISObject sobj, String key, S3Service s3) {
            super(key, s3);
            this.sobj = sobj;
            this.contentType = sobj.getAttribute(ISObject.ATTR_CONTENT_TYPE);
            String s = sobj.getAttribute(S3Service.ATTR_STORAGE_CLASS);
            if (null != s) {
                scls = S3Service.StorageClass.valueOfIgnoreCase(s);
            }
        }
    
        @Override
        protected String getHttpVerb() {
            return "PUT";
        }

        @Override
        protected String getContentType() {
            return contentType;
        }

        @Override
        protected void prepareHeaders() {
            Map<String, String> attrs = sobj.getAttributes();
            for (String k : attrs.keySet()) {
                if (ISObject.ATTR_CONTENT_TYPE.equalsIgnoreCase(k)) {
                    continue;
                }
                if (S3Service.ATTR_STORAGE_CLASS.equalsIgnoreCase(k)) {
                    continue;
                }
                headers.put("x-amz-meta-" + k, attrs.get(k));
            }
            headers.put("x-amz-storage-class", scls.toString());
        }

        @Override
        protected WS.HttpResponse submit(WS.WSRequest request) {
            try {
                request.body = sobj.asInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return request.put();
        }
    }
    

    private static class Delete extends S3Action {
    
        public Delete(String key, S3Service s3) {
            super(key, s3);
        }
    
        @Override
        protected String getHttpVerb() {
            return "DELETE";
        }

        @Override
        protected WS.HttpResponse submit(WS.WSRequest request) {
            return request.delete();
        }
    }
    
    
    
}
