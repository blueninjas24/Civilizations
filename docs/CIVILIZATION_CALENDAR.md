# Saved civilization calendar

Schema 10 adds an explicitly initialized real-time anchor and queued demographic
target. It preserves existing settlements, people, events and clocks. Migration
does not activate simulation on its own.

`CivilizationTime` is pure Java. Callers provide current epoch milliseconds,
positive real milliseconds per civilization month, and an explicit elapsed-time
cap. Fractional months carry forward. Backward computer-clock changes freeze
accrual until the previous timestamp is reached, avoiding duplicate aging.
Overflow fails explicitly. Calendar presentation starts at year 0, month 1;
12 demographic steps per year comes from the editable population policy.

`CivilizationClockRepository.initialize` requires an existing demographic clock,
adopts its completed-month count, and rejects duplicate initialization. Elapsed
time, fractional remainder and queued target commit atomically before population
catch-up. The caller passes the saved target to `DemographicRepository.advance`
with a bounded step count. If demographic saving fails or the process stops,
the queued target survives; already completed months are never repeated.

Both repositories must use their connection on one owning thread. Existing active
transactions are rejected. Changed real-time cadence requires an explicit
transition; it cannot reinterpret a saved fractional month silently. Demographic
progress beyond the saved target fails visibly instead of discarding queued work.

The elapsed cap is a caller input, separate from per-batch step budgets. Any
discarded milliseconds are returned for diagnostics. No production offline cap
or runtime cadence is locked by this implementation. Runtime integration will
choose documented editable prototype settings under the user's authorization.
The plugin now creates/schedules the calendar through PopulationSession and exposes
read-only status commands. Automatic loaded-world observation/import is also wired;
clock advancement alone does not validate the complete visible population experience.

Tests use temporary SQLite files: partial months/restart, bounded catch-up,
duplicate requests, snapshot restore, cap/backward-clock persistence, queued work
after demographic failure, accrual failure/overflow rollback, ownership/settings
protection, and schema-9 upgrade failure/retry. Pure tests cover elapsed conversion,
year/month display, boundaries and invalid input. Tests are replay/accounting
checks, not a Paper performance measurement or population-balance guarantee.
