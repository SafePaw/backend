package com.ne7k.safepaw.global.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String role;

    // 권한 목록
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("Role_" + role)); // ex Role_User
    }

    // jwt라서 비밀번호는 비어둠
    @Override
    public String getPassword() {
        return "";
    }

    // 사용자 식별용
    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    // 계정 만료 아니면 true
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // 계정 잠금
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // 비밀번호 만료
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 활성 계정 체크
    @Override
    public boolean isEnabled() {
        return true;
    }
}
