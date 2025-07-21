# Sanctions Case Management System - Requirements

## Overview

A comprehensive case management system for handling sanctions screening alerts. When the screening engine detects potential sanctions hits, it triggers this workflow system to manage the review, validation, and decision-making process across multiple organizational levels.

## System Flow Architecture

```
Screening Engine → Sanctions Hit → Case Management System
                                             ↓
                      ┌─────────────────────────────────────┐
                      │           LEVEL 1                   │
                      │                                     │
                      │  Maker Path    │    Checker Path    │
                      │  (parallel)    │    (parallel)      │
                      │      ↓         │         ↓          │
                      │  Decisions on  │   Decisions on     │
                      │  all matches   │   all matches      │
                      │      ↓         │         ↓          │
                      │  Validation    │   Validation       │
                      └─────────────────────────────────────┘
                                             ↓
                                   Gateway Logic (All True?)
                                   ↓                    ↓
                              Auto to L2          L1 Supervisor
                                   ↓                    ↓
                          ┌─────────────────────────────────────┐
                          │           LEVEL 2                   │
                          │                                     │
                          │  Maker Path    │    Checker Path    │
                          │  (parallel)    │    (parallel)      │
                          │      ↓         │         ↓          │
                          │  Decisions on  │   Decisions on     │
                          │  all matches   │   all matches      │
                          │      ↓         │         ↓          │
                          │  Validation    │   Validation       │
                          └─────────────────────────────────────┘
                                             ↓
                                   Gateway Logic (All True?)
                                   ↓                    ↓
                              Auto to L3          L2 Supervisor
                                   ↓                    ↓
                          ┌─────────────────────────────────────┐
                          │           LEVEL 3 (Parallel)       │
                          │                                     │
                          │ Narcotics │ Terrorism │ Money      │
                          │   SME     │    SME    │ Laundering │
                          │  Queue    │   Queue   │    SME     │
                          │     ↓     │     ↓     │   Queue    │
                          │ Decision  │ Decision  │     ↓      │
                          │           │           │  Decision  │
                          └─────────────────────────────────────┘
                                             ↓
                                      L3 Supervisor
                                             ↓
                                      Final Decision
```

## Core Data Structure

### Case Payload
```json
{
  "caseId": "CASE-001",
  "region": "APAC",              // APAC, EU, or NAM
  "customer": {
    "name": "John Doe",
    "account": "12345",
    "customerType": "Individual"
  },
  "matches": [
    {
      "matchId": "M1",
      "sanctionedEntity": "John DOE",
      "confidence": 85,
      "category": "narcotics",           // Category for L3 routing
      "level1MakerDecision": "",         // true_match | false_positive
      "level1CheckerDecision": "",       // true_match | false_positive
      "level1MakerComment": "",
      "level1CheckerComment": "",
      "level2MakerDecision": "",
      "level2CheckerDecision": "",
      "level2MakerComment": "",
      "level2CheckerComment": "",
      "level3ExpertDecision": "",        // Final SME decision
      "level3ExpertComment": ""
    },
    {
      "matchId": "M2", 
      "sanctionedEntity": "J. Doe",
      "confidence": 70,
      "category": "terrorism",
      // ... similar decision fields
    }
  ],
  "metadata": {
    "createdDate": "2025-01-17T10:00:00Z",
    "priority": "HIGH",
    "screeningEngine": "ACME_SCREENING_V2"
  }
}
```

## Queue Structure

### Level 1 & 2 Queues
```json
{
  "candidateGroupMappings": {
    "level1-maker": "level1-maker-queue",
    "level1-checker": "level1-checker-queue", 
    "level1-supervisor": "level1-supervisor-queue",
    "level2-maker": "level2-maker-queue",
    "level2-checker": "level2-checker-queue",
    "level2-supervisor": "level2-supervisor-queue"
  }
}
```

### Level 3 Expert Queues (Category-Based)
```json
{
  "candidateGroupMappings": {
    "narcotics-expert": "narcotics-expert-queue",
    "terrorism-expert": "terrorism-expert-queue",
    "money-laundering-expert": "aml-expert-queue",
    "level3-supervisor": "level3-supervisor-queue"
  }
}
```

## Workflow Logic & Routing Rules

