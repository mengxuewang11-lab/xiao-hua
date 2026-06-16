package com.xiaohua.novel.shared.api;

public final class ApiPaths {

    public static final String API = "/api";
    public static final String ADMIN_API = "/admin-api";

    public static final String API_ALL = API + "/**";
    public static final String ADMIN_API_ALL = ADMIN_API + "/**";

    public static final String NOVEL_AGENT = API + "/novel-agent";
    public static final String CHAT = API + "/chat";
    public static final String TEST = API + "/test";
    public static final String TEST_PING = TEST + "/ping";

    private ApiPaths() {
    }
}
