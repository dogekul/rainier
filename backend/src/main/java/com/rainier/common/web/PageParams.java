/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.web;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Common pagination + free-text search query parameters. Bound from the query string on list
 * endpoints; validated via {@code @Valid} which surfaces violations through {@code
 * GlobalExceptionHandler} as 400 JSON with {@code fieldErrors[]}.
 *
 * <p>Defaults: {@code page=0}, {@code size=20}, {@code search=null}. Size is capped at 100 to
 * prevent accidental large reads.
 */
public class PageParams {

  @Min(value = 0, message = "page must be >= 0")
  private int page = 0;

  @Min(value = 1, message = "size must be >= 1")
  @Max(value = 100, message = "size must be <= 100")
  private int size = 20;

  private String search;

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }
}
