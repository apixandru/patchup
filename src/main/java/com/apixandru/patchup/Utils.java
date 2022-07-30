package com.apixandru.patchup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

final class Utils {

    static String getFileChecksum(MessageDigest digest, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }

        byte[] bytes = digest.digest();

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    static String getFileChecksum(String digest, File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md5Digest = MessageDigest.getInstance(digest);
        return getFileChecksum(md5Digest, file);
    }

    static File getEntrypointFile(String entrypoint) throws URISyntaxException {
        URL resourceAsStream = QuickReturnTransformer.class.getResource(entrypoint);
        if (resourceAsStream == null) {
            throw new IllegalArgumentException("Could not find the entrypoint");
        }
        String schemeSpecificPart = resourceAsStream.toURI()
                .getSchemeSpecificPart();
        String jar = schemeSpecificPart.split(".jar!/")[0] + ".jar";
        if (!jar.startsWith("file:/")) {
            throw new IllegalArgumentException("Expecting url to start with file:/");
        }
        jar = jar.substring(5);
        return new File(jar);
    }

    static String getMandatory(Map<String, String> props, String key) {
        String value = props.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing config " + key);
        }
        return value;
    }

    @SuppressWarnings({"rawtypes", "raw-types", "unchecked"})
    static HashMap<String, String> loadProperties(String args) throws IOException {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("Missing argument.");
        }
        File configFile = new File(args);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("Missing config file " + configFile);
        }

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        }
        return new HashMap(properties);
    }

    static String normalize(String string) {
        return string.replace(".", "/");
    }

}
