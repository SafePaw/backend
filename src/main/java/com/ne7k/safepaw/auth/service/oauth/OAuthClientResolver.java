package com.ne7k.safepaw.auth.service.oauth;

import com.ne7k.safepaw.auth.domain.SocialProvider;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// OAuth 클라이언트 지정
@Component
public class OAuthClientResolver {

    private final Map<SocialProvider, OAuthClient> clients;

    public OAuthClientResolver(List<OAuthClient> clients) {
        Map<SocialProvider, OAuthClient> map = new EnumMap<>(SocialProvider.class);
        for (OAuthClient client : clients) {
            map.put(client.support(), client);
        }
        this.clients = Map.copyOf(map);
    }

    public OAuthClient resolve(SocialProvider provider) {
        OAuthClient client = clients.get(provider);
        if (client == null) {
            throw new BusinessException(ErrorCode.AUTH_UNSUPPORTED_PROVIDER, "지원하지 않는 provider : " + provider);
        }
        return client;
    }
}
