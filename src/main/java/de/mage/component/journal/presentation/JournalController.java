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
package de.mage.component.journal.presentation;

import de.mage.component.journal.common.TypedPage;
import de.mage.component.journal.data.Document;
import de.mage.component.journal.data.Journal;
import de.mage.component.journal.data.JournalItem;
import de.mage.component.journal.domain.AddItemRequest;
import de.mage.component.journal.domain.AttachDocumentRequest;
import de.mage.component.journal.domain.CreateJournalRequest;
import de.mage.component.journal.domain.TransitionJournalRequest;
import de.mage.component.journal.processor.JournalRequestProcessor;
import de.mage.component.journal.service.JournalService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/journals")
public class JournalController {

  private final JournalRequestProcessor journalRequestProcessor;
  private final JournalService journalService;

  @Autowired
  public JournalController(
      final JournalRequestProcessor journalRequestProcessor,
      final JournalService journalService) {
    super();
    this.journalRequestProcessor = journalRequestProcessor;
    this.journalService = journalService;
  }

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.OK)
  public Long createJournal(final CreateJournalRequest request) {
    return this.journalRequestProcessor.process(request);
  }

  @GetMapping(
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.OK)
  public TypedPage<Journal> fetchJournals(
      @RequestParam(value = "p", defaultValue = "0") final Integer page,
      @RequestParam(value = "s", defaultValue = "20") final Integer size) {
    return this.journalService.fetchJournals(page, size);
  }

  @GetMapping(
      path = "/{sequence}",
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.OK)
  public Journal getJournal(@PathVariable("sequence") final Long sequence) {
    return this.journalService.findJournal(sequence);
  }

  @PostMapping(
      path = "/{sequence}/items",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void addJournalItem(@PathVariable("sequence") final Long sequence,
      @RequestBody @Valid final AddItemRequest request) {
    this.journalRequestProcessor.process(sequence, request);
  }

  @GetMapping(
      path = "/{sequence}/items",
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.OK)
  public List<JournalItem> fetchItemsByJournal(@PathVariable("sequence") final Long sequence) {
    return this.journalService.findAllItemsByJournal(sequence);
  }

  @PostMapping(
      path = "/{sequence}/items/{itemSequence}/documents",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void attachDocumentToJournalItem(@PathVariable("sequence") final Long sequence,
      @PathVariable("itemSequence") final Long itemSequence,
      @RequestBody @Valid final AttachDocumentRequest request) {
    this.journalRequestProcessor.process(sequence, itemSequence, request);
  }

  @GetMapping(
      path = "/{sequence}/items/{itemSequence}/documents",
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.OK)
  public List<Document> fetchDocumentsByItem(@PathVariable("sequence") final Long sequence,
  @PathVariable("itemSequence") final Long itemSequence) {
    return this.journalService.findAllDocumentsByJournalItem(itemSequence);
  }

  @PostMapping(
      path = "/{sequence}/states",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void transitionJournal(@PathVariable("sequence") final Long sequence,
      @RequestBody @Valid final TransitionJournalRequest request) {
    this.journalRequestProcessor.process(sequence, request);
  }
}
