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

import de.mage.component.journal.JournalConfiguration;
import de.mage.component.journal.common.TypedPage;
import de.mage.component.journal.data.Document;
import de.mage.component.journal.data.Journal;
import de.mage.component.journal.data.JournalItem;
import de.mage.component.journal.exception.ResourceNotFoundException;
import de.mage.component.journal.repository.DocumentRepository;
import de.mage.component.journal.repository.JournalItemRepository;
import de.mage.component.journal.repository.JournalRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

@Service
public class JournalService {

  private final JournalRepository journalRepository;
  private final JournalItemRepository journalItemRepository;
  private final DocumentRepository documentRepository;

  @Autowired
  public JournalService(
      final JournalRepository journalRepository,
      final JournalItemRepository journalItemRepository,
      final DocumentRepository documentRepository) {
    super();
    this.journalRepository = journalRepository;
    this.journalItemRepository = journalItemRepository;
    this.documentRepository = documentRepository;
  }

  public TypedPage<Journal> fetchJournals(final Integer page, final Integer size) {
    final Page<Journal> resultPage =
        this.journalRepository.findAll(
            PageRequest.of(page, size, Sort.by(Direction.DESC, "sequence"))
        );

    return TypedPage.of(resultPage.getContent(), resultPage.getTotalPages(), resultPage.getTotalElements());
  }

  public Journal findJournal(final Long sequence) {
    return this.journalRepository.findById(sequence)
        .orElseThrow(() ->
            new ResourceNotFoundException(String.format("Journal '%s' not found.", sequence))
        );
  }

  public List<JournalItem> findAllItemsByJournal(final Long sequence) {
    final Journal journal = this.findJournal(sequence);
    return this.journalItemRepository.findAllByJournalSequenceOrderBySequence(journal.getSequence());
  }

  public List<Document> findAllDocumentsByJournalItem(final Long sequence) {
    return this.documentRepository.findAllByJournalItemSequence(sequence);
  }
}
