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
package de.mage.component.journal.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
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
public class AddItemRequest {
  @NotBlank
  @Size(min = 4, max = 64)
  private String identifier;
  @NotNull
  @Size(min = 1)
  private Allocation source;
  @NotNull
  @Size(min = 1)
  private List<Allocation> targets;
  @Size(max = 4096)
  private String purpose;

  @NoArgsConstructor
  @Getter
  @Setter
  public static class Allocation {
    @NotBlank
    @Size(min = 4, max = 64)
    private String accountReference;
    @NotNull
    @Positive
    @Digits(integer = 12, fraction = 12)
    private BigDecimal amount;
  }

  public static Builder create(final String identifier) {
    return new Builder(identifier);
  }

  @RequiredArgsConstructor
  public static class Builder {
    private final String identifier;
    private Allocation source;
    private List<Allocation> targets;
    private String purpose;

    public Builder source(final Allocation source) {
      this.source = source;
      return this;
    }

    public Builder addTarget(final Allocation target) {
      if (this.targets == null) {
        this.targets = new ArrayList<>();
      }
      this.targets.add(target);
      return this;
    }

    public Builder purpose(final String purpose) {
      this.purpose = purpose;
      return this;
    }

    public AddItemRequest build() {
      return new AddItemRequest(this.identifier, this.source, this.targets, this.purpose);
    }
  }
}
