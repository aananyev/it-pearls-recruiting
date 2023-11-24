package com.company.itpearls.core;

import com.google.gson.Gson;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import org.springframework.stereotype.Service;

@Service(VkService.NAME)
public class VkServiceBean implements VkService {
    public void initVkAPI() {
    }
}