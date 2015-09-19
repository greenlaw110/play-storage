package play.modules.storage;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;

import java.io.File;
import java.util.Calendar;
import java.util.Map;

public class StoragePlugin extends PlayPlugin {

    public static IStorageService service = null;

    @Override
    public void onApplicationStart() {
        try {
            String className = Play.configuration.getProperty("storage.serviceImpl");
            Class clazz = Class.forName(className);
            service = (IStorageService) clazz.newInstance();
            service.configure((Map)Play.configuration);
        } catch (Exception e) {
            Logger.error(e, "error init StoragePlugin");
            throw new UnexpectedException(e);
        }

        Logger.trace("gallery module initialized");
    }

    private static Object lock_ = new Object();

    public static String newKey() {
        return newKey(null, Structure.BY_DATE);
    }

    public static String newKey(Structure structure) {
        return newKey(null, structure);
    }

    public static String newKey(String name) {
        return newKey(name, Structure.BY_DATE);
    }

    /**
     * try to generate unique key (usually, for naming new files to be
     * put into the storage)
     *
     * @param name      - the proposed name of the file. optional, if it is null
     *                  then a random name will be generated
     * @param structure - the storage structure
     * @return the key
     */
    public static String newKey(String name, Structure structure) {
        if (StringUtils.isBlank(name)) {
            synchronized (lock_) {
                long l = System.currentTimeMillis();
                String prefix = RandomStringUtils.randomAlphabetic(5);
                name = prefix + String.valueOf(l);
            }
        }

        if (null == structure) structure = Structure.BY_DATE;
        switch (structure) {
            case BY_DATETIME:
                return String.format("%2$tY%1$s%2$tm%1$s%2$td%1$s%2$tH%1$s%2$tM%1$s%2$tS%1$s%3$s", File.separator, Calendar.getInstance(), name);
            case BY_DATE:
                return String.format("%2$tY%1$s%2$tm%1$s%2$td%1$s%3$s", File.separator, Calendar.getInstance(), name);
            case PLAIN:
                return name;
            default:
                throw new RuntimeException("oops, how can I get here?");
        }
    }

    /**
     * Define the storage structure. There are 2 possible structure
     * 1. PLAIN, all files are saved directly in one folder
     * 2. BY_DATE, files are saved in a hierarchical structure like yyyy/mm/dd
     * 3. BY_DATETIME, files are saved in a hierarchical structure like yyyy/mm/dd/HH/MM/SS
     *
     * @author greenl
     */
    public static enum Structure {
        PLAIN, BY_DATE, BY_DATETIME
    }

    public static void main(String[] args) {
        System.out.println(newKey());
        System.out.println(newKey("Hello.world"));
        System.out.println(newKey("Hello.world", Structure.BY_DATETIME));
        System.out.println(newKey(Structure.PLAIN));
    }

}
