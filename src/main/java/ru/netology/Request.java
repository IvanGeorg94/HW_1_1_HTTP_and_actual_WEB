package ru.netology;

import java.util.*;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final String body;
    private final Map<String, List<String>> queryParams;

    public Request(String method, String path, Map<String, String> headers, String body,
                   Map<String, List<String>> queryParams) {
        this.method = method;
        this.path = path;
        this.headers = headers != null ? headers : new HashMap<>();
        this.body = body;
        this.queryParams = queryParams != null ? queryParams : new HashMap<>();
    }

    public String getMethod() { return method; }
    public String getPath() { return path; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }
    public Map<String, List<String>> getQueryParams() { return queryParams; }

    public String getQueryParam(String name) {
        List<String> values = queryParams.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }
}