### Level 1 Processing
1. **Parallel Execution**: Maker and Checker work simultaneously on same case
2. **Task**: Review all matches and provide decisions (true_match/false_positive)
3. **Form**: Single form showing all matches for decision-making
4. **Validation**: Must provide decisions for ALL matches before submission
5. **Gateway Logic**: 
   - If ALL matches have `level1MakerDecision=true_match AND level1CheckerDecision=true_match` → **Auto route to Level 2**
   - If ANY match has `level1MakerDecision=false_positive OR level1CheckerDecision=false_positive` → **Route to L1 Supervisor**

### Level 1 Supervisor
- **Task**: Review maker/checker decisions and case details
- **Decision Options**:
  - Send to Level 2 for further review
  - Close case (if confident it's false positive)

### Level 2 Processing
- **Same Logic as Level 1**: Parallel maker/checker review
- **Gateway Logic**: Same as Level 1
- **Routing**: 
  - All true matches → Auto to Level 3
  - Any false positives → L2 Supervisor

### Level 2 Supervisor  
- **Same Function as L1 Supervisor**:
  - Send to Level 3 for expert review
  - Close case

### Level 3 Expert Processing

#### Dynamic Expert Task Activation
**Challenge**: Cases may contain different combinations of categories (narcotics, terrorism, money-laundering, etc.). We need to dynamically activate only the relevant expert tasks without pre-coding every possible combination.

**Solution**: Explicit Branch Declaration with Conditional Gateways

1. **Category Detection Script**: 
   ```groovy
   def matches = execution.getVariable("matches") as List;
   def categories = matches.collect { it.category }.unique();
   
   // Set boolean flags for each possible category
   execution.setVariable("needsNarcotics", categories.contains("narcotics"));
   execution.setVariable("needsTerrorism", categories.contains("terrorism"));
   execution.setVariable("needsAML", categories.contains("money-laundering"));
   
   // Store categories for filtering
   execution.setVariable("requiredCategories", categories);
   ```

2. **BPMN Flow Structure**:
   ```
   Level 2 Gateway → Category Detection → Parallel Gateway
                                               ↓
           ┌─────────────────────────────────────────────────┐
           │                                                 │
     Exclusive Gateway    Exclusive Gateway    Exclusive Gateway
     (needsNarcotics?)    (needsTerrorism?)    (needsAML?)
           ↓                      ↓                   ↓
      Narcotics Expert      Terrorism Expert     AML Expert
         Task                   Task              Task
           ↓                      ↓                   ↓
        End Event              End Event         End Event
           │                      │                   │
           └──────────────────────┼───────────────────┘
                                  ↓
                           Join Gateway
                                  ↓
                         L3 Supervisor
   ```

3. **Category-Specific Data Filtering**:
   ```groovy
   // For each expert task - filter matches by category
   def allMatches = execution.getVariable("matches") as List;
   def narcoticsMatches = allMatches.findAll { it.category == "narcotics" };
   execution.setVariable("narcoticsMatches", narcoticsMatches);
   ```

4. **Behavior Examples**:
   - **Case with 2 categories** (narcotics, terrorism): Only narcotics and terrorism expert tasks activate
   - **Case with 1 category** (aml): Only AML expert task activates, other branches skip to end events
   - **Case with all categories**: All expert tasks activate in parallel

#### Technical Implementation
1. **Category-Based Routing**: Matches routed to expert queues based on `category` field
   - `narcotics` → `narcotics-expert-queue`
   - `terrorism` → `terrorism-expert-queue` 
   - `money-laundering` → `aml-expert-queue`

2. **Parallel Processing**: All relevant expert queues work simultaneously (only activated branches)

3. **Expert Task**: SMEs review matches in their domain expertise and provide final decisions

4. **Consolidation**: All expert decisions feed into L3 Supervisor for final case resolution

#### Design Decision Rationale
**Chosen Approach**: Explicit Branch Declaration
- **Why**: Sanctions categories are typically stable and limited (3-5 categories)
- **Benefits**: Visual BPMN flow, easy debugging, works with existing wrapper queue mappings
- **Trade-off**: New categories require BPMN modifications, but this is acceptable for stable domain

**Alternative Considered**: Multi-Instance For Each Loop
- **Why Not**: More complex queue assignment, harder debugging
- **Future**: Can migrate to this approach if categories exceed 10+ or become highly dynamic

### Level 3 Supervisor
- **Task**: Review all expert decisions and make final case determination
- **Decision**: Final case closure with overall determination

## User Interface Requirements

### Task Form Structure (Levels 1 & 2)
```
Case: CASE-001 - John Doe (Region: APAC)

Match 1: John DOE (Narcotics) - 85% confidence
Decision: [True Match] [False Positive] 
Comment: [text box]

Match 2: J. Doe (Terrorism) - 70% confidence  
Decision: [True Match] [False Positive]
Comment: [text box]

Match 3: Johnny Doe (Money Laundering) - 60% confidence
Decision: [True Match] [False Positive] 
Comment: [text box]

[Submit All Decisions]
```

### Expert Task Form (Level 3)
```
Case: CASE-001 - Expert Review (Narcotics)

Previous Reviews:
- Level 1 Maker: True Match
- Level 1 Checker: True Match  
- Level 2 Maker: True Match
- Level 2 Checker: True Match

Matches for Your Review:
Match 1: John DOE (Narcotics) - 85% confidence
Expert Decision: [True Match] [False Positive]
Expert Analysis: [detailed text box]

[Submit Expert Decision]
```

## Validation Requirements

### Universal Validation (All Levels)
- **Completeness Check**: All matches must have decisions provided
- **Decision Validation**: Each match must have valid decision selection
- **Comment Validation**: Comments required for all decisions (configurable)

### Validation Implementation
```groovy
// BPMN Script Task Validation Example
def matches = execution.getVariable("matches") as List;
def userDecisions = execution.getVariable("userDecisions") as List;
def errors = new java.util.ArrayList();

// Check completeness
if (userDecisions.size() != matches.size()) {
    errors.add("Please provide decisions for all " + matches.size() + " matches");
}

// Check decision validity
userDecisions.each { decision ->
    if (!decision.decision || decision.decision.trim() == "") {
        errors.add("Please select True Match or False Positive for all matches");
    }
    if (!decision.comment || decision.comment.trim().length() < 10) {
        errors.add("Please provide detailed comments (minimum 10 characters) for all decisions");
    }
}

// Set validation results
if (errors.isEmpty()) {
    execution.setVariable(taskId + "Valid", true);
} else {
    execution.setVariable(taskId + "Valid", false);
    execution.setVariable(taskId + "ValidationError", errors);
}
```

## Technical Implementation Notes

### Process Variable Management
- **Main Case Data**: Stored as `matches` process variable
- **User Input**: Captured as `userDecisions` in task completion
- **Decision Merge**: Script tasks merge user decisions into matches array
- **Validation State**: Standard validation variables per task

### Gateway Decision Logic
```groovy
// Level 1 to Level 2 Gateway
def matches = execution.getVariable("matches") as List;
def allTrueMatches = matches.every { match ->
    match.level1MakerDecision == "true_match" && 
    match.level1CheckerDecision == "true_match"
};
execution.setVariable("autoToLevel2", allTrueMatches);
```

### Category-Based Routing (Level 3)
```groovy
// Route matches to appropriate expert queues
def matches = execution.getVariable("matches") as List;
def categories = matches.collect { it.category }.unique();

categories.each { category ->
    def categoryMatches = matches.findAll { it.category == category };
    execution.setVariable(category + "Matches", categoryMatches);
}
```

## Future Enhancements

### Regional Authorization (Future)
- Region-based queue assignments
- Regional supervisor hierarchies  
- Region-specific compliance rules

### Advanced Features (Future)
- SLA tracking and escalation
- Bulk case processing
- Machine learning confidence scoring
- Audit trail and reporting
- Integration with external sanctions databases

## Success Criteria

1. **Workflow Efficiency**: Cases flow smoothly through all levels without manual intervention
2. **Decision Tracking**: Complete audit trail of all decisions and comments
3. **Expert Routing**: Accurate category-based routing to subject matter experts
4. **Validation Integrity**: No cases proceed without complete decision coverage
5. **Parallel Processing**: Maker/checker and expert reviews happen simultaneously
6. **Supervisor Oversight**: Proper escalation paths for complex cases