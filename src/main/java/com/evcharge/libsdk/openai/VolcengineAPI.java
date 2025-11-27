package com.evcharge.libsdk.openai;

import lombok.NonNull;
import okhttp3.OkHttpClient;

/**
 * 火山方舟 API
 */
public class VolcengineAPI extends OpenAI {

    public VolcengineAPI(@NonNull String api_key) {
        super("https://ark.cn-beijing.volces.com/api/v3", api_key);
    }

    public VolcengineAPI(@NonNull String base_url, @NonNull String api_key) {
        super(base_url, api_key);
    }

    public VolcengineAPI(@NonNull String api_key, @NonNull OkHttpClient client) {
        super("https://ark.cn-beijing.volces.com/api/v3", api_key, client);
    }

    public VolcengineAPI(@NonNull String base_url, @NonNull String api_key, @NonNull OkHttpClient client) {
        super(base_url, api_key, client);
    }
}
