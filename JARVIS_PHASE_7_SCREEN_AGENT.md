# JARVIS Phase 7: Screen Agent (Vision & Context-Aware Control)

This document outlines the architecture and components for Phase 7 of **JARVIS AI**, adding screen perception, vision analysis, planning engines, and interactive command execution.

---

## Screen Agent Architecture Flow

The Screen Agent operates in an autonomous, closed-loop planning and acting cycle:

```
      USER COMMAND ("screen dekho aur... ")
                      │
                      ▼
    CAPTURE SCREEN (SamScreenContextService)
                      │
                      ▼
  ANALYZE & REFLECT (JarvisScreenCommandParser)
                      │
                      ▼
       PLAN STEP (JarvisAgentEngine)
                      │
                      ▼
     EXECUTE ACTION (JarvisScreenAgent)
                      │
                      ▼
      VERIFY STATE (ActionResultVerifier)
                      │
             ┌────────┴────────┐
             ▼                 ▼
         SUCCESS?             NO (Max 3 retries) ──► REFLECT & RE-PLAN
             │
             ▼
         COMPLETED
```

---

## Core Components Created

### 1. JarvisScreenAgent (`com/example/agent/JarvisScreenAgent.kt`)
The primary execution engine that performs visual actions on the screen (clicks, text input, scrolls) utilizing accessibility node trees and coordinates derived from the Screen Vision parser.

### 2. JarvisAgentEngine (`com/example/agent/JarvisAgentEngine.kt`)
Manages the cognitive agent execution loop: captures the screen, parses commands, executes single actions, verifies the results, and loops until the user's goal is accomplished or aborted.

### 3. JarvisScreenCommandParser (`com/example/agent/JarvisScreenCommandParser.kt`)
Translates raw user natural-language commands and the screen layout tree or captured image into structured, executable screen actions.

### 4. NavigationManager (`com/example/system/NavigationManager.kt`)
Coordinates system-level and in-app navigation transitions (back, home, open app drawer, launch settings) dynamically.

### 5. ActionResultVerifier (`com/example/system/ActionResultVerifier.kt`)
Inspects the newly refreshed screen state, layout nodes, and window bounds to verify if the previous action succeeded.

### 6. ActionRegistry (`com/example/device/command/ActionRegistry.kt`)
Central repository for registering, describing, and querying available screen-aware tool behaviors.

---

*Phase 7 Screen Agent integrated, fully tested, and compiled successfully.*
