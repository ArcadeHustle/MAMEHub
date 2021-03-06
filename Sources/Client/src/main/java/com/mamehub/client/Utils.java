package com.mamehub.client;

import java.awt.Desktop;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.net.RpcEngine;
import com.mamehub.client.utility.ClientDatabaseEngine;
import com.mamehub.thrift.OperatingSystem;
import com.mamehub.thrift.PlayerProfile;
import com.petebevin.markdown.MarkdownProcessor;

public class Utils {
  static final org.slf4j.Logger logger = LoggerFactory.getLogger(Utils.class);

  public static Set<Window> windows = new HashSet<Window>();
  private static ClientDatabaseEngine auditDatabaseEngine;
  private static ClientDatabaseEngine applicationDatabaseEngine;

  public static final int AUDIT_DATABASE_VERSION = 23;

  private static PlayerProfile playerProfile = null;
  private static Configuration configuration = null;

  public static PlayerProfile getPlayerProfile(RpcEngine rpcEngine) {
    if (playerProfile == null) {
      playerProfile = rpcEngine.getMyProfile();
    }
    return playerProfile;
  }

  public static void commitProfile(RpcEngine rpcEngine) {
    rpcEngine.updateProfile(playerProfile);
  }

  public static void openWebpage(URL url) {
    try {
      openWebpage(url.toURI());
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  public static void openWebpage(URI uri) {
    try {
      Desktop.getDesktop().browse(uri);
    } catch (UnsupportedOperationException e) {
      try {
        Runtime.getRuntime().exec("xdg-open " + uri.toString());
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String fileToString(File iniFile) throws IOException {
    StringBuilder sb = new StringBuilder((int) iniFile.length());

    String line;
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(iniFile));

      while (true) {
        line = reader.readLine();
        if (line == null) {
          break;
        }
        if (sb.length() > 0) {
          sb.append('\n');
        }
        sb.append(line);
      }

      return sb.toString();
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  public static synchronized void deleteAuditDatabaseEngine() {
    if (Utils.applicationDatabaseEngine != null) {
      Utils.applicationDatabaseEngine.close();
    }
    Utils.applicationDatabaseEngine = null;
    String dbDirectory = "./";// System.getProperty( "user.home" );
    try {
      FileUtils.deleteDirectory(new File(dbDirectory, "MAMEHubAuditDB"
          + AUDIT_DATABASE_VERSION));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static synchronized ClientDatabaseEngine getAuditDatabaseEngine() {
    String dbDirectory = "./";// System.getProperty( "user.home" );
    if (Utils.auditDatabaseEngine == null) {
      try {
        boolean inMemory = false;
        Utils.auditDatabaseEngine = new ClientDatabaseEngine(dbDirectory,
            "MAMEHubAuditDB" + AUDIT_DATABASE_VERSION, false, inMemory, true);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return Utils.auditDatabaseEngine;
  }

  public static void shutdownDatabaseEngine() {
    if (Utils.auditDatabaseEngine != null) {
      Utils.auditDatabaseEngine.close();
    }
    if (Utils.applicationDatabaseEngine != null) {
      Utils.applicationDatabaseEngine.close();
    }
    Utils.auditDatabaseEngine = null;
    Utils.applicationDatabaseEngine = null;
  }

  public static boolean isWindows() {

    String os = System.getProperty("os.name").toLowerCase();
    // windows
    return (os.indexOf("win") >= 0);

  }

  public static boolean isMac() {

    String os = System.getProperty("os.name").toLowerCase();
    // Mac
    return (os.indexOf("mac") >= 0);

  }

  public static boolean isUnix() {

    String os = System.getProperty("os.name").toLowerCase();
    // linux or unix
    return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);

  }

  @SuppressWarnings("rawtypes")
  public static void flushAllLogs() {
    try {
      Set<FileAppender> flushedFileAppenders = new HashSet<FileAppender>();
      Enumeration currentLoggers = LogManager.getLoggerRepository()
          .getCurrentLoggers();
      while (currentLoggers.hasMoreElements()) {
        Object nextLogger = currentLoggers.nextElement();
        if (nextLogger instanceof Logger) {
          Logger currentLogger = (Logger) nextLogger;
          Enumeration allAppenders = currentLogger.getAllAppenders();
          while (allAppenders.hasMoreElements()) {
            Object nextElement = allAppenders.nextElement();
            if (nextElement instanceof FileAppender) {
              FileAppender fileAppender = (FileAppender) nextElement;
              if (!flushedFileAppenders.contains(fileAppender)
                  && !fileAppender.getImmediateFlush()) {
                flushedFileAppenders.add(fileAppender);
                // log.info("Appender "+fileAppender.getName()+" is not doing immediateFlush ");
                fileAppender.setImmediateFlush(true);
                currentLogger.info("FLUSH");
                fileAppender.setImmediateFlush(false);
              } else {
                // log.info("fileAppender"+fileAppender.getName()+" is doing immediateFlush");
              }
            }
          }
        }
      }
    } catch (RuntimeException e) {
      logger.error("Failed flushing logs", e);
    }
  }

  public static void removeWindow(Window window) {
    windows.remove(window);
    if (windows.isEmpty()) {
      logger.info("No windows left, exiting");
      System.exit(0);
    }
  }

  public static String osToShortOS(OperatingSystem operatingSystem) {
    switch (operatingSystem) {
    case WINDOWS:
      return "WIN";
    case LINUX:
      return "LNX";
    case MAC:
      return "MAC";
    default:
      throw new RuntimeException("UNKNOWN OS");
    }
  }

  public static synchronized URL getResourceUrl(String suffix) {
    URL u = Utils.class.getResource(suffix);
    if (u == null) {
      throw new RuntimeException("Could not find resource: " + suffix);
    }
    return u;
  }

  public static synchronized InputStream getResourceInputStream(String suffix) {
    return Utils.class.getResourceAsStream(suffix);
  }

  public static synchronized BufferedReader getResourceReader(String location) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        Utils.getResourceInputStream(location)));
    return reader;
  }

  public static synchronized List<String> getResourcesWithPrefix(String prefix) {
    // Remove the leading slash on the prefix
    if (!prefix.startsWith("/")) {
      throw new RuntimeException("Prefix needs to start with /");
    }
    prefix = prefix.substring(1);

    try {
      URL url = Utils.class.getResource("Utils.class");
      String scheme = url.getProtocol();
      if (!"jar".equals(scheme))
        throw new IllegalArgumentException("Unsupported scheme: " + scheme);
      JarURLConnection con = (JarURLConnection) url.openConnection();
      JarFile archive = con.getJarFile();
      /* Search for the entries you care about. */
      Enumeration<JarEntry> entries = archive.entries();
      List<String> results = new ArrayList<>();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (entry.getName().startsWith(prefix)) {
          results.add("/" + entry.getName());
        }
      }
      return results;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public static <K, V> void stagedClear(Map<K, V> map,
      ClientDatabaseEngine databaseEngine) {
    Set<K> keys = new HashSet<K>();
    keys.addAll(map.keySet());
    int count = 0;
    for (K key : keys) {
      map.remove(key);
      count++;
      if (count % 5000 == 0) {
        databaseEngine.commit();
      }
    }
    databaseEngine.commit();
  }

  public static void dumpMemoryUsage() {
    System.out.println("MEMORY USAGE: "
        + (Runtime.getRuntime().freeMemory() / 1024 / 1024)
        + " / "
        + (Runtime.getRuntime().maxMemory() / 1024 / 1024)
        + "     ("
        + ((Runtime.getRuntime().freeMemory()) * 100.0 / Runtime
            .getRuntime().maxMemory()) + ")");
  }

  public static boolean windowIsInactive(MameHubEngine mameHubEngine) {
    return KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .getFocusedWindow() == null && !mameHubEngine.isGameRunning();
  }

  public static String injectLinks(String string) {
    // separete input by spaces ( URLs don't have spaces )
    String[] parts = string.split("\\s");
    String result = "";

    // Attempt to convert each item into an URL.
    for (String item : parts)
      try {
        URL url = new URL(item);
        // If possible then replace with anchor...
        result += "[" + url + "](" + url + ") ";
      } catch (MalformedURLException e) {
        // If there was an URL that was not it!...
        result += item + " ";
      }
    return result;
  }

  public static String markdownToHtml(String str) {
    // Escape html
    str = StringUtils.replaceEach(str, new String[] { "&", "\"", "<", ">" },
        new String[] { "&amp;", "&quot;", "&lt;", "&gt;" });
    str = injectLinks(str);
    MarkdownProcessor m = new MarkdownProcessor();
    str = m.markdown(str);
    return str;
  }

  public static void wipeAuditDatabaseEngine() throws IOException {
    Utils.auditDatabaseEngine.wipeDatabase();
    Utils.auditDatabaseEngine = null;
  }

  public static File getHashDirectory() {
    if (Utils.class.getResource("Utils.class").toString().contains("jar")) {
      return new File("../hash");
    } else {
      return new File("../../Binaries/hash");
    }
  }

  public static Configuration getConfiguration() {
    if (configuration == null) {
      try {
        CompositeConfiguration compositeConfig = new CompositeConfiguration();
        new File("mamehub.properties").createNewFile();
        PropertiesConfiguration userConfig = new PropertiesConfiguration(
            "mamehub.properties");
        userConfig.setAutoSave(true);
        compositeConfig.addConfiguration(userConfig, true);
        PropertiesConfiguration defaultConfig = new PropertiesConfiguration(
            Utils.getResourceUrl("/mamehubdefault.properties"));
        compositeConfig.addConfiguration(defaultConfig);
        configuration = compositeConfig;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return configuration;
  }

  public static String encodePassword(String password) {
    try {
      byte[] input = password.getBytes("UTF-8");

      // Compress the bytes
      byte[] output = new byte[65536];
      Deflater compresser = new Deflater();
      compresser.setInput(input);
      compresser.finish();
      int compressedDataLength = compresser.deflate(output, 0, output.length,
          Deflater.FULL_FLUSH);
      if (compressedDataLength == 65536) {
        throw new IOException("Encoded password is too long");
      }
      byte[] compressedPassword = Arrays.copyOf(output, compressedDataLength);
      return Base64.encodeBase64String(compressedPassword);
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  public static String decodePassword(String encodedPassword) {
    if (encodedPassword.isEmpty()) {
      return "";
    }

    try {
      byte[] deflatedBytes = Base64.decodeBase64(encodedPassword);

      // Decompress the bytes
      Inflater decompresser = new Inflater();
      decompresser.setInput(deflatedBytes);
      byte[] result = new byte[65536];
      int resultLength = decompresser.inflate(result);
      if (resultLength == 0) {
        throw new IOException("Could not decompress");
      }
      decompresser.end();

      // Decode the bytes into a String
      return new String(result, 0, resultLength, "UTF-8");
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

}
