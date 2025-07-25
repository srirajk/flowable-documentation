Step-by-step breakdown
Outer !() (NOT)

The entire block is negated. The claim is allowed only if none of the inner conditions are true.
The block is a series of OR (||) conditions, each checking a different scenario. If any are true, the claim is denied.

First Condition
  Checks if the current task is the L1 checker review task.
  Checks if the L1 maker review task exists.
  Checks if the current user was the assignee for the L1 maker review task.
  If all are true, the user cannot claim the L1 checker task after doing the L1 maker task.
Second Condition
  Checks if the current task is the L1 maker review task.
  Checks if the L1 checker review task exists.
  Checks if the current user was the assignee for the L1 checker review task.
  If all are true, the user cannot claim the L1 maker task after doing the L1 checker task.
Third Condition
  Checks if the current task is the L2 checker review task.
  Checks if the L2 maker review task exists.
  Checks if the current user was the assignee for the L2 maker review task.
  If all are true, the user cannot claim the L2 checker task after doing the L2 maker task.
Fourth Condition
  Checks if the current task is the L2 maker review task.
  Checks if the L2 checker review task exists.
  Checks if the current user was the assignee for the L2 checker review task.
  If all are true, the user cannot claim the L2 maker task after doing the L2 checker task.