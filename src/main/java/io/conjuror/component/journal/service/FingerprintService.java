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
package io.conjuror.component.journal.service;

import io.conjuror.component.journal.data.Journal;
import io.conjuror.component.journal.data.JournalItem.Allocation;
import io.conjuror.component.journal.exception.ResourceNotFoundException;
import io.conjuror.component.journal.repository.JournalItemRepository;
import io.conjuror.component.journal.repository.JournalRepository;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FingerprintService {

  private final JournalRepository journalRepository;
  private final JournalItemRepository journalItemRepository;

  @Autowired
  public FingerprintService(
      final JournalRepository journalRepository,
      final JournalItemRepository journalItemRepository) {
    super();
    this.journalRepository = journalRepository;
    this.journalItemRepository = journalItemRepository;
  }

  public String generate(final Long sequence) throws Exception {
    final Optional<Journal> optionalJournal = this.journalRepository.findById(sequence);
    if (optionalJournal.isEmpty()) {
      throw new ResourceNotFoundException(String.format("Journal '%s' not found.", sequence));
    }
    final Journal journal = optionalJournal.get();
    final String data = this.buildData(journal);

    return this.toHex(
        MessageDigest.getInstance("SHA3-256")
            .digest(data.getBytes(StandardCharsets.UTF_8))
    );
  }

  public boolean valid(final String fingerprint, final Long sequence) {
    if (fingerprint == null || fingerprint.isBlank() || sequence == null) {
      return Boolean.FALSE;
    }

    try {
      final String generatedFingerprint = this.generate(sequence);
      return fingerprint.equals(generatedFingerprint);
    } catch (final Exception ex) {
      return Boolean.FALSE;
    }
  }

  String buildData(final Journal journal) {
    final String[] dataContainer = new String[9];
    dataContainer[0] = journal.getSequence().toString();
    dataContainer[1] = journal.getIdentifier();
    dataContainer[2] = journal.getCurrencyCode();
    dataContainer[3] = journal.getDescription();
    dataContainer[4] = DateTimeFormatter.ISO_DATE.format(journal.getValueDate());
    dataContainer[5] = DateTimeFormatter.ISO_DATE.format(journal.getBookingDate());
    dataContainer[6] = DateTimeFormatter.ISO_DATE_TIME.format(journal.getCreatedAt());
    dataContainer[7] = journal.getCreatedBy();

    dataContainer[8] =
        this.journalItemRepository.findAllByJournalSequenceOrderBySequence(journal.getSequence())
          .stream()
          .map(journalItem ->
            String.join(
                "~",
                journalItem.getJournalSequence().toString(),
                journalItem.getSequence().toString(),
                journalItem.getIdentifier(),
                journalItem.getSource().asText(),
                journalItem.getTargets()
                    .stream()
                    .map(Allocation::asText).collect(Collectors.joining(";")),
                journalItem.getPurpose()
            )
          )
          .collect(Collectors.joining("$"));
    final BigInteger checksum =
        BigInteger
            .valueOf(
                Objects.hash(
                    dataContainer[0],
                    dataContainer[1],
                    dataContainer[2],
                    dataContainer[3],
                    dataContainer[4],
                    dataContainer[5],
                    dataContainer[6],
                    dataContainer[7],
                    dataContainer[8]
                )
            )
            .abs();

    return String.join(".", String.join("&", dataContainer), checksum.toString());
  }

  String toHex(final byte[] toConvert) {
    final StringBuilder hexCollector = new StringBuilder();
    for (final byte b : toConvert) {
      hexCollector.append(String.format("%02x", b));
    }
    return hexCollector.toString();
  }
}
