# Vision: A Simple and Powerful Authorization Model with Cerbos

This document presents a clear vision for integrating Cerbos to manage authorization in the Flowable ecosystem. The goal is to create a system that is secure, flexible, and easy for developers and business analysts to understand and manage.

## 1. The Big Idea: Keep It Simple

Our authorization model is built on a simple but powerful idea: every time a user tries to do something, we check if they are allowed. We do this by answering three basic questions:

1.  **Who is the user?** (This is the `Principal`)
2.  **What are they trying to do?** (This is the `Action`, like "claim" or "approve")
3.  **What are they trying to do it on?** (This is the `Resource`, which is the specific workflow they are interacting with)

By separating the "who" from the "what," we can manage business rules in one central place (Cerbos) instead of scattering them throughout the application code.

## 2. How It Works: A Step-by-Step Flow

Let's imagine a user named Sally tries to approve an expense report. Here is a simple, visual guide to what happens behind the scenes.

```
USER ACTION: Sally clicks "Approve" on a $5,000 expense report.

      │
      ▼
┌──────────────────────────┐
│ Flowable Wrapper API     │  1. The API receives the request and pauses.
│                          │     Instead of immediately approving, it first needs to ask for permission.
└──────────────────────────┘
      │
      │
      ▼
┌──────────────────────────┐   ┌──────────────────────────┐
│ User Profile Store (IAM) │   │ Workflow DB (Flowable)   │  2. The API gathers information from two separate sources:
│ fetch("sally")           │   │ fetch("expense-report-123")│     - It gets Sally's details (her roles and attributes).
└──────────────────────────┘   └──────────────────────────┘     - It gets the expense report's details (the amount, who created it).
      │                               │
      └─────────────┬───────────────┘
                    │
                    ▼
┌──────────────────────────┐
│ Cerbos                   │  3. The API sends all this information to Cerbos and asks a simple question:
│                          │     "Can Sally approve this expense report?"
└──────────────────────────┘
      │
      │
      ▼
┌──────────────────────────┐
│ Cerbos Policy Engine     │  4. Cerbos uses pre-defined rules to make a decision.
│                          │     For example, a rule might say: "A person cannot approve their own expense report."
│                          │     Cerbos sees Sally submitted this report, so it answers "DENY".
└──────────────────────────┘
      │
      │
      ▼
┌──────────────────────────┐
│ Flowable Wrapper API     │  5. The API receives the "DENY" decision and stops.
│                          │     It sends a clear error message back to Sally. The workflow is never touched.
└──────────────────────────┘
      │
      ▼
END: The action is safely blocked, and the reason is clear.

```

## 3. Managing Your Users (The Principal)

A policy is only as good as the information it has about the user. This is where user management becomes critical.

### The Onboarding Process

We need a clear way to manage what users are allowed to do. This involves a simple, three-step onboarding process, managed via an API:

1.  **Create the User:** First, the user's basic profile is created.
2.  **Assign to a Workflow:** Next, the user is granted access to specific workflows, like "Expense Reimbursement" or "Vacation Requests," and given roles within them (e.g., "submitter," "approver").
3.  **Define Attributes:** Finally, we add specific details about the user that are important for our rules, such as their department, their spending approval limit, or their geographic location.

### How It Connects to the Application

When a user logs into the application, the system uses the information defined during onboarding to build a complete user profile. This profile, called the `Principal`, is attached to their session.

For every action they take, this rich profile is what gets sent to Cerbos, allowing for very specific and powerful rules.

## 4. The Data We Use for Decisions

Here are simple examples of the data sent to Cerbos to make a decision.

### The User's Profile (`Principal`)

This object represents the person taking the action. It contains everything Cerbos needs to know about them.

```json
{
  "id": "sally_jones",
  "roles": ["manager", "expense_approver"],
  "attributes": {
    "department": "Sales",
    "geography": "EMEA",
    "approval_limit": 10000
  }
}
```

### The Workflow's Data (`Resource`)

This object represents the item being worked on. It contains all the important details about the specific workflow instance.

```json
{
  "kind": "ExpenseReimbursement",
  "id": "proc-inst-9f86b4d1",
  "attributes": {
    "amount": 7500,
    "currency": "USD",
    "requester_id": "bob_smith",
    "region": "EMEA"
  }
}
```

### A Sample Cerbos Policy

With this data, we can write very clear, powerful rules in Cerbos. This example policy checks three things before allowing an approval:

1.  The user must have the "approver" role.
2.  The user's approval limit must be greater than the expense amount.
3.  The user cannot be the same person who requested the expense.

```yaml
- actions: ["approve"]
  effect: EFFECT_ALLOW
  roles:
    - "approver"

  condition:
    match:
      all:
        - expr: "P.attr.approval_limit >= R.attr.amount"
        - expr: "P.id != R.attr.requester_id"
```

## 5. How to Handle Lists and Queues

A common challenge is how to show a user a list of only the tasks they are allowed to see, without checking thousands of items one by one.

Cerbos solves this elegantly. Instead of asking "Can Sally see this task?" for every task, we ask a different question: **"Tell me the rules for what Sally is allowed to see."**

Cerbos then provides a set of conditions. The application translates these conditions directly into a database query. The result is a single, highly efficient database call that fetches *only* the tasks Sally is permitted to see.
