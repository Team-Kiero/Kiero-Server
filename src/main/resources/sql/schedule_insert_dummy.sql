INSERT INTO schedule (
    id,
    schedule_color,
    created_at,
    start_Time,
    end_Time,
    is_recurring,
    name,
    parent_id,
    child_id
)
VALUES
    (1, 'SCHEDULE5', '2026-01-10 04:09:10.330633', '09:00:00', '10:00:00', 1, '피아노 연습', :parentId, :childId),
    (2, 'SCHEDULE3','2026-01-10 04:09:10.330633', '16:00:00', '17:30:00', 0, '학원 숙제',  :parentId, :childId);