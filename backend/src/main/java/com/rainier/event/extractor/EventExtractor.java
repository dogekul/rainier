/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.extractor;

import com.rainier.event.domain.Event;
import java.util.Optional;

/**
 * Strategy interface: given a raw {@link Event}, decide whether this extractor can handle it
 * ({@link #supports}) and, if so, return the internal entity ref it should attach to ({@link
 * #extract}). Multiple implementations are scanned in Spring bean order; the first matching one
 * wins.
 */
public interface EventExtractor {

  boolean supports(Event event);

  Optional<ExtractionResult> extract(Event event);
}
