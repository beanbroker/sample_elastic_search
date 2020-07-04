package com.beanbroker.search;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserProfileDto {

  private String userId;
  private String name;
  private String email;

  public BeanbrokerUser toBeanbrokerUser() {

    return BeanbrokerUser.builder()
        .userId(userId)
        .name(name)
        .email(email)
        .createdAt(LocalDateTime.now())
        .build();
  }
}
