/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */
package de.mage.component.journal.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SnowflakeService {

  private static final long UNUSED_BITS = 1L;
  private static final long TIMESTAMP_BITS = 41L;
  private static final long TENANT_ID_BITS = 5L;
  private static final long SERVICE_ID_BITS = 5L;
  private static final long SEQUENCE_BITS = 12L;

  private static final long MAX_TENANT_ID = ~(-1L << TENANT_ID_BITS);
  private static final long MAX_SERVICE_ID = ~(-1L << SERVICE_ID_BITS);
  private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

  private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + TENANT_ID_BITS + SERVICE_ID_BITS;
  private static final long TENANT_ID_SHIFT = SEQUENCE_BITS + SERVICE_ID_BITS;
  private static final long SERVICE_ID_SHIFT = SEQUENCE_BITS;

  private final long INCORPORATION_DATE = 1569888000000L;

  private long lastTimestamp = -1L;
  private long sequenceCounter = 0L;

  private final AtomicLong waitCount = new AtomicLong(0);

  @Value("${de.mage.sequence.tenant:test}")
  private String tenant;
  @Value("${de.mage.sequence.service:journal}")
  private String service;

  private MessageDigest messageDigest;

  public SnowflakeService() {
    super();
  }

  public synchronized long next() {
    long currentTimestamp = this.getCurrentTimestamp();

    if (currentTimestamp < this.lastTimestamp) {
      throw new IllegalStateException(
          String.format("Clock moved backwards. Refusing to generate id for %d milliseconds",
              this.lastTimestamp - currentTimestamp));
    }

    if (currentTimestamp == this.lastTimestamp) {
      this.sequenceCounter = (this.sequenceCounter + 1) & MAX_SEQUENCE;
      if (this.sequenceCounter == 0) {
        currentTimestamp = this.awaitNextCycle(currentTimestamp);
      }
    } else {
      this.sequenceCounter = 0L;
    }

    this.lastTimestamp = currentTimestamp;

    return ((currentTimestamp - INCORPORATION_DATE) << TIMESTAMP_SHIFT)
        | (this.getTenant() << TENANT_ID_SHIFT)
        | (this.getService() << SERVICE_ID_SHIFT)
        | this.sequenceCounter;
  }

  public String parseAndFormat(final long sequence) {
    final long[] arr = this.parse(sequence);
    final String tmf =
        LocalDateTime
            .ofInstant(
                Instant.ofEpochMilli(arr[0]), ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_DATE_TIME);

    return String.format("%s, #%d, @(%d,%d)", tmf, arr[3], arr[1], arr[2]);
  }

  private long getTenant() {
    return this.tenant.hashCode() & MAX_TENANT_ID;
  }

  private long getService() {
    return this.service.hashCode() & MAX_SERVICE_ID;
  }

  private long getCurrentTimestamp() {
    return Instant.now(Clock.systemUTC()).toEpochMilli();
  }

  private long awaitNextCycle(long currentTimestamp) {
    this.waitCount.incrementAndGet();
    while (currentTimestamp <= this.lastTimestamp) {
      currentTimestamp = this.getCurrentTimestamp();
    }
    return currentTimestamp;
  }

  private long diode(long offset, long length) {
    int lb = (int) (64 - offset);
    int rb = (int) (64 - (offset + length));
    return (-1L << lb) ^ (-1L << rb);
  }

  private long[] parse(long sequence) {
    long[] arr = new long[5];
    arr[4] = ((sequence & diode(UNUSED_BITS, TIMESTAMP_BITS)) >> TIMESTAMP_SHIFT);
    arr[0] = arr[4] + INCORPORATION_DATE;
    arr[1] = (sequence & diode(UNUSED_BITS + TIMESTAMP_BITS, TENANT_ID_BITS)) >> TENANT_ID_SHIFT;
    arr[2] = (sequence & diode(UNUSED_BITS + TIMESTAMP_BITS + TENANT_ID_BITS, SERVICE_ID_BITS)) >> SERVICE_ID_SHIFT;
    arr[3] = (sequence & diode(UNUSED_BITS + TIMESTAMP_BITS + TENANT_ID_BITS + SERVICE_ID_BITS, SEQUENCE_BITS));
    return arr;
  }

  private MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
    if (this.messageDigest == null) {
      this.messageDigest = MessageDigest.getInstance("MD5");
    }
    return this.messageDigest;
  }
}
