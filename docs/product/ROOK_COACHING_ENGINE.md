# Rook Coaching Engine v1

Rook is DeAddict's optional local coaching character. Rook challenges excuses and patterns without attacking a person's worth.

## Product behavior

- The account has a default tone: Direct, Quiet, or Brutal Banter.
- A Recovery Track can override the account default locally.
- Rook coaching appears on Today, Tracks, Rescue, and Insights.
- Today coaching uses only the selected Recovery Track and its saved daily outcome.
- Rescue coaching uses only the Recovery Track that owns the active Rescue session and its recorded urge values.
- Insights coaching uses only aggregate goal progress, trend, and run length for the selected Recovery Track.
- Private notes are never passed to the coaching engine.
- Coaching generation is deterministic and runs entirely on the device.

## Tone boundaries

### Direct

Clear, concise, action-oriented language. It names the outcome and points to the next controllable action.

### Quiet

Low-pressure, compassionate language. It supports observation, pacing, and small next steps.

### Brutal Banter

Blunt, playful challenge aimed at the habit, excuse, or autopilot pattern. It must never use humiliation, threats, discriminatory language, body shaming, worth-based insults, coercion, or abuse.

## Safety overrides

- Medically high-risk programs always use Direct safety language when risk guidance is relevant.
- Brutal Banter is downgraded to Direct for clinically sensitive programs.
- DeAddict never provides detox, taper, diagnosis, treatment, or emergency instructions.
- Severe withdrawal, overdose risk, or immediate danger directs the user toward qualified medical or emergency support.
- Safety wording always wins over tone preference or per-track override.

## Privacy and deletion

Rook preferences are stored in local DataStore. Per-track overrides contain only a Recovery Track UUID and tone enum. They are not synced. Local-data deletion and account deletion clear Rook preferences from the device.

## Future commercial phases

- Native-reviewed Hindi and Telugu dialogue libraries.
- A reviewed message-authoring pipeline and prohibited-phrase tests.
- User-controlled banter intensity within the safe Brutal Banter boundary.
- More contextual moments after check-in save, goal changes, replacement-action trends, and long-term maintenance.
- Optional message-history rotation without uploading recovery data.
