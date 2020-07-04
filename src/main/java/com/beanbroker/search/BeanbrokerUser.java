package com.beanbroker.search;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BeanbrokerUser {

  private String uniqueId;
  private String userId;
  private String name;
  private String email;
  private LocalDateTime createdAt;
}
