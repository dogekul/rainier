/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.fulllink;

import com.rainier.customer.dto.CustomerDetail;
import com.rainier.operation.dto.OperationDetail;
import com.rainier.opportunity.dto.OpportunityDetail;
import com.rainier.project.dto.ProjectDetail;
import java.util.ArrayList;
import java.util.List;

/**
 * Response for {@code GET /api/opportunities/{id}/full-link} (v0.0.94 D6). 商机为锚，把 Customer /
 * Project / Operation 全链节点串起来供前端时间线渲染。
 */
public class FullLinkResponse {

  private OpportunityDetail opportunity;
  private CustomerDetail customer;
  private ProjectDetail project;
  private OperationDetail operation;
  private List<StageSummary> presaleStages = new ArrayList<StageSummary>();
  private List<StageSummary> deliveryStages = new ArrayList<StageSummary>();

  public OpportunityDetail getOpportunity() {
    return opportunity;
  }

  public void setOpportunity(OpportunityDetail opportunity) {
    this.opportunity = opportunity;
  }

  public CustomerDetail getCustomer() {
    return customer;
  }

  public void setCustomer(CustomerDetail customer) {
    this.customer = customer;
  }

  public ProjectDetail getProject() {
    return project;
  }

  public void setProject(ProjectDetail project) {
    this.project = project;
  }

  public OperationDetail getOperation() {
    return operation;
  }

  public void setOperation(OperationDetail operation) {
    this.operation = operation;
  }

  public List<StageSummary> getPresaleStages() {
    return presaleStages;
  }

  public void setPresaleStages(List<StageSummary> presaleStages) {
    this.presaleStages = presaleStages;
  }

  public List<StageSummary> getDeliveryStages() {
    return deliveryStages;
  }

  public void setDeliveryStages(List<StageSummary> deliveryStages) {
    this.deliveryStages = deliveryStages;
  }
}
