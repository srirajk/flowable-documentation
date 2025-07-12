# Purchase Order Approval Workflow Diagram

## Visual Flow

```
┌─────────────────┐
│   Start Event   │
│  (PO Submitted) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Manager Review  │ ──────► Queue: manager-queue
│ (User Task)     │         Group: managers
└────────┬────────┘
         │
         ▼
    ┌────────────┐
    │  Manager   │
    │ Decision?  │
    └─────┬──────┘
          │
    ┌─────┴─────┬──────────┐
    │           │           │
    ▼           ▼           ▼
[Approve]  [Escalate]  [Reject]
    │           │           │
    │           ▼           │
    │  ┌─────────────────┐  │
    │  │ Director Review │  │ ──────► Queue: director-queue
    │  │ (User Task)     │  │         Group: directors
    │  └────────┬────────┘  │
    │           │           │
    │           ▼           │
    │     ┌──────────┐      │
    │     │ Director │      │
    │     │Decision? │      │
    │     └────┬─────┘      │
    │          │            │
    │    ┌─────┴─────┐      │
    │    │           │      │
    │    ▼           ▼      │
    │ [Approve]  [Reject]   │
    │    │           │      │
    └────┘           │      │
         │           │      │
         ▼           │      │
    ┌────────────┐   │      │
    │  Amount    │   │      │
    │  Check?    │   │      │
    └─────┬──────┘   │      │
          │          │      │
    ┌─────┴─────┐    │      │
    │           │    │      │
    ▼           ▼    │      │
[< $5000]   [≥ $5000]│      │
    │           │    │      │
    │           ▼    │      │
    │  ┌─────────────────┐  │
    │  │ Finance Review  │  │ ──────► Queue: finance-queue
    │  │ (User Task)     │  │         Group: finance
    │  └────────┬────────┘  │
    │           │           │
    │           ▼           │
    │     ┌──────────┐      │
    │     │ Finance  │      │
    │     │Decision? │      │
    │     └────┬─────┘      │
    │          │            │
    │    ┌─────┴─────┐      │
    │    │           │      │
    │    ▼           ▼      │
    │ [Approve]  [Reject]───┤
    │    │                  │
    └────┘                  │
         │                  │
         ▼                  ▼
┌─────────────────┐   ┌─────────────────┐
│  Procurement    │   │Send Rejection   │
│  Processing     │   │Notification     │
│  (User Task)    │   │(Service Task)   │
└────────┬────────┘   └────────┬────────┘
         │                     │
         │ Queue: procurement  │
         │ Group: procurement  │
         ▼                     ▼
   ┌──────────┐         ┌──────────┐
   │   End    │         │   End    │
   │(Complete)│         │(Rejected)│
   └──────────┘         └──────────┘
```

## Queue Distribution

| Task | Candidate Group | Queue Name | Purpose |
|------|----------------|------------|---------|
| Manager Review | managers | manager-queue | Initial approval |
| Director Review | directors | director-queue | Escalated approvals |
| Finance Review | finance | finance-queue | Budget validation (≥$5000) |
| Procurement Processing | procurement | procurement-queue | Final processing |

## Decision Points

1. **Manager Decision**
   - Approve → Check amount
   - Escalate → Director review
   - Reject → Send notification

2. **Amount Gateway**
   - < $5,000 → Direct to procurement
   - ≥ $5,000 → Finance review required

3. **Director Decision**
   - Approve → Check amount
   - Reject → Send notification

4. **Finance Decision**
   - Approve → Procurement processing
   - Reject → Send notification

## Process Variables

- `orderId`: Unique order identifier
- `requester`: Email of requester
- `department`: Requesting department
- `amount`: Order amount (determines routing)
- `description`: Order description
- `urgency`: Priority level
- `decision`: Manager's decision
- `directorDecision`: Director's decision
- `financeApproval`: Finance decision
- `budgetCode`: Assigned budget code
- `vendorSelected`: Selected vendor
- `purchaseOrderNumber`: Final PO number