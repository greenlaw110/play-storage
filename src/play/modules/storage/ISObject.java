package play.modules.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Represent an item stored in an <code>IStorageService</code> 
 * 
 * @author greenl
 */
public interface ISObject {

    /**
     * A standard attribute: content-type
     */
    public static final String ATTR_CONTENT_TYPE = "content-type";

    /**
     * A standard attribute: filename
     */
    public static final String ATTR_FILE_NAME = "filename";

    /**
     * @return key of this object
     */
    String getKey();

    /**
     * @return length of the object
     */
    long getLength();
    
    /** 
     * Return attribute associated with this storage object by key. If there is 
     * no such attribute found then <code>null</code> is returned
     * 
     * @return the attribute if found or <code>null</code> if not found
     */
    String getAttribute(String key);

    /**
     * Set an attribute to the storage object associated by key specified. 
     * @param key
     * @param val
     */
    void setAttribute(String key, String val);

    /**
     * @return <code>true</code> if the storage object has attributes
     */
    boolean hasAttribute();

    /**
     * @return a copy of attributes of this storage object
     */
    Map<String, String> getAttributes();

    /**
     * Save the object along with it's attributes
     */
    void save() throws IOException;
    
   /**
    * @return the the stuff content as an file
    */
   File asFile() throws IOException;
   /**
    * @return the stuff content as a string
    */
   String asString() throws IOException;
   /**
    * @return the stuff content as a byte array
    */
   byte[] asByteArray() throws IOException;
   /**
    * @return the stuff content as an input stream
    */
   InputStream asInputStream() throws IOException;

    /**
     * @return the external URL to access this ISObject. It requires the 
     * underline storage service supports the external URL. 
     */
   String getUrl();

}
