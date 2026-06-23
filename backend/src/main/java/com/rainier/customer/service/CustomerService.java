/* (C) 2026 Rainier — internal use only. */
package com.rainier.customer.service;

import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.customer.domain.Customer;
import com.rainier.customer.dto.CustomerCreateRequest;
import com.rainier.customer.dto.CustomerDetail;
import com.rainier.customer.dto.CustomerUpdateRequest;
import com.rainier.customer.repository.CustomerRepository;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD for {@link Customer} (v0.0.45). all-users (token-gated). Soft-deleted. */
@Service
@Transactional(readOnly = true)
public class CustomerService {

  private final CustomerRepository repo;

  public CustomerService(CustomerRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public CustomerDetail create(CustomerCreateRequest req) {
    Customer c = new Customer();
    c.setName(req.getName().trim());
    c.setIndustry(req.getIndustry());
    c.setContactName(req.getContactName());
    c.setNotes(req.getNotes());
    return CustomerDetail.from(repo.saveAndFlush(c));
  }

  public CustomerDetail findById(Long id) {
    return CustomerDetail.from(getOrThrow(id));
  }

  public PageResponse<CustomerDetail> list(PageParams page) {
    Specification<Customer> spec =
        (root, query, cb) -> {
          Predicate p = cb.conjunction();
          String search = page.getSearch();
          if (search != null && !search.isEmpty()) {
            String pattern = "%" + search.toLowerCase() + "%";
            p =
                cb.and(
                    p,
                    cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("industry")), pattern),
                        cb.like(cb.lower(root.get("contactName")), pattern)));
          }
          return p;
        };
    PageRequest pr =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<Customer> result = repo.findAll(spec, pr);
    return PageResponse.of(
        result.stream().map(CustomerDetail::from).collect(java.util.stream.Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public CustomerDetail update(Long id, CustomerUpdateRequest req) {
    Customer c = getOrThrow(id);
    c.setName(req.getName().trim());
    c.setIndustry(req.getIndustry());
    c.setContactName(req.getContactName());
    c.setNotes(req.getNotes());
    return CustomerDetail.from(repo.saveAndFlush(c));
  }

  @Transactional
  public void delete(Long id) {
    repo.delete(getOrThrow(id));
  }

  private Customer getOrThrow(Long id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("customer not found: id=" + id));
  }
}
