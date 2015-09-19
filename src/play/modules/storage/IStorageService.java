package play.modules.storage;

import java.io.IOException;
import java.util.Map;


public interface IStorageService {

    void configure(Map<String, String> conf);
    
    /**
     * Retrieve the stuff from the storage by key
     * 
     * If file cannot be find by key, then <code>null</code> is returned
     * 
     * @param key
     * @return the file associated with key or null if not found
     */
    ISObject get(String key);
    /**
     * Update the stuff in the storage. If the existing file cannot be find
     * in the storage then it will be added.
     * 
     * @param key
     * @param stuff
     */
    void put(String key, ISObject stuff) throws IOException;
    /**
     * Remove the file from the storage by key and return it to caller.
     * 
     * if the file cannot be found, then <code>null</code> is returned
     * 
     * @param key
     * @return the stuff associated with key or null if not found
     */
    ISObject remove(String key);

    /**
     * Return the URL to access a stored resource by key 
     * 
     * @param key
     * @return the URL
     */
    String getUrl(String key);
}
