# DeAddict Safety Boundaries

Last updated: 2026-07-29

## Product boundary

DeAddict is a private self-management recovery companion. It is not medical treatment, emergency care, diagnosis, detox management, or a replacement for qualified professional support.

## Safety tiers

```text
GENERAL_SELF_MANAGEMENT
CLINICALLY_SENSITIVE
MEDICALLY_HIGH_RISK
IMMEDIATE_DANGER
```

### General self-management

The app may provide tracking, goal setting, urge-management tools, environmental changes, replacement actions, and progress summaries.

### Clinically sensitive

The app must use careful language, avoid diagnosis, avoid shame, and recommend professional support when appropriate.

### Medically high-risk

The app must:

- Warn that suddenly changing use may carry medical risk for some substances.
- Avoid detox, taper, dosage, and medication instructions.
- Recommend qualified professional support.
- Disable Rook sarcasm and banter.
- Provide calm safety-focused guidance.

### Immediate danger

Immediate-danger handling overrides normal coaching, achievements, commercial prompts, and subscription prompts.

## Rook boundary

Rook may challenge:

- Excuses
- Avoidance
- Negotiation
- Repeated decisions
- Dishonest logging

Rook may never attack:

- Human worth
- Intelligence
- Appearance
- Family
- Trauma
- Religion
- Gender
- Sexuality
- Disability
- Mental illness
- Social background

Core rule:

> Attack the excuse. Challenge the decision. Confront the pattern. Protect the person.

## Rook safety requirements

- Direct is the production default.
- Brutal Banter requires explicit preview and opt-in.
- Quiet produces no unsolicited coaching.
- High-risk and severe-distress contexts force calm Direct behavior.
- Safety-critical responses must use reviewed deterministic content.
- Users must be able to report Helpful, Too Far, Repetitive, or Not Relevant.
- A Too Far response must reduce future intensity and block the offending template locally.

## Rescue boundary

Rescue must work offline and may provide:

- Pause and grounding
- Urge reassessment
- Trigger identification
- Environment change
- Replacement actions
- Motivation reminder
- Trusted-support shortcut
- Verified safety resources

Rescue must not provide:

- Medical detox plans
- Medication advice
- Guaranteed prevention claims
- Autonomous emergency dispatch
- Unrestricted generated crisis counseling

## Regional resources

Production release requires a versioned, clinically reviewed resource dataset with a country-neutral fallback. Resource updates must be controlled and auditable.

## Content review

Safety-sensitive copy requires:

- Content version
- Reviewer identity or role
- Review date
- Applicable regions
- Applicable safety tier
- Change history

## Claims boundary

Avoid claims such as:

- Cure addiction
- Prevent relapse
- Clinically proven without evidence
- Diagnose a disorder
- Treat withdrawal
- AI therapist
- Guaranteed recovery

Prefer:

- Recovery companion
- Self-management support
- Pattern tracking
- Urge-management tools
- Habit-change support

## Release blockers

- No verified regional safety-resource fallback
- No professional review of high-risk guidance
- Rook banter active in high-risk contexts
- Detox or taper instructions present
- Immediate-danger flow unavailable offline
- Safety-sensitive user content exposed in logs or analytics
