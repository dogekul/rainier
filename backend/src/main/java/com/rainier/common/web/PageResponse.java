/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.web;

import java.util.List;

/**
 * Stable envelope returned by all list endpoints. Exposing only 4 fields keeps the front-end
 * contract simple and isolates it from Spring Data {@code Page} internals.
 *
 * @param <T> the element type of {@code content}
 */
public class PageResponse<T> {

  private List<T> content;
  private int page;
  private int size;
  private long total;

  public PageResponse() {
    // for Jackson
  }

  public PageResponse(List<T> content, int page, int size, long total) {
    this.content = content;
    this.page = page;
    this.size = size;
    this.total = total;
  }

  public static <T> PageResponse<T> of(List<T> content, int page, int size, long total) {
    return new PageResponse<>(content, page, size, total);
  }

  public List<T> getContent() {
    return content;
  }

  public void setContent(List<T> content) {
    this.content = content;
  }

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

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }
}
