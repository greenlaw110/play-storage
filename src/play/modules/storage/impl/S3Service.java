package play.modules.storage.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import play.exceptions.ConfigurationException;
import play.libs.Codec;
import play.libs.Crypto;
import play.modules.storage.ISObject;
import play.modules.storage.IStorageService;

import java.io.IOException;
import java.util.Map;

/**
 * Implement {@link IStorageService} on Amazon S3
 */
public class S3Service implements IStorageService {

    public static enum StorageClass {
        STANDARD, REDUCED_REDUNDANCY;

        public static StorageClass valueOfIgnoreCase(String s) {
            s = s.trim();
            if ("rrs".equalsIgnoreCase(s) || "rr".equalsIgnoreCase(s) || "reduced_redundancy".equalsIgnoreCase(s) || "reducedRedundancy".equalsIgnoreCase(s)) {
                return REDUCED_REDUNDANCY;
            } else {
                return STANDARD;
            }
        }
    }

    public static final String S3_KEY_ID = "storage.s3.keyId";
    public static final String S3_KEY_SECRET = "storage.s3.keySecret";
    public static final String S3_DEF_STORAGE_CLASS = "storage.s3.defStorageClass";
    public static final String S3_BUCKET = "storage.s3.bucket";
    public static final String S3_STATIC_WEB_ENDPOINT = "storage.s3.staticWebEndpoint";

    public static final String ATTR_STORAGE_CLASS = "storage-class";


    private String awsKeyId;
    private String awsKeySecret;
    StorageClass defStorageClass = StorageClass.REDUCED_REDUNDANCY;
    String bucket;
    String staticWebEndPoint = null;
    
    public static AmazonS3 s3;

    @Override
    public void configure(Map<String, String> conf) { 
        awsKeyId = conf.get(S3_KEY_ID);
        awsKeySecret = conf.get(S3_KEY_SECRET);
        if (null == awsKeySecret || null == awsKeyId) {
            throw new ConfigurationException("AWS Key ID or AWS Key Secret not found in the configuration");
        }
        String sc = conf.get(S3_DEF_STORAGE_CLASS);
        if (null != sc) {
            defStorageClass = StorageClass.valueOfIgnoreCase(sc);
        }
        bucket = conf.get(S3_BUCKET);
        if (null == bucket) {
            throw new ConfigurationException("AWS bucket not found in the configuration");
        }

        staticWebEndPoint = conf.get(S3_STATIC_WEB_ENDPOINT);
        System.setProperty("line.separator", "\n");
        AWSCredentials cred = new BasicAWSCredentials(awsKeyId, awsKeySecret);
        s3 = new AmazonS3Client(cred);
    }

    String authStr(String stringToSign) {
        byte[] key = awsKeySecret.getBytes();
        String ss = Crypto.sign(stringToSign, key);
        ss = Codec.encodeBASE64(ss);
        return "AWS " + awsKeyId + ":" + ss;
    }

    @Override
    public ISObject get(String key) {
        //S3Object obj = s3.getObject(new GetObjectRequest(bucket, key));
        return SObject.getDumpObject(key);
//        S3Action act = S3Action.get(key, this);
//        WS.HttpResponse resp = act.doIt();
//        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void put(String key, ISObject stuff) throws IOException {
        ObjectMetadata meta = new ObjectMetadata();
        meta.setUserMetadata(stuff.getAttributes()); 
        
        PutObjectRequest req = new PutObjectRequest(bucket, key, stuff.asInputStream(), meta);
        req.withCannedAcl(CannedAccessControlList.PublicRead);
        s3.putObject(req);
//        S3Action act = S3Action.put(stuff, key, this);
//        WS.HttpResponse resp = act.doIt();
    }

    @Override
    public ISObject remove(String key) {
        s3.deleteObject(new DeleteObjectRequest(bucket, key));
//        S3Action act = S3Action.delete(key, this);
//        WS.HttpResponse resp = act.doIt();
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getUrl(String key) {
        if (null == staticWebEndPoint) {
            return null;
        }
        return "//" + staticWebEndPoint + "/" + key;
    }
}
