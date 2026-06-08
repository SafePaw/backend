package com.ne7k.safepaw.auth.repository;

import com.ne7k.safepaw.auth.domain.SocialIdentity;
import com.ne7k.safepaw.auth.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialIdentityRepository extends JpaRepository<SocialIdentity, Long> {

    // 소셜 및 소셜 아이디로 가입한적이 있는지 체크
    Optional<SocialIdentity> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );
}
