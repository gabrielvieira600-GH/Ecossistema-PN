package br.com.feiras;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Maintenance {
  private final Commerce service;

  public Maintenance(Commerce service) {
    this.service = service;
  }

  @Scheduled(fixedDelay = 60000, initialDelay = 60000)
  public void expire() {
    service.sweep();
  }
}
