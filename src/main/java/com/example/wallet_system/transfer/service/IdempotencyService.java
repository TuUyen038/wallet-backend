package com.example.wallet_system.transfer.service;

import com.example.wallet_system.common.exception.AppException;
import com.example.wallet_system.transfer.dto.TransferResponse;
import com.example.wallet_system.transfer.entity.IdempotencyKey;
import com.example.wallet_system.transfer.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

  private final IdempotencyKeyRepository idempotencyKeyRepository;
  private final ObjectMapper objectMapper;

  /**
   * Cố gắng claim key bằng INSERT.
   * - Nếu INSERT thành công → key chưa tồn tại, caller được phép xử lý
   * - Nếu duplicate key exception → key đã tồn tại
   * + status=DONE → trả về response snapshot
   * + status=PROCESSING → request đang được xử lý (concurrent), trả 409
   *
   * Dùng REQUIRES_NEW để transaction này độc lập — commit ngay lập tức,
   * không bị rollback theo transaction cha nếu transfer thất bại.
   */
  // IdempotencyService.java
  public enum ClaimResult {
    CLAIMED, // key mới, được phép xử lý
    DUPLICATE, // key đã DONE, có cached response
    IN_PROGRESS // key đang PROCESSING
  }

  @Getter
  public static class ClaimOutcome {
    private final ClaimResult result;
    private final TransferResponse cachedResponse;

    public ClaimOutcome(ClaimResult result, TransferResponse cachedResponse) {
      this.result = result;
      this.cachedResponse = cachedResponse;
    }

    public static ClaimOutcome claimed() {
      return new ClaimOutcome(ClaimResult.CLAIMED, null);
    }

    public static ClaimOutcome duplicate(TransferResponse cached) {
      return new ClaimOutcome(ClaimResult.DUPLICATE, cached);
    }

    public static ClaimOutcome inProgress() {
      return new ClaimOutcome(ClaimResult.IN_PROGRESS, null);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ClaimOutcome claim(String key) {
    try {
      IdempotencyKey entity = IdempotencyKey.builder()
          .key(key)
          .status("PROCESSING")
          .build();
      idempotencyKeyRepository.saveAndFlush(entity);
      return ClaimOutcome.claimed();

    } catch (DataIntegrityViolationException ex) {
      IdempotencyKey existing = idempotencyKeyRepository.findById(key)
          .orElseThrow(() -> ex);

      if ("DONE".equals(existing.getStatus())) {
        return ClaimOutcome.duplicate(deserialize(existing.getResponse()));
      }

      // PROCESSING
      if (existing.getUpdatedAt().isBefore(Instant.now().minusSeconds(300))) {
        log.warn("Key {} bị treo, đang reset...", key);

        // Chỉ cần update status, Trigger ở Postgres sẽ tự nhảy updated_at lên NOW()
        existing.setStatus("PROCESSING");
        idempotencyKeyRepository.saveAndFlush(existing);

        return ClaimOutcome.claimed();
      }

      return ClaimOutcome.inProgress();
    }
  }

  /**
   * Sau khi transfer thành công, lưu response snapshot vào idempotency_keys.
   * Dùng REQUIRES_NEW để commit độc lập.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void complete(String key, TransferResponse response) {
    log.debug("Completing idempotency key={}", key); // thêm dòng này
    idempotencyKeyRepository.findById(key).ifPresent(entity -> {
      entity.setStatus("DONE");
      entity.setResponse(serialize(response));
      idempotencyKeyRepository.save(entity);
      log.debug("Idempotency key={} marked DONE", key); // thêm dòng này
    });
  }

  private String serialize(TransferResponse response) {
    try {
      return objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize response", e);
    }
  }

  private TransferResponse deserialize(String json) {
    try {
      return objectMapper.readValue(json, TransferResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to deserialize response", e);
    }
  }
}