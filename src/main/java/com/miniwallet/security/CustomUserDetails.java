package com.miniwallet.security;

import com.miniwallet.entity.User;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

  private final User user;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(
        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
    );
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getUserName();
  }

  public long getId() {
    return user.getId();
  }

  public String getRole() {
    return user.getRole().name();
  }
}
