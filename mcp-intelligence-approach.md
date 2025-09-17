# **CVE Agent with Intelligent Version Targeting via MCP**

## **The Smart Approach: Query-First, Patch-Second**

Instead of **guess → patch → fail → learn**, we do:
**query → understand → patch intelligently**

```ascii
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        CLAUDE CODE WITH MCP INTELLIGENCE                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│ 1. Clone Repository & Analyze Structure                                        │
│ 2. Generate Current Dependency Tree                                            │
│ 3. *** MCP VULNERABILITY INTELLIGENCE ***                                      │
│    ┌─────────────────────────────────────────────────────────────────────────┐ │
│    │                    MCP VULNERABILITY ANALYZER                           │ │
│    │                                                                         │ │
│    │ INPUT: Current dependency tree                                          │ │
│    │ ├── spring-boot-starter-web:3.15                                       │ │
│    │ ├── log4j-core:2.14.1                                                  │ │
│    │ └── jackson-core:2.12.3                                                │ │
│    │                                                                         │ │
│    │ MCP TOOL QUERIES:                                                       │ │
│    │ ├── CVE Database: Which versions are vulnerable?                       │ │
│    │ ├── Version Database: What are the safe target versions?               │ │
│    │ ├── Compatibility Matrix: What versions work together?                 │ │
│    │ └── Framework Intelligence: BOM management patterns?                   │ │
│    │                                                                         │ │
│    │ OUTPUT: Intelligent Upgrade Plan                                        │ │
│    │ ├── log4j-core:2.14.1 → CVE-2021-44228 (CRITICAL)                     │ │
│    │ ├── Safe Target: log4j-core:2.17.2+ (CVE fixed)                       │ │
│    │ ├── Framework Issue: Spring Boot 3.15 BOM overrides log4j             │ │
│    │ ├── Recommendation: Upgrade Spring Boot to 3.18                       │ │
│    │ └── Strategy: BOM upgrade, not direct dependency                       │ │
│    └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                                 │
│ 4. Apply MCP-Informed Intelligent Patches                                      │
│ 5. Generate Predicted After-Patch Dependency Tree                              │
│ 6. Validate Prediction Against MCP Intelligence                                │
│ 7. Create Branch + Submit PR with Intelligence Context                         │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## **MCP Tool Design: Vulnerability Intelligence Service**

```python
# MCP Tool: vulnerability_intelligence.py

class VulnerabilityIntelligenceServer:
    def __init__(self):
        self.tools = [
            analyze_dependencies,
            get_vulnerability_info,
            recommend_versions,
            check_compatibility,
            get_framework_patterns
        ]

async def analyze_dependencies(dependency_tree: str, ecosystem: str) -> dict:
    """
    Analyze entire dependency tree for vulnerabilities and recommendations
    
    Input:
    dependency_tree: "spring-boot-starter-web:3.15\n├── log4j-core:2.14.1"
    ecosystem: "maven" | "npm" | "python" | "gradle"
    
    Output:
    {
        "vulnerabilities": [
            {
                "component": "log4j-core",
                "current_version": "2.14.1", 
                "cves": ["CVE-2021-44228", "CVE-2021-45046"],
                "severity": "CRITICAL",
                "safe_versions": ["2.17.2", "2.18.0", "2.19.0"],
                "recommended_version": "2.17.2",
                "upgrade_complexity": "MEDIUM"
            }
        ],
        "framework_issues": [
            {
                "framework": "spring-boot",
                "current_version": "3.15",
                "issue": "BOM override prevents direct log4j upgrade",
                "solution": "upgrade_spring_boot",
                "target_version": "3.18",
                "reason": "includes log4j 2.17.2 in BOM"
            }
        ],
        "upgrade_plan": {
            "strategy": "framework_bom_upgrade",
            "primary_action": "upgrade spring-boot 3.15 -> 3.18",
            "secondary_actions": ["remove explicit log4j declaration"],
            "expected_result": "log4j 2.17.2 via Spring Boot BOM"
        }
    }
    """

async def get_vulnerability_info(component: str, version: str) -> dict:
    """
    Get detailed CVE information for specific component version
    
    APIs to integrate:
    - CVE Database (NIST NVD)
    - Nexus IQ API  
    - WIZ API
    - Checkmarx API
    - OSV (Open Source Vulnerabilities)
    """

async def recommend_versions(component: str, current_version: str, constraints: dict) -> dict:
    """
    Recommend safe target versions considering constraints
    
    Constraints:
    - Framework compatibility (Spring Boot BOM)
    - Other dependency requirements  
    - Java version compatibility
    - Stability preferences (LTS vs latest)
    """

async def check_compatibility(upgrades: list) -> dict:
    """
    Check if multiple upgrades are compatible together
    
    Example: Spring Boot 3.18 + Jackson 2.15 + Log4j 2.17.2
    """

