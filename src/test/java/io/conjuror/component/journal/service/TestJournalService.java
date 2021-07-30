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

import io.conjuror.component.journal.common.Balance;
import io.conjuror.component.journal.data.Document;
import io.conjuror.component.journal.data.Journal;
import io.conjuror.component.journal.data.JournalItem;
import io.conjuror.component.journal.processor.JournalRequestProcessor;
import io.conjuror.component.journal.request.AddItemRequest;
import io.conjuror.component.journal.request.AddItemRequest.Allocation;
import io.conjuror.component.journal.request.AddItemRequest.Builder;
import io.conjuror.component.journal.request.AttachDocumentRequest;
import io.conjuror.component.journal.request.CreateJournalRequest;
import io.conjuror.component.journal.request.TransitionJournalRequest;
import io.conjuror.component.journal.request.TransitionJournalRequest.Action;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.util.MimeTypeUtils;

@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {
        TestConfiguration.class
    }
)
public class TestJournalService extends IntegrationTestsSupport {

  final Random random = new Random();

  @Autowired
  private JournalRequestProcessor journalRequestProcessor;

  @Autowired
  private JournalService journalService;

  @Autowired
  private AccountService accountService;

  @Autowired
  private FingerprintService fingerprintService;

  public TestJournalService() {
    super();
  }

  @Test
  public void givenExistingJournal_whenAddingItem_shouldSucceed() throws Exception {
    final LocalDate now = LocalDate.now();
    final CreateJournalRequest createJournalRequest =
        CreateJournalRequest
            .create(this.randomString(), "EUR")
            .valueDate(now)
            .bookingDate(now)
            .build();

    final Long sequence = this.journalRequestProcessor.process(createJournalRequest);
    Assertions.assertNotNull(sequence);

    final Journal journal = this.journalService.findJournal(sequence);
    Assertions.assertNotNull(journal);

    final Builder itemRequestBuilder = AddItemRequest
        .create(this.randomString());

    final Allocation debtor = new Allocation();
    debtor.setAccountReference(this.randomString());
    debtor.setAmount(BigDecimal.TEN);
    itemRequestBuilder.source(debtor);

    final long creditorCount = 2L;
    final ArrayList<String> creditorAccounts = new ArrayList<>();
    Stream
        .iterate(0, index -> index += 1)
        .limit(creditorCount)
        .forEach(index -> {
          final Allocation creditor = new Allocation();
          creditor.setAccountReference(this.randomString());
          creditor.setAmount(
              debtor.getAmount()
                  .divide(BigDecimal.valueOf(creditorCount), MathContext.DECIMAL128)
          );
          itemRequestBuilder.addTarget(creditor);
          creditorAccounts.add(creditor.getAccountReference());
        });

    this.journalRequestProcessor.process(sequence, itemRequestBuilder.build());

    final List<JournalItem> journalItems = this.journalService.findAllItemsByJournal(sequence);
    Assertions.assertNotNull(journalItems);
    Assertions.assertEquals(1, journalItems.size());

    journalItems.forEach(journalItem -> {
      final JournalItem.Allocation source = journalItem.getSource();
      final Balance sourceBalance =
          this.accountService.determineBalance(source.getAccountReference(), "EUR");
      Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(sourceBalance.getAccountBalance()));

      journalItem.getTargets()
          .forEach(target -> {
            final Balance targetBalance =
                this.accountService.determineBalance(target.getAccountReference(), "EUR");
            Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(targetBalance.getAccountBalance()));
          });
    });

    final AttachDocumentRequest attachDocumentRequest =
        AttachDocumentRequest
            .create(MimeTypeUtils.IMAGE_PNG_VALUE)
            .bytes(10849L)
            .content("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")
            .build();

    final JournalItem journalItem = journalItems.get(0);
    this.journalRequestProcessor.process(sequence, journalItem.getSequence(), attachDocumentRequest);

    final List<Document> documents =
        this.journalService.findAllDocumentsByJournalItem(journalItem.getSequence());
    Assertions.assertNotNull(documents);
    Assertions.assertEquals(1, documents.size());

    final Document document = documents.get(0);
    Assertions.assertEquals(attachDocumentRequest.getMimeType(), document.getMimeType());
    Assertions.assertEquals(attachDocumentRequest.getBytes(), document.getBytes());
    Assertions.assertEquals(attachDocumentRequest.getContent(), document.getContent());

    this.journalRequestProcessor.process(sequence, TransitionJournalRequest.of(Action.SCHEDULE));

    this.journalRequestProcessor.process(sequence, TransitionJournalRequest.of(Action.RELEASE));

    final Balance releaseBalance =
        this.accountService.determineBalance(debtor.getAccountReference(), "EUR");
    Assertions.assertEquals(0, releaseBalance.getAccountBalance().compareTo(BigDecimal.TEN.negate()));

    final BigDecimal creditorAccountBalanceSum = creditorAccounts
        .stream()
        .map(accountNumber -> {
          final Balance balance = this.accountService.determineBalance(accountNumber, "EUR");
          return balance.getAccountBalance();
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    Assertions.assertEquals(0,
        creditorAccountBalanceSum
            .add(releaseBalance.getAccountBalance(), MathContext.DECIMAL128)
            .compareTo(BigDecimal.ZERO)
    );

    final Journal processedJournal = this.journalService.findJournal(sequence);
    Assertions.assertTrue(this.fingerprintService.valid(processedJournal.getFingerPrint(), sequence));
  }

  private String randomString() {
    final byte[] randomBytes = new byte[32];
    random.nextBytes(randomBytes);
    return Base64.getEncoder().encodeToString(randomBytes);
  }
}
