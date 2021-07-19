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
package io.conjuror.component.journal.processor;

import io.conjuror.component.journal.data.Document;
import io.conjuror.component.journal.data.Journal;
import io.conjuror.component.journal.data.Journal.State;
import io.conjuror.component.journal.data.JournalItem;
import io.conjuror.component.journal.data.JournalItem.Allocation;
import io.conjuror.component.journal.exception.RequestValidationException;
import io.conjuror.component.journal.exception.ResourceConflictException;
import io.conjuror.component.journal.exception.ResourceNotFoundException;
import io.conjuror.component.journal.repository.DocumentRepository;
import io.conjuror.component.journal.repository.JournalItemRepository;
import io.conjuror.component.journal.repository.JournalRepository;
import io.conjuror.component.journal.request.AddItemRequest;
import io.conjuror.component.journal.request.AttachDocumentRequest;
import io.conjuror.component.journal.request.CreateJournalRequest;
import io.conjuror.component.journal.request.TransitionJournalRequest;
import io.conjuror.component.journal.request.TransitionJournalRequest.Action;
import io.conjuror.component.journal.service.SnowflakeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalRequestProcessor {

  private final SnowflakeService snowflakeService;
  private final JournalRepository journalRepository;
  private final JournalItemRepository journalItemRepository;
  private final DocumentRepository documentRepository;

  @Autowired
  public JournalRequestProcessor(
      final SnowflakeService snowflakeService,
      final JournalRepository journalRepository,
      final JournalItemRepository journalItemRepository,
      final DocumentRepository documentRepository) {
    this.snowflakeService = snowflakeService;
    this.journalRepository = journalRepository;
    this.journalItemRepository = journalItemRepository;
    this.documentRepository = documentRepository;
  }

  @Transactional
  public Long process(final CreateJournalRequest request) {
    if (this.journalRepository.findByIdentifier(request.getIdentifier()).isPresent()) {
      throw new ResourceConflictException(
          String.format("Journal with identifier '%s' already exists.", request.getIdentifier())
      );
    }

    final long sequence = this.snowflakeService.next();
    final Journal journal = new Journal();
    journal.setSequence(sequence);
    journal.setIdentifier(request.getIdentifier());
    journal.setDescription(request.getBookingDate());
    if (request.getValueDate() != null) {
      journal.setValueDate(
          LocalDate.parse(request.getValueDate(), DateTimeFormatter.ISO_DATE)
      );
    }
    if (request.getBookingDate() != null) {
      journal.setBookingDate(
          LocalDate.parse(request.getBookingDate(), DateTimeFormatter.ISO_DATE)
      );
    }
    journal.setCurrencyCode(request.getCurrencyCode());
    journal.setState(State.PREPARATION);
    this.journalRepository.save(journal);
    return sequence;
  }

  @Transactional
  public void process(final Long sequence, final AddItemRequest request) {
    final Journal journal = this.resolveAndValidate(sequence, State.PREPARATION);

    final JournalItem journalItem = new JournalItem();
    journalItem.setSequence(this.snowflakeService.next());
    journalItem.setJournalSequence(journal.getSequence());
    journalItem.setIdentifier(request.getIdentifier());

    final Allocation source = new Allocation();
    journalItem.setSource(source);
    source.setAccountReference(request.getSource().getAccountReference());
    source.setAmount(request.getSource().getAmount());

    final AtomicReference<BigDecimal> targetSumReference = new AtomicReference<>(BigDecimal.ZERO);
    journalItem.setTargets(
        request.getTargets()
            .stream()
            .map(allocation -> {
              targetSumReference.getAndAccumulate(allocation.getAmount(), BigDecimal::add);
              final Allocation target = new Allocation();
              target.setAccountReference(allocation.getAccountReference());
              target.setAmount(allocation.getAmount());
              return target;
            })
            .collect(Collectors.toList())
    );

    if (source.getAmount().compareTo(targetSumReference.get()) != 0) {
      throw new RequestValidationException("Item is not in balance.");
    }

    journalItem.setPurpose(request.getPurpose());

    this.journalItemRepository.save(journalItem);
  }

  @Transactional
  public void process(final Long sequence, final Long itemSequence,
      final AttachDocumentRequest request) {
    this.resolveAndValidate(sequence, State.PREPARATION);

    final JournalItem referencedItem =
        this.journalItemRepository
            .findById(itemSequence)
            .orElseThrow(() ->
                new ResourceNotFoundException(String.format("Journal item '%s' not found.", itemSequence))
            );

    final Document document = new Document();
    document.setSequence(this.snowflakeService.next());
    document.setJournalItemSequence(referencedItem.getSequence());
    document.setMimeType(request.getMimeType());
    document.setBytes(request.getBytes());
    document.setContent(request.getContent());

    this.documentRepository.save(document);
  }

  @Transactional
  public void process(final Long sequence, final TransitionJournalRequest request) {
    final Action action = request.getAction();
    final Journal journal = this.resolveAndValidate(sequence, action.expectedState());
    journal.setState(action.desiredState());
    this.journalRepository.save(journal);
  }

  Journal resolveAndValidate(final Long sequence, final State expectedState) {
    final Journal journal = this.journalRepository.findById(sequence)
        .orElseThrow(() ->
            new ResourceNotFoundException(String.format("Journal '%s' not found.", sequence))
        );

    if (journal.getState() != expectedState) {
      throw new ResourceConflictException(
          String.format("Journal '%s' is not in expected state.", sequence)
      );
    }

    return journal;
  }
}
