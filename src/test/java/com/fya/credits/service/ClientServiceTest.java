package com.fya.credits.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fya.credits.model.Client;
import com.fya.credits.repository.ClientRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {
  @Mock ClientRepository clientRepository;

  @Test
  void createsNewClientOnFirstUpsert() {
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T20:00:00Z"), ZoneOffset.UTC);
    ClientService service = new ClientService(clientRepository, clock);
    when(clientRepository.findByDocumentNormalized("100000001")).thenReturn(Optional.empty());

    service.upsert("100000001", "Pepito", "", "Perez", "");

    ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
    verify(clientRepository).save(captor.capture());
    assertThat(captor.getValue().getDocument()).isEqualTo("100000001");
    assertThat(captor.getValue().getDocumentNormalized()).isEqualTo("100000001");
    assertThat(captor.getValue().getFullName()).isEqualTo("Pepito Perez");
    assertThat(captor.getValue().getCreatedAt()).isEqualTo(clock.instant());
    assertThat(captor.getValue().getUpdatedAt()).isEqualTo(clock.instant());
  }

  @Test
  void updatesExistingClientKeepingOriginalCreatedAt() {
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T20:00:00Z"), ZoneOffset.UTC);
    ClientService service = new ClientService(clientRepository, clock);
    Client existing = new Client();
    existing.setDocument("100000001");
    existing.setDocumentNormalized("100000001");
    existing.setFullName("Pepito Perez");
    existing.setCreatedAt(Instant.parse("2026-08-01T10:00:00Z"));
    when(clientRepository.findByDocumentNormalized("100000001")).thenReturn(Optional.of(existing));

    service.upsert("100000001", "Pepito", "", "Perez Gomez", "");

    ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
    verify(clientRepository).save(captor.capture());
    assertThat(captor.getValue().getFullName()).isEqualTo("Pepito Perez Gomez");
    assertThat(captor.getValue().getCreatedAt()).isEqualTo(Instant.parse("2026-08-01T10:00:00Z"));
    assertThat(captor.getValue().getUpdatedAt()).isEqualTo(clock.instant());
  }

  @Test
  void neverThrowsWhenRepositoryFails() {
    ClientService service = new ClientService(clientRepository, Clock.systemUTC());
    when(clientRepository.findByDocumentNormalized(any())).thenThrow(new RuntimeException("boom"));

    service.upsert("100000001", "Pepito", "", "Perez", "");
  }
}
