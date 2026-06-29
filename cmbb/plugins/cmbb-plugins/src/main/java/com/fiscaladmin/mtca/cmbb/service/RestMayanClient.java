package com.fiscaladmin.mtca.cmbb.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Mayan EDMS REST v4 client (CMBB-F07, P15). Token obtained at runtime from
 * MAYAN_URL / MAYAN_USER / MAYAN_PASSWORD (DEV: localhost:8880, autoadmin).
 * Upload: single-shot POST /documents/upload/ (multipart document_type_id +
 * file); falls back to two-step create + file when the endpoint is absent.
 */
public class RestMayanClient implements MayanClient {

    private final String base;
    private final String user;
    private final String password;
    private final String docTypeLabel;
    private String token;

    public RestMayanClient() {
        this(env("MAYAN_URL", "http://localhost:8880"),
             env("MAYAN_USER", "admin"), env("MAYAN_PASSWORD", "admin"),
             env("MAYAN_DOC_TYPE", "DM Case Document"));
    }

    public RestMayanClient(String baseUrl, String user, String password, String docTypeLabel) {
        this.base = baseUrl.replaceAll("/+$", "") + "/api/v4";
        this.user = user;
        this.password = password;
        this.docTypeLabel = docTypeLabel;
    }

    @Override
    public String upload(String label, String fileName, byte[] content) throws Exception {
        ensureToken();
        String typeId = documentTypeId();
        // ALWAYS two-step (create -> attach file): the single-shot
        // /documents/upload/ response carries OTHER object ids (the document
        // TYPE id leaked into the register during testing) — two-step is the
        // only deterministic way to learn the document id. Live-verified.
        String created = json("POST", "/documents/",
                "{\"document_type_id\":" + typeId + ",\"label\":\"" + esc(label) + "\"}");
        // the response nests document_type (its "id" comes FIRST in the JSON,
        // which poisoned the register with the TYPE id) — take the document id
        // from a documents/<id>/ URL instead
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("/documents/(\\d+)/").matcher(created);
        if (!m.find()) {
            throw new IllegalStateException("Mayan create failed: " + created);
        }
        String docId = m.group(1);
        multipartFileOnly(base + "/documents/" + docId + "/files/", fileName, content);
        return docId;
    }

    private void ensureToken() throws Exception {
        if (token != null) {
            return;
        }
        String resp = json("POST", "/auth/token/obtain/",
                "{\"username\":\"" + esc(user) + "\",\"password\":\"" + esc(password) + "\"}");
        token = DeadlineService.jsonStr(resp, "token");
        if (token.isEmpty()) {
            throw new IllegalStateException("Mayan token obtain failed: " + resp);
        }
    }

    private String documentTypeId() throws Exception {
        String resp = json("GET", "/document_types/?page_size=100", null);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\{\"id\"\\s*:\\s*(\\d+)[^}]*?\"label\"\\s*:\\s*\""
                        + java.util.regex.Pattern.quote(docTypeLabel) + "\"").matcher(resp);
        if (m.find()) {
            return m.group(1);
        }
        // tolerate field order differences
        m = java.util.regex.Pattern.compile("\"label\"\\s*:\\s*\""
                + java.util.regex.Pattern.quote(docTypeLabel)
                + "\"[^}]*?\"id\"\\s*:\\s*(\\d+)").matcher(resp);
        if (m.find()) {
            return m.group(1);
        }
        m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(resp);
        if (m.find()) {
            return m.group(1); // first available type as last resort (DEV)
        }
        throw new IllegalStateException("no Mayan document types: " + resp);
    }

    private String json(String method, String path, String body) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(base + path).openConnection();
        con.setRequestMethod(method);
        con.setRequestProperty("Accept", "application/json");
        if (token != null) {
            con.setRequestProperty("Authorization", "Token " + token);
        }
        if (body != null) {
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        return read(con);
    }

    private String multipart(String url, String typeId, String label,
                             String fileName, byte[] content) {
        try {
            String boundary = "----cmbb" + System.nanoTime();
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Token " + token);
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                writeField(os, boundary, "document_type_id", typeId);
                writeField(os, boundary, "label", label);
                writeFile(os, boundary, "file", fileName, content);
                os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }
            int code = con.getResponseCode();
            String resp = read(con);
            return code >= 200 && code < 300 ? resp : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void multipartFileOnly(String url, String fileName, byte[] content) throws Exception {
        String boundary = "----cmbb" + System.nanoTime();
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Token " + token);
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) {
            writeField(os, boundary, "action_name", "replace");
            writeFile(os, boundary, "file_new", fileName, content);
            os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }
        int code = con.getResponseCode();
        if (code >= 300) {
            throw new IllegalStateException("Mayan file upload failed: " + code + " " + read(con));
        }
    }

    private static void writeField(OutputStream os, String boundary,
                                   String name, String value) throws Exception {
        os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name
                + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFile(OutputStream os, String boundary, String field,
                                  String fileName, byte[] content) throws Exception {
        os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field
                + "\"; filename=\"" + fileName + "\"\r\nContent-Type: application/octet-stream"
                + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(content);
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String read(HttpURLConnection con) throws Exception {
        InputStream is = con.getResponseCode() < 400
                ? con.getInputStream() : con.getErrorStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (is != null) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    static String env(String key, String dflt) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? dflt : v;
    }

    static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
