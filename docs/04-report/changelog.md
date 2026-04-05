# PDCA Completion Changelog

## [2026-03-27] - train-arrival Feature Completed

### Added
- Real-time train arrival tracking for KTX/ITX/누리로
- 4 realtime fields: 남은시간 (remaining), 현재위치 (location), 도착예정 (ETA), 실제도착 (actual arrival)
- Cron endpoint: `/api/cron/train-arrival` with 1-minute interval polling
- Dynamic polling intervals: 1/5/10/30 minutes based on time to arrival
- GIS Korail API integration: `gis.korail.com/api/train` (no authentication)
- Admin-web UI integration: ScheduleTable now displays train + express bus realtime info
- CronCountdown component: Added "🛤️ 열차도착" status display

### Changed
- ScheduleTable.tsx: Extended transport_type filtering to include 'train' alongside 'express'
- CronCountdown.tsx: Parallel fetch for both bus-arrival and train-arrival on page load
- schedules/page.tsx: Passed lastTrainArrivalCheckAt prop to CronCountdown
- vercel.json: Added train-arrival cron job configuration (*/1 * * * *)

### Database
- schedule_version table: Added `last_train_arrival_check_at` column (timestamptz)

### Implementation Stats
- **Match Rate**: 100%
- **Iterations Required**: 0
- **Files Modified**: 8
- **Files Created**: 1 (route.ts)
- **Lines Added**: ~175 (fetchTrainArrivalInfo function)
- **Duration**: 5 days (2026-03-23 ~ 2026-03-27)

### Verification
- Gap analysis: 100% design-implementation match
- All 13 functional requirements met
- Pattern consistency with express bus logic: 100%
- Type safety: TypeScript strict mode compliant

### Related Documents
- Plan: `.claude/plans/radiant-hugging-popcorn.md`
- Analysis: `docs/03-analysis/train-arrival.analysis.md`
- Report: `docs/04-report/features/train-arrival.report.md`

---

## [2026-03-23] - admin-web Feature Completed

### Added
- Schedule server migration from TAGO API (driver-app) to admin-web (Next.js server)
- Passenger estimation feature structure (seat difference algorithm)
- Daily cron job for schedule synchronization
- 10-minute interval seat collection with dynamic scheduler
- Admin schedule management page with manual edit + refresh buttons

### Implementation Stats
- **Match Rate**: 100%
- **Iterations Required**: 0
- **Plan Items**: 33 completed + 3 bonus features
- **Duration**: 5 days (2026-03-19 ~ 2026-03-23)

### Related Documents
- Report: `docs/04-report/features/admin-web.report.md`