async def get_framework_patterns(framework: str, version: str) -> dict:
    """
    Get framework-specific upgrade patterns
    
    Spring Boot: BOM management, starter dependencies
    Node.js: peer dependencies, engine compatibility
    Python: virtual environment, dependency resolution
    """
```

## **Example: Claude Code with MCP Intelligence**

```ascii
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     INTELLIGENT CVE FIXING WORKFLOW                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│ STEP 1: Dependency Analysis                                                     │
│ Claude Code: "I see log4j-core:2.14.1 in Spring Boot 3.15 project"            │
│                                                                                 │
│ STEP 2: MCP Vulnerability Query                                                │
│ MCP Call: analyze_dependencies(current_tree, "maven")                          │
│                                                                                 │
│ STEP 3: MCP Intelligence Response                                              │
│ {                                                                               │
│   "vulnerabilities": [{                                                         │
│     "component": "log4j-core:2.14.1",                                          │
│     "cves": ["CVE-2021-44228"],                                                │
│     "recommended_version": "2.17.2"                                            │
│   }],                                                                           │
│   "framework_issues": [{                                                        │
│     "framework": "spring-boot:3.15",                                           │
│     "issue": "BOM override prevents direct log4j upgrade",                     │
│     "solution": "upgrade spring-boot to 3.18"                                  │
│   }],                                                                           │
│   "upgrade_plan": {                                                             │
│     "strategy": "framework_bom_upgrade",                                        │
│     "action": "spring-boot 3.15 -> 3.18",                                      │
│     "expected_result": "log4j 2.17.2 via BOM"                                  │
│   }                                                                             │
│ }                                                                               │
│                                                                                 │
│ STEP 4: Intelligent Patch Application                                          │
│ Claude Code: "I understand! Need to upgrade Spring Boot, not just log4j"       │
│ ├── Update pom.xml: spring-boot-parent 3.15 -> 3.18                           │
│ ├── Remove explicit log4j declaration (will come from BOM)                     │
│ └── Add comment explaining BOM-driven approach                                 │
│                                                                                 │
│ STEP 5: Validation                                                             │
│ Claude Code generates predicted dependency tree:                               │
│ ├── spring-boot-starter-web:3.18                                               │
│ │   └── log4j-core:2.17.2 (from Spring Boot BOM)                             │
│                                                                                 │
│ MCP Validation: "Prediction matches recommended upgrade plan ✓"                │
│                                                                                 │
│ RESULT: First-try success instead of trial-and-error                           │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## **MCP Integration APIs**

### **Vulnerability Databases**
```python
# CVE Database Integration
NIST_NVD_API = "https://services.nvd.nist.gov/rest/json/cves/2.0"
OSV_API = "https://api.osv.dev/v1/query"

# Security Tool APIs  
NEXUS_IQ_API = "https://your-nexus-iq/api/v2"
WIZ_API = "https://api.wiz.io/graphql" 
CHECKMARX_API = "https://your-checkmarx/cxrestapi"

# Version/Compatibility APIs
MAVEN_CENTRAL_API = "https://search.maven.org/solrsearch/select"
NPM_REGISTRY_API = "https://registry.npmjs.org"
PYTHON_PYPI_API = "https://pypi.org/pypi"
```

### **Framework Intelligence**
```python
# Framework-specific knowledge
SPRING_BOOT_BOM_MAPPING = {
    "3.15": {"log4j": "2.15.0", "jackson": "2.13.3"},
    "3.18": {"log4j": "2.17.2", "jackson": "2.14.2"},
    "3.19": {"log4j": "2.18.0", "jackson": "2.15.0"}
}

COMPATIBILITY_MATRIX = {
    "java": {
        "11": ["spring-boot:2.x", "spring-boot:3.0-3.15"],
        "17": ["spring-boot:3.x"],
        "21": ["spring-boot:3.17+"]
    }
}
```

## **Benefits of MCP-Powered Intelligence**

### **1. Upfront Intelligence**
- **No Trial-and-Error**: Know the right fix before applying
- **Framework Awareness**: Understand BOM patterns, dependency management
- **Compatibility Validation**: Ensure upgrades work together

### **2. Real-Time Vulnerability Data**
- **Multiple Sources**: CVE databases + security tool APIs
- **Severity Assessment**: Prioritize critical vulnerabilities  
- **Version Recommendations**: Safe target versions with rationale

### **3. Learning Acceleration**
- **Immediate Application**: No waiting for pipeline failures
- **Pattern Recognition**: Framework + dependency patterns upfront
- **Cross-Repository**: Share intelligence across all repositories

### **4. Reduced Iterations**
- **First-Try Success**: Intelligent patches vs guessing
- **Faster Resolution**: No failure-retry loops
- **Lower Risk**: Validated approaches vs experimental patches

**Your instinct is spot-on**: Why wait for failures when we can be intelligent from the start? MCP gives Claude Code the vulnerability intelligence it needs to make smart decisions upfront rather than learning through expensive trial-and-error cycles.

**This is much better architecture** - query-first, patch-smart!
