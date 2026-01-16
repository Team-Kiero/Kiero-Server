INSERT INTO schedule (
    schedule_color,
    start_time,
    end_time,
    is_recurring,
    name,
    created_at,
    parent_id,
    child_id
)
VALUES
    ('SCHEDULE1', '09:00:00', '13:00:00', true, '학교', '2026-01-16 08:39:18.000000', :parentId, :childId),
    ('SCHEDULE2', '13:00:00', '15:00:00', true, '돌봄 교실', '2026-01-16 08:39:18.000000', :parentId, :childId),
    ('SCHEDULE3', '14:00:00', '16:00:00', true, '태권도', '2026-01-16 08:39:18.000000', :parentId, :childId),
    ('SCHEDULE3', '09:00:00', '12:00:00', true, '태권도', '2026-01-16 08:39:18.000000', :parentId, :childId),
    ('SCHEDULE4', '14:00:00', '16:00:00', true, '피아노', '2026-01-16 08:39:18.000000', :parentId, :childId),
    ('SCHEDULE4', '12:00:00', '14:00:00', false, '피아노', '2026-01-16 08:39:18.000000', :parentId, :childId),
    ('SCHEDULE5', '16:00:00', '17:00:00', true, '수영 교실', '2026-01-16 08:39:18.000000', :parentId, :childId),
    ('SCHEDULE2', '18:00:00', '19:00:00', true, '수학', '2026-01-16 08:39:18.000000', :parentId, :childId),
    ('SCHEDULE3', '19:00:00', '20:00:00', false, '영어', '2026-01-16 08:39:18.000000', :parentId, :childId);