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
package io.conjuror.component.journal.request;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Setter
public class CreateJournalRequest {
  @NotBlank
  @Size(min = 4, max = 64)
  private String identifier;
  @Size(max = 4096)
  private String description;
  @Size(min = 10, max = 10)
  private String valueDate;
  @Size(min = 10, max = 10)
  private String bookingDate;
  @NotBlank
  @Size(min = 3, max = 3)
  private String currencyCode;

  public static Builder create(final String identifier, final String currencyCode) {
    return new Builder(identifier, currencyCode);
  }

  @RequiredArgsConstructor
  public static class Builder {
    private final String identifier;
    private String description;
    private String valueDate;
    private String bookingDate;
    private final String currencyCode;

    public Builder description(final String description) {
      this.description = description;
      return this;
    }

    public Builder valueDate(final LocalDate valueDate) {
      this.valueDate = valueDate.format(DateTimeFormatter.ISO_DATE);
      return this;
    }

    public Builder bookingDate(final LocalDate bookingDate) {
      this.bookingDate = bookingDate.format(DateTimeFormatter.ISO_DATE);
      return this;
    }

    public CreateJournalRequest build() {
      return new CreateJournalRequest(this.identifier, this.description, this.valueDate,
          this.bookingDate, this.currencyCode);
    }
  }
}